package basicSocket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TCPServer extends Thread{

    private ServerSocket serverSocket;

    private String receivedFile = "D:\\test.wav";

    private TCPServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("server start.");
        serverSocket.setSoTimeout(10000);
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("Waiting for remote connection, port is:" + serverSocket.getLocalPort() + "...");
                // 接受客户端socket
                Socket server = serverSocket.accept();
                System.out.println("remote host address:" + server.getRemoteSocketAddress());
                // 读入客户端写在socket中的数据并写入到e盘某文件夹中，名字为copy.mp4
                BufferedInputStream bfis = new BufferedInputStream(server.getInputStream());
                BufferedOutputStream bfos = new BufferedOutputStream(new FileOutputStream(receivedFile));
                DataInputStream in = new DataInputStream(bfis);
                int by;
                byte[] ts = new byte[1024];
                while ((by = bfis.read(ts)) != -1) {
                    bfos.write(ts, 0, by);
                }
                //System.out.println(in.readUTF());
                // 关键！
                server.shutdownInput();
                // 写入数据传给客户端
                DataOutputStream out = new DataOutputStream(server.getOutputStream());
                out.writeUTF(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS").format(new Date()));
                bfis.close();
                bfos.close();
                server.close();
            }catch (SocketTimeoutException e) {
                System.out.println("Socket timed out!");
                break;
            }
            catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt("8000");
        try {
            Thread t = new TCPServer(port);
            t.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
