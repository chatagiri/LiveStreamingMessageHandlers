import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LocalMixerTerminalClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    Runtime runtime = Runtime.getRuntime();

    String strServerIp = "172.16.126.95";
    String order = "";

    String[] termInfo;
    // termInfo[0] = msg prefix "START"
    // termInfo[1] = MixerTerminalIP
    // termInfo[2]-[n] = SourceTerminalIP

    public LocalMixerTerminalClient() {

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
        String myLocalIp = socket.getLocalAddress().toString().substring(1);
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

                    // role : Mixer
                    case "Local":
                        order = "ffmpeg" +
                                " -i rtmp://" + termInfo[4] +
                                " -i rtmp://" + termInfo[5] +
                                " -vcodec copy -acodec copy" +
                                " -f flv rtmp://" + strServerIp +"live/1" +
                                " -f flv rtmp://" + strServerIp +"live/2";
                        System.out.println("noworder" + order);
                        runtime.exec(order);
                        break;

                    // role: none
                    case "Server":
                        System.out.println("U have no role!");
                        break;

                    // role: none
                    case "Remote":
                        System.out.println("U have no role!");
                        break;
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        LocalMixerTerminalClient client = new LocalMixerTerminalClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}
