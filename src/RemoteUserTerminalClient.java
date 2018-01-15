/* a program for Streaming Server
 maybe it works good with ur RTMP server.
 and plz type ur RTMP streaming key in build ffmpeg order.
 */

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
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
    JFrame frame = new JFrame("ConnectionController");
    JButton button = new JButton("通信開始");

    String strServerIp = "172.16.126.95";

    String[] termInfo;
    // termInfo[0] = msg prefix "START"
    // termInfo[1] = MixerTerminalIP
    // termInfo[2]-[n] = SourceTerminalIP

    public RemoteUserTerminalClient() throws IOException {

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setBounds(300,200,450,300);
        button.setSize(450,300);


        // Layout GUI
        frame.getContentPane().add(button, "Center");

        String controllerIp = "localhost";
        Socket socket = new Socket(controllerIp, 11111);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                out.println("REACH");
            }
        });
    }

    private void run(String cpuPerf) throws IOException, InterruptedException{

        // get my own IPv4 address
        String myLocalIp = Inet4Address.getLocalHost().toString();
        myLocalIp = myLocalIp.substring(myLocalIp.length()-13);
        System.out.println(myLocalIp);

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine();
            System.out.println(line);
            if (line.startsWith("SUBMITNAME")) {
                out.println("Remote:remoteuser:"+ myLocalIp + ":"+cpuPerf);
            } else if (line.startsWith("NAMEACCEPTED")) {
                System.out.println("Name Accepted");
            } else if (line.startsWith("MESSAGE")) {
                System.out.println("Message:" + line);
            } else if (line.startsWith("START")) {

                System.out.println("Starting Connection");
                termInfo = line.split(":",20);
                // ミキシング箇所によってコマンド変更
                // termInfo[] = { prefix, form, mixerIp, source...
                System.out.println("MixingForm: "+termInfo[1]);
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
                        System.out.println("you'll be a Mixer");
                        System.out.println(strServerIp);
                        Thread.sleep(10000);
                        ProcessBuilder pb = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://"+strServerIp+"/live/1",
                                "-i", "rtmp://"+strServerIp+"/live/2",
                                "-threads","0",
                                "-filter_complex", "hstack,scale=1920x1080",
                                "-vcodec", "libx264", "-max_interleave_delta", "0",
                                "-vsync","1", "-b:v", "2000k",
                                "-f", "flv", "-vsync", "1", "rtmp://localhost/live/watch").redirectErrorStream(true);
                        System.out.println("視聴用URL : rtmp://"+ myLocalIp + "/live/watch" );

                        // ffmpeg実行
                        Process p = pb.start();
                        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        Catcher c = new Catcher(br);
                        c.start();
                        p.waitFor();
                        p.destroy();
                        System.out.println(c.out.toString());
                        break;
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        String cpuPerf = "70";
        if(args.length == 1)
            cpuPerf = args[0];
        RemoteUserTerminalClient client = new RemoteUserTerminalClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run(cpuPerf);
    }
}