package basicSocket;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TCPClient {

    private static Long startTime;

    private static Long finishTime;

    public static void main(String[] args) {

        String fileName = "test.wav";
        String serverName = "localhost";
        int port = Integer.parseInt("8000");

        try {
            System.out.println("Connecting to host:" + serverName + " , port:" + port);
            Socket client = new Socket(serverName, port);
            System.out.println("Remote Host address:" + client.getRemoteSocketAddress());
            // 向socket写入字节流数据以传给服务器
            BufferedInputStream bfis = new BufferedInputStream(new FileInputStream(fileName));
            OutputStream outToServer = client.getOutputStream();
            DataOutputStream out = new DataOutputStream(outToServer);
            int by;
            byte[] ts = new byte[1024];
            System.out.println("writing...");
            startTime = fromDateStringToLong(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS").format(new Date()));
            while((by = bfis.read(ts)) != -1) {
                out.write(ts, 0, by);
            }
            System.out.println("transmisson completed");
            bfis.close();
            // 关键！
            client.shutdownOutput();
            // 读取服务器传来的数据
            InputStream inFromServer = client.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);
            String response = in.readUTF();
            finishTime = fromDateStringToLong(response);
            System.out.println("TCP传输该文件用了" + (finishTime-startTime) + "ms");
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
