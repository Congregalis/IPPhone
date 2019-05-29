package basicSocket;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UDPClient {

    private static Long startTime;

    private static Long finishTime;

    private static String fileName = "E:\\迅雷下载\\[阳光电影www.ygdy8.com].攻壳机动队.BD.720p.中英双字幕.mkv";

    public static void main(String[] args){
        try {
            DatagramSocket client = new DatagramSocket(5070);
            DatagramPacket packet = new DatagramPacket(new byte[1024],1024);
            // 发送数据
            packet.setPort(5060);
            packet.setAddress(InetAddress.getLocalHost());
            BufferedInputStream bfis = new BufferedInputStream(new FileInputStream(fileName));
            byte[] ts = new byte[1024];
            int by = -1;
            System.out.println("sending...");
            startTime = fromDateStringToLong(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS").format(new Date()));
            while ((by = bfis.read(ts)) != -1) {
                packet.setData(ts, 0, by);
                client.send(packet);
            }
            bfis.close();
            System.out.println("send completely");
//            packet.setData("Hello Server".getBytes());
//            client.send(packet);
            // 接受服务端传来的数据
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            client.receive(receivePacket);
            finishTime = fromDateStringToLong(new String(receivePacket.getData()));
            System.out.println("UDP传输该文件用了" + (finishTime-startTime) + "ms");
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long fromDateStringToLong(String inVal) {
        Date date = null;
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS");
        try {
            date = inputFormat.parse(inVal);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

}
