import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

public class ConnectionController {

    private static final int PORT = 11111;

    private static HashMap<String, HashMap<String, String>> mixerNames = new HashMap<String, HashMap<String, String>>();
    private static HashMap<String, String> sourceNames = new HashMap<String, String>();

    private static String sourceIp = "SourceIPs";

    // number of terminals
    private static int num = 0;
    private static int mixernum = 0;
    private static int cpuPerf = 0;
    private static String form;
    private static String servIp = "172.16.126.95";
    private static int sourceTerminal = 0;

    private static int nwWidth = 1500;

    static String[][] terminals = new String[20][5];
    // [n][0]: a kind of terminal{source,mixer,loMixer}
    // [n][1]: terminal name
    // [n][2]: terminal ipaddr
    // [n][3]: usage CPU resource, MixerTerminal only

    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    public static void main(String[] args) throws Exception {
        System.out.println("Chat server booted. listening on :" + PORT);
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept() ).start();
            }
        } finally {
            listener.close();
        }
    }

    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private InetAddress opponentIp;


        public Handler(Socket socket) {
            this.socket = socket;
            this.opponentIp = socket.getInetAddress();
        }

        public void run() {

            System.out.println();
            try {
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // waiting for any message
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }

                    // split joining message
                    System.out.println(name);
                    terminals[num] = name.split(":",5);
                    System.out.println("term" + terminals[num][0]);
                    // add terminals
                    num++;
                    System.out.println(num);
                    // wait Msg input break
                    break;
                }

                out.println("NAMEACCEPTED");
                writers.add(out);

                // メッセージ送信
                while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }

                    for (PrintWriter writer : writers) {
                        // 所謂開始のきっかけ"REACH"
                        if(input.equals("REACH")){

                        makemsg();
                        System.out.println(sourceIp);

                            // start, form, mixerIP, sourceTerminals...[n]
                            String order = "START:" + form + ":" + terminals[mixernum][2] +":"+ sourceIp;
                            writer.println(order);
                            System.out.println("Order = " + order);
                            System.out.println(input);
                        }
                        else {
                            writer.println("MESSAGE " + name + ": " + input);
                            System.out.println(input);
                        }
                    }
                }


            } catch (IOException e) {
                System.out.println(e);
            } finally {
                if (name != null) {
                    if (sourceNames.containsKey(name))
                            sourceNames.remove(name);
                    else if(mixerNames.containsKey(name))
                        mixerNames.remove(name);
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void makemsg(){
        System.out.println("Starting CPU process");
        for(int i=0; i < num ; i++){
            System.out.println(terminals[i][0]);

            switch (terminals[i][0]) {
                case "Local":

                    mixernum = i;
                    form = "Local";
                    break;
                case "Server":
                    if (Integer.parseInt(terminals[i][3]) > cpuPerf){
                        cpuPerf = Integer.parseInt(terminals[i][3]);
                        mixernum = i;
                        form = "Server";
                    }
                    break;
                case "Remote":
                    System.out.println("Rem m join");
                    if (Integer.parseInt(terminals[i][3]) > cpuPerf) {
                        cpuPerf = Integer.parseInt(terminals[i][3]);
                        mixernum = i;
                        form = "Remote";
                    }
                    break;
                case "Source":
                    System.out.println("join");
                    //System.out.println("terminals:"+ terminals[i][2]);
                    sourceTerminal++;
                    sourceIp = sourceIp +  ":" + terminals[i][2].substring(1);

                    System.out.println("sourceips = "+sourceIp);
                    break;
            }
            // System.out.println("MixerNum = " + mixernum);
            // System.out.println("CPUperf = " + cpuPerf);
            // System.out.println("MixerInfo = " + terminals[mixernum][2]);
        }
       // return sourceIp;
    }
}