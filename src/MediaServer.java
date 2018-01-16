import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Enumeration;

public class MediaServer {

    boolean startedFlag = false;
    BufferedReader in;
    PrintWriter out;
    ProcessBuilder pb1, pb2;
    Process p1 , p2;
    Catcher c1 , c2;
    BufferedReader br1, br2;
    String[] termInfo;
    // termInfo[0] = msg prefix "START"
    // termInfo[1] = MixerTerminalIP
    // termInfo[2]-[n] = SourceTerminalIP
    String controllerIp = "172.16.126.91";

    private void run(String cpuPerf) throws IOException, InterruptedException {

        // Make connection and initialize streams
        String myrole = "StreamingServer";
        Socket socket = new Socket(controllerIp, 11111);
        String myLocalIp = getIpAddr().substring(1); //ipaddrのみ取得 0文字目： "/"
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine();
            System.out.println(line);
            if (line.startsWith("SUBMITNAME")) {
                out.println("Server:strserver:"+ myLocalIp + ":"+cpuPerf);
            } else if (line.startsWith("NAMEACCEPTED")) {
                System.out.println("Name Accepted.");
            } else if (line.startsWith("MESSAGE")) {
                System.out.println("Message:" + line);
            }else if(line.startsWith("RESTART")){
                startedFlag = false;
                out.println("Server:strserver:"+ myLocalIp + ":"+cpuPerf);
                System.out.println("waiting for start message...");
            } else if (line.startsWith("START")) {

                termInfo = line.split(":",20);
                System.out.println("MixingForm: "+termInfo[1]);
                if(startedFlag = true){
                    p1.destroy();
                    if(p2.isAlive())
                        p2.destroy();
                    System.out.println("flag true, destroyed Processes");
                }
                // ミキシング箇所によってコマンド変更
                // termInfo[] = { prefix, form, mixerIp, source...
                switch(termInfo[1]){
                    // role : streamer
                    case "Local":
                        System.out.println("you'll be a STREAMER");
                        pb1 = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://"+termInfo[2]+ "/live/mixed",
                                "-f", "flv", "rtmp://localhost/live/watch").redirectErrorStream(true);
                        p1 = pb1.start();
                        br1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
                        c1 = new Catcher(br1);
                        c1.start();
                        p1.waitFor();
                        p1.destroy();
                        System.out.println(c1.out.toString());
                        break;

                    // role: Mixer
                    case "Server":
                         System.out.println("you'll be a Mixer");
                         pb1 = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://"+myLocalIp+ "/live/1",
                                "-i", "rtmp://"+myLocalIp+"/live/2",
                                 "-threads","0",
                                 "-filter_complex", "hstack,scale=1920x1080",
                                 "-vcodec", "libx264", "-max_interleave_delta", "0",
                                 "-vsync","1", "-b:v", "2000k",
                                 "-f", "flv", "-vsync", "1", "rtmp://localhost/live/watch").redirectErrorStream(true);
                        p1 = pb1.start();
                        br1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
                        c1 = new Catcher(br1);
                        c1.start();
                        p1.waitFor();
                        p1.destroy();
                        System.out.println(c1.out.toString());
                        break;

                    // role: relay
                    case "Remote":

                        pb1 = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://localhost/live/1",
                                "-vcodec", "copy", "-acodec", "copy",
                                "-f", "flv", "rtmp://"+termInfo[2]+"/live/1").redirectErrorStream(true);

                        pb2 = new ProcessBuilder("ffmpeg",
                            "-i", "rtmp://localhost/live/2",
                            "-vcodec", "copy", "-acodec", "copy",
                            "-f", "flv", "rtmp://"+termInfo[2]+"/live/2").redirectErrorStream(true);
                        p1 = pb1.start();
                        p2 = pb2.start();
                        BufferedReader br1 = new BufferedReader(new InputStreamReader(p1.getInputStream()));
                        BufferedReader br2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
                        Catcher c1 = new Catcher(br1);
                        Catcher c2 = new Catcher(br2);
                        c1.start();
                        c2.start();
                        p1.waitFor();
                        p2.waitFor();
                        p1.destroy();
                        p2.destroy();
                        System.out.println(c1.out.toString());
                        System.out.println(c2.out.toString());
                        break;
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        String cpuPerf = "20";
        if(args.length == 1)
            cpuPerf =args[0];
        MediaServer ms = new MediaServer();
        ms.run(cpuPerf);
    }

    // IPv4アドレス取得
    public String getIpAddr() throws IOException{
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            int i =0;
            while(interfaces.hasMoreElements()){

                NetworkInterface network = interfaces.nextElement();
                Enumeration<InetAddress> addresses = network.getInetAddresses();
                i++;
                while(addresses.hasMoreElements()){
                    InetAddress ip = addresses.nextElement();

                    if(!ip.isLoopbackAddress() && ip instanceof Inet4Address){
                        return ip.toString();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "null";
    }
}
