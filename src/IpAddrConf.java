import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by chata on 2018/01/18.
 */
// IPv4アドレス取得
 class IpAddrConf{

    static String getIpAddr() throws IOException {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {

                NetworkInterface network = interfaces.nextElement();
                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();

                    if (!ip.isLoopbackAddress() && ip instanceof Inet4Address) {
                        return ip.toString();
                    }
                }
            }
        return "null";
    }
}

