import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;

public class MediaServer {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    Runtime runtime = Runtime.getRuntime();

    int termNum = 0;
    String order = "";

    String[] termInfo;
    // termInfo[0] = msg prefix "START"
    // termInfo[1] = MixerTerminalIP
    // termInfo[2]-[n] = SourceTerminalIP

    public MediaServer() {

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
        // remote user terminal's ipaddr
        String controllerIp = "172.16.126.91";
        Socket socket = new Socket(controllerIp, 11111);
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
                out.println("Server:strserver:"+ myLocalIp + ":"+30);
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("START")) {

                termInfo = line.split(":",20);
                // ミキシング箇所によってコマンド変更
                // termInfo[] = { prefix, form, mixerIp, source...
                switch(termInfo[1]){

                    // role : streamer
                    case "Local":
                        order = "ffmpeg -i rtmp://" + termInfo[2] + "/live/mixed -f flv -r 30 rtmp://localhost/live/watch";
                        System.out.println("noworder" + order);
                        runtime.exec(order);
                        break;

                    // role: Mixer
                    case "Server":
                        System.out.println("IM MIXer");
                        // building ffmpeg order
                        order = " -i rtmp://" + myLocalIp + "/live/1" +
                                " -i rtmp://" + myLocalIp + "/live/2";
                        // Mixing Process
                        order.concat(" -filter_complex \" [0:v]pad=2*iw[a]; [a][1:v]overlay=w \" -vcodec libx264 -f flv -r 30  rtmp://localhost/live/watch" );
                        System.out.println("resultOrder:"+order);
                        runtime.exec(order);
                        break;

                    // role: relay
                    case "Remote":
                        order = "ffmpeg" +
                                " -i rtmp://" + termInfo[2] + "/live/1" +
                                " -i rtmp://" + termInfo[2] + "/live/2"+
                                " -vcodec copy -acodec copy" +
                                " -f flv rtmp://" + myLocalIp +"live/1" +
                                " -f flv rtmp://" + myLocalIp +"live/2";
                        System.out.println("resultOrder:"+order);
                        runtime.exec(order);
                        break;
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        MediaServer ms = new MediaServer();
        ms.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ms.frame.setVisible(true);
        ms.run();
    }
}
