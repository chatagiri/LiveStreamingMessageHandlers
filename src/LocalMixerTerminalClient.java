import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ProxySelector;
import java.net.Socket;

public class LocalMixerTerminalClient {

    String controllerIp =  "172.16.126.91";

    BufferedReader in;
    PrintWriter out;
    String[] termInfo;
    boolean startedFlag  = false;
    Catcher c; // handling standard output from FFmpeg
    Process p; // exec FFmpeg from this impl

    // termInfo[0] = msg prefix:"START"
    // termInfo[1] = MixerTerminalIP
    // termInfo[2]-[n] = SourceTerminalIP, here we covering only {2}terminals.

    private void run(String cpuPerf) throws IOException, InterruptedException {

        // Make connection and initialize streams

        Socket socket = new Socket(controllerIp, 11111);
        String myLocalIp = socket.getLocalAddress().toString().substring(1);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        processThread pt = null;

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine();
            System.out.println(line);
            if (line.startsWith("SUBMITNAME")) {
                out.println("Local:localMixer:"+ myLocalIp + ":"+cpuPerf);
            } else if (line.startsWith("NAMEACCEPTED")) {
                System.out.println("Name Accepted");
            } else if (line.startsWith("MESSAGE")) {
                System.out.println("MESSAGE:"+line);
            } else if (line.startsWith("START")) {

                // 通信開始
                System.out.println("Starting Connection");
                termInfo = line.split(":",20);
                // 送信されてくるミキサー箇所名によってコマンド変更
                // termInfo[] = { prefix, form, mixerIp, source...
                System.out.println("MixingForm: " +termInfo);

                switch(termInfo[1]){
                    // role : Mixer
                    case "Local":
                        System.out.println("you'll be a Mixer");

                        ProcessBuilder pb = new ProcessBuilder("ffmpeg",
                                "-i", "rtmp://localhost/live/1",
                                "-i", "rtmp://localhost/live/2",
                                "-threads","0",
                                "-filter_complex", "hstack,scale=1280x720",
                                "-vcodec", "libx264", "-max_interleave_delta", "0",
                                "-vsync","1", "-b:v","800k",
                                "-f", "flv", "-vsync", "1","rtmp://localhost/live/mixed").redirectErrorStream(true);
                        pt = new processThread(pb);
                        pt.run();
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
            }else if(line.startsWith("RESTART")){
                // 再接続処理

                out.println("Local:localMixer:"+ myLocalIp + ":"+cpuPerf);
                System.out.println("Reconnected to controller.");
                startedFlag = false;

            }
        }
    }

    /* @args cpuperf*/
    public static void main(String[] args) throws Exception {
        String cpuPerf = "30";
        if(args.length == 1)
            cpuPerf = args[0];

        LocalMixerTerminalClient client = new LocalMixerTerminalClient();
        client.run(cpuPerf);
    }

    class processThread extends Thread{
        private ProcessBuilder pb;
        private volatile boolean done = false;
        public processThread(ProcessBuilder pb){
            this.pb = pb;
        }

        public void run(){
            BufferedReader br;
           // Catcher c = null;
            try {
                Process p = pb.start();
                startedFlag = true;
                br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                Catcher c = new Catcher(br);
                c.start();
                p.waitFor();
                p.destroy();
                System.out.println(c.out.toString());
            } catch (Exception e) {
                System.out.println("interrupt");
            }
        }
    }
}
