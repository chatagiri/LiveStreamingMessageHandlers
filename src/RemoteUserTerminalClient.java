/* a program for Streaming Server
 maybe it works good with ur RTMP server.
 and plz type ur RTMP streaming key in build ffmpeg order.
 */

import jdk.internal.util.xml.impl.Input;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.ArrayList;

public class RemoteUserTerminalClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    Runtime runtime = Runtime.getRuntime();

    int termNum = 0;
    String order = "";
    String strServerIp = "172.16.126.95";

    String[] termInfo;
    // termInfo[0] = msg prefix "START"
    // termInfo[1] = MixerTerminalIP
    // termInfo[2]-[n] = SourceTerminalIP

    public RemoteUserTerminalClient() {

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();

        // Add Listeners
        textField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    private void run() throws IOException {

        // Make connection and initialize streams
        String myrole = "Server" ;
        String serverAddress = "localhost";
        Socket socket = new Socket(serverAddress, 11111);
        //String myLocalIp = socket.getLocalAddress().toString().substring(1);
        String myLocalIp = Inet4Address.getLocalHost().toString();
        myLocalIp = myLocalIp.substring(myLocalIp.length()-13);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine();
            System.out.println(line);
            if (line.startsWith("SUBMITNAME")) {
                out.println("Remote:remoteuser:"+ myLocalIp + ":"+50);
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("START")) {

                termInfo = line.split(":",20);
                // ミキシング箇所によってコマンド変更
                // termInfo[] = { prefix, form, mixerIp, source...
                switch(termInfo[1]){
                    // role: View only
                    case "Local":
                        System.out.println("形態:"+ termInfo[1]);
                        System.out.println("視聴用URL : rtmp://"+ strServerIp + "/live/watch" );
                        break;

                    // role: View only
                    case "Server":
                        System.out.println("形態:"+ termInfo[1]);
                        System.out.println("視聴用URL : rtmp://"+ strServerIp + "/live/watch" );
                        break;

                    // role:Mixer
                    case "Remote":
                        System.out.println("IM MIXer");
                        ProcessBuilder pb = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://" ,strServerIp, "/live/1",
                                "-i", "rtmp://" ,strServerIp, "/live/2",
                                "-filter_complex", "\"[0:v]pad=2*iw[a];", "[a][1:v]overlay=w\"",
                                "-vcodec", "libx264", "-f", "flv", "rtmp://localhost/live/watch");
                        Process p = pb.start();
                        InputStream is = p.getInputStream();
                        try {
                            while(is.read() >= 0); //標準出力だけ読み込めばよい
                        } finally {
                            is.close();
                        }
                        System.out.println("ffmpeg closed");
                        break;
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        RemoteUserTerminalClient client = new RemoteUserTerminalClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}