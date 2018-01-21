import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import com.sun.management.OperatingSystemMXBean;

import javax.management.MXBean;
import javax.swing.*;

public class ConnectionController {

    private static final int PORT = 11111;
     private static String sourceIp = "SourceIPs";
   // private static String sourceIp = "SourceIps:172.16.126.93:172.16.126.86";

    // number of terminals
    private static int num = 0;
    private static int mixernum = 0;
    private static int localcpuPerf = 0;
    private static int remotecpuPerf = 0;
    private static int servercpuPerf = 0;
    private static String form;
    private static int sourceTerminal = 0;
    private static ArrayList<String> formList = new ArrayList<String>();
    private static boolean startedFlag = false;
    private boolean lmFlag = false;
    static int nwWidth ;

    private JFrame frame = new JFrame("ConnectionController");
    private JButton button = new JButton("通信開始");
    private static JCheckBox lmBox = new JCheckBox("LocalMixerを使用する");
    private JLabel label = new JLabel("現地可用NW帯域:");
    private JLabel spacer = new JLabel("KB  ");
    private static JTextField nwField = new JTextField("1500");

    public ConnectionController() {
        JPanel p1 = new JPanel();
        p1.setLayout(null);
        JPanel p2 = new JPanel();
        p2.setLayout(new FlowLayout(FlowLayout.LEFT));

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setBounds(900, 200, 450, 300);
        frame.setAlwaysOnTop(true);
        button.setSize(450, 250);
        button.setBackground(Color.decode("#87CEFA"));
        button.setFont(new Font("メイリオ",Font.PLAIN,32));
        label.setSize(225,50);
        label.setFont(new Font("メイリオ",Font.PLAIN,12));
        spacer.setFont(new Font("メイリオ",Font.PLAIN,12));
        lmBox.setSize(225,50);
        lmBox.setFont(new Font("メイリオ",Font.PLAIN,12));
        nwField.setSize(225,50);

        // Layout GUI
        p1.setSize(450,250);
        p1.add(button,"Center");
        p2.setSize(450,50);
        p2.add(label);
        p2.add(nwField);
        p2.add(spacer);
        p2.add(lmBox);
        frame.getContentPane().add(p1);
        frame.getContentPane().add(p2,"South");

        // ボタン降下で接続開始
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectStart();
                if(startedFlag == true) {
                    button.setBackground(Color.decode("#FFC0CB"));
                    button.setText("通信中");
                }
                else{
                    button.setBackground(Color.decode("#87CEFA"));
                    button.setText("通信開始");
                }
            }
        });
    }

    static String[][] terminals = new String[20][5];
    // [n][0]: a kind of terminal{source,mixer,loMixer}
    // [n][1]: terminal name
    // [n][2]: terminal ipaddr
    // [n][3]: usage CPU resource, MixerTerminal only

    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();
    public static void main(String[] args) throws Exception {

        ConnectionController cc = new ConnectionController();
        cc.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cc.frame.setVisible(true);
        cc.run();
    }

    void run()throws Exception{
        System.out.println("Connection Controller booted. listening on :" + PORT);
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
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

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 端末接続待機
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    break;
                }
                out.println("NAMEACCEPTED");
                writers.add(out);

                // メッセージ受信
                while (true) {
                    /*** 端末リスト生成 ***/
                    // split joining message,
                    System.out.println("inputee"+name);
                    terminals[num] = name.split(":",5);
                    System.out.println("term" + terminals[num][0]);
                    // add terminals
                    num++;
                    System.out.println(num);
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                if (name != null) {
//                    if (terminals.containsKey(name))
//                            terminals.remove(name);
//                    else if(mixerNames.containsKey(name))
//                        mixerNames.remove(name);
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

    // 接続開始
    public void connectStart() {
        int i = 0;
        String order = "";
        nwWidth = Integer.parseInt(nwField.getText());
        makemsg();
        System.out.println(sourceIp);

        // start, form, mixerIP, sourceTerminals...[n]
        if(!startedFlag == true) {
            order = "START:" + form + ":" + terminals[mixernum][2] + ":" + sourceIp;
            startedFlag = true;
        }
        else{
            // 再構成
            i++;
            terminals = new String[20][5];
            num = 0;
            order = "RESTART";
            sourceIp ="SourceIPs";
            System.out.println("接続待機に移行します");
            startedFlag = false;
        }

        System.out.println("Order = " + order);
        for (PrintWriter writer : writers) {
            writer.println(order);
        }

        if(i>0)
            writers.clear();

    }

    // ミキシング箇所制定
    public static void makemsg(){
        boolean localflag = false;
        boolean serverflag = false;
        boolean remoteflag = false;

        int localNum = 0, serverNum = 0, remoteNum = 0 ;
        System.out.println("Starting CPU process");
        /*** 適合可能形態リスト生成処理 ***/
        for(int i=0; i < num ; i++){
            System.out.println(terminals[i][0]);

            switch (terminals[i][0]) {
                case "Local":
                    if(lmBox.isSelected()) {
                        localNum = i;
                        localflag = true;
                        localcpuPerf = Integer.parseInt(terminals[i][3]);
                    }
                    break;
                case "Server":
                    serverNum = i;
                    serverflag = true;
                    servercpuPerf = Integer.parseInt(terminals[i][3]);
                    break;
                case "Remote":
                    remoteNum = i;
                    remoteflag = true;
                    remotecpuPerf = Integer.parseInt(terminals[i][3]);
                    break;
                case "Source":
                    //System.out.println("terminals:"+ terminals[i][2]);
                    sourceTerminal++;
                    sourceIp = sourceIp +  ":" + terminals[i][2].substring(1);
                    break;
            }
        }
        /*** 適合形態選択処理 ***/
        if(sourceTerminal * 500 >= nwWidth) {
            if (localflag == true) {
                form = "Local";
                mixernum = localNum;
            }
            else if (serverflag == true) {
                form = "Server";
                mixernum = serverNum;
            }
            else if (remoteflag == true) {
                form = "Remote";
                mixernum = remoteNum;
            }
        }
        else if (sourceTerminal * 500 <= nwWidth) {
            if(remotecpuPerf >= servercpuPerf) {
                mixernum = serverNum;
                form = "Server";
            }
            else if (remotecpuPerf <= servercpuPerf) {
                mixernum = remoteNum;
                form = "Remote";
            }
        }

        System.out.println("MixingForm = " + form);
    }
}