import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Enumeration;

public class MediaServer {

    BufferedReader in;
    PrintWriter out;
    ProcessBuilder pb;
    Process p;
    Catcher c;
    BufferedReader br;
    String[] termInfo;

    String controllerIp = "172.16.126.91";
    // termInfo[0] = msg prefix "START"
    // termInfo[1] = MixerTerminalIP
    // termInfo[2]-[n] = SourceTerminalIP

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
            } else if (line.startsWith("START")) {

                termInfo = line.split(":",20);
                System.out.println("MixingForm: "+termInfo[1]);
                // ミキシング箇所によってコマンド変更
                // termInfo[] = { prefix, form, mixerIp, source...
                switch(termInfo[1]){
                    // role : streamer
                    case "Local":
                        System.out.println("you'll be a STREAMER");
                        pb = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://"+termInfo[2]+ "/live/mixed",
                                "-f", "flv", "rtmp://localhost/live/watch").redirectErrorStream(true);
                        p = pb.start();
                        br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        c = new Catcher(br);
                        c.start();
                        p.waitFor();
                        p.destroy();
                        System.out.println(c.out.toString());
                        break;

                    // role: Mixer
                    case "Server":
                         System.out.println("you'll be a Mixer");
                         pb = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://"+myLocalIp+ "/live/1",
                                "-i", "rtmp://"+myLocalIp+"/live/2",
                                 "-threads","0",
                                 "-filter_complex", "hstack,scale=1920x1080",
                                 "-vcodec", "libx264", "-max_interleave_delta", "0",
                                 "-vsync","1", "-b:v", "2000k",
                                 "-f", "flv", "-vsync", "1", "rtmp://localhost/live/watch").redirectErrorStream(true);
                        p = pb.start();
                        br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        c = new Catcher(br);
                        c.start();
                        p.waitFor();
                        p.destroy();
                        System.out.println(c.out.toString());
                        break;

                    // role: relay
                    case "Remote":

                        pb = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://localhost/live/1",
                                "-i", "rtmp://localhost/live/2",
                                "-vcodec", "copy", "-acodec", "copy",
                                "-f", "flv", "rtmp://"+termInfo[2]+"/live/1",
                                "-f", "flv", "rtmp://"+termInfo[2]+"/live/2").redirectErrorStream(true);
                        p = pb.start();
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
