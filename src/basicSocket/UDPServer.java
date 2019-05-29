package basicSocket;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UDPServer {

    private static String receivedFile = "E:\\迅雷下载\\copy.mkv";

    public static void main(String[] args) {
        try {
            DatagramSocket server = new DatagramSocket(5060);
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            RandomAccessFile accessFile = null;
            // 接受客户端发送的数据
            BufferedOutputStream bfos = new BufferedOutputStream(new FileOutputStream(receivedFile));
            System.out.println("Waiting for remote connection, port is:" + server.getLocalPort() + "...");
            int port;

            byte[] ts = new byte[1024];
            int len = 0;
            server.receive(packet);
            port = packet.getPort();
            bfos.write(ts, 0, len);
            bfos.flush();
            System.out.println("remote host port is " + port);
            System.out.println("receiving...");
            while (len == 0) {
                server.receive(packet);
                len = packet.getLength();
                if (len > 0) {
                    bfos.write(ts, 0, len);
                    bfos.flush();
                    if (len !=1024)
                        break;
                    len = 0;
                }
            }
            String finishtime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS").format(new Date());
            System.out.println("receive completely");

            // 向客户端发送结束时间
            byte[] sendData = finishtime.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getLocalHost(),
                    port);
            System.out.println("send completely,  " + new String(sendData));
            bfos.close();
            server.send(sendPacket);

            server.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
