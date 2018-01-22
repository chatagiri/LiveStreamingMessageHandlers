import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Enumeration;

public class MediaServer {

    boolean startedFlag = false;
    boolean relayFlag = false;
    BufferedReader in;
    PrintWriter out;
    ProcessBuilder pb1, pb2;

    // PrintWriter r1, r2;
    Process p1, p2;
    Catcher c1, c2;
    BufferedReader br1, br2;
    String[] termInfo;
    // termInfo[0] = msg prefix "START"
    // termInfo[1] = MixerTerminalIP
    // termInfo[2]-[n] = SourceTerminalIP
    String controllerIp = "172.16.126.91";

    private void run(String cpuPerf) throws IOException, InterruptedException {

        processThread pt1 = null;
        processThread pt2 = null;

        // Make connection and initialize streams
        String myrole = "StreamingServer";
        Socket socket = new Socket(controllerIp, 11111);
        String myLocalIp = IpAddrConf.getIpAddr(); //ipaddrのみ取得 0文字目： "/
        System.out.println(myLocalIp);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine();
            System.out.println(line);
            if (line.startsWith("SUBMITNAME")) {
                out.println("Server:strserver:" + myLocalIp + ":" + cpuPerf);
            } else if (line.startsWith("NAMEACCEPTED")) {
                System.out.println("Name Accepted.");
            } else if (line.startsWith("MESSAGE")) {
                System.out.println("Message:" + line);
            } else if (line.startsWith("RESTART")) {
                if (startedFlag == true) {
                    System.out.println("restart receive");
                    if (relayFlag == true) {
                       pt1.interrupt();
                       pt2.interrupt();
                       relayFlag = false;
                    } else if(relayFlag == false) {
                        pt1.interrupt();
                        System.out.println("interrupt OK");
                    }
                    startedFlag = false;
                    out.println("Server:strserver:" + myLocalIp + ":" + cpuPerf);
                    System.out.println("waiting for start message...");
                }

            } else if (line.startsWith("START")) {
                startedFlag = true;

                termInfo = line.split(":", 20);
                System.out.println("MixingForm: " + termInfo[1]);
                // ミキシング箇所によってコマンド変更
                // termInfo[] = { prefix, form, mixerIp, source...
                switch (termInfo[1]) {
                    // role : streamer
                    case "Local":
                        System.out.println("you'll be a STREAMER");
                        pb1 = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://" + termInfo[2] + "/live/mixed",
                                " -vcodec", "copy", "-acodec", "copy",
                                "-f", "flv", "rtmp://localhost/live/watch").redirectErrorStream(true);
                        pt1 = new processThread(pb1);
                        pt1.run();
                        break;

                    // role: Mixer
                    case "Server":
                        System.out.println("you'll be a Mixer");
                        pb1 = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://" + myLocalIp + "/live/1",
                                "-i", "rtmp://" + myLocalIp + "/live/2",
                                "-threads", "0",
                                "-filter_complex", "hstack,scale=1280x720",
                                "-vcodec", "libx264", "-max_interleave_delta", "0",
                                "-vsync", "1", "-b:v", "800k",
                                "-f", "flv", "-vsync", "1", "rtmp://localhost/live/watch").redirectErrorStream(true);
                        pt1 = new processThread(pb1);
                        pt1.run();
                        break;

                    // role: relay
                    case "Remote":

                        pb1 = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://"+myLocalIp+"/live/1",
                                "-vcodec", "copy", "-acodec", "copy",
                                "-f", "flv", "rtmp://" + termInfo[2] + "/live/1").redirectErrorStream(true);

                        pb2 = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://"+myLocalIp+"/live/2",
                                "-vcodec", "copy", "-acodec", "copy",
                                "-f", "flv", "rtmp://" + termInfo[2] + "/live/2").redirectErrorStream(true);

                        relayFlag = true;
                        pt1 = new processThread(pb1);
                        pt2 = new processThread(pb2);
                        pt1.run();
                        pt2.run();


                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String cpuPerf = "20";
        if (args.length == 1)
            cpuPerf = args[0];
        MediaServer ms = new MediaServer();
        ms.run(cpuPerf);
    }

    class processThread extends Thread {
        private PrintWriter r1;
        private ProcessBuilder pb;
        private volatile boolean done = false;
        private Catcher c = null;
        private Process p = null;

        public processThread(ProcessBuilder pb) throws InterruptedException {
            this.pb = pb;
        }

        public void run() {
            BufferedReader br;
            try {
                try {
                    p = pb.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startedFlag = true;
                br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                c = new Catcher(br);
                System.out.println("catcher") ;
                c.start();
                r1 = new PrintWriter(p.getOutputStream(),true);
                Thread.sleep(6000);
                System.out.println("sleep done");
                //p.waitFor();
                //p.destroy();
                System.out.println(c.out.toString());
            } catch (InterruptedException e) {
                System.out.println("interrupt");
                System.out.println(c.out.toString());
                p.destroyForcibly();
            }
        }
    }
}
