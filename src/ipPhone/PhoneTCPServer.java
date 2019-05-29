package ipPhone;

import javax.sound.sampled.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class PhoneTCPServer extends Thread{

    private ServerSocket serverSocket;

    private int controlFlag;

    private Socket server;

    private Socket control;

    private ServerSocket controlSocket;

    private DataOutputStream out;

    private DataOutputStream controlOut;

    private AudioFormat audioFormat;

    private TargetDataLine targetDataLine;

    private SourceDataLine sourceDataLine;

    private ReceiveThread receiveThread;

    private CaptureThread captureThread;

    private HangUpThread hangUpThread;

    private InputThread inputThread;

    public static void main(String[] args) {
        int port = Integer.parseInt("8000");
        int controlPort = Integer.parseInt("8001");
        try {
            Thread t = new PhoneTCPServer(port, controlPort);
            t.run();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private PhoneTCPServer(int port, int cotrolPort) throws IOException {
        serverSocket = new ServerSocket(port);
        controlSocket = new ServerSocket(cotrolPort);
        System.out.println("服务器已启动。");
        serverSocket.setSoTimeout(10000);
    }


    @Override
    public void run() {
        while (true) {
            controlFlag = 0;
            try {
                System.out.println("等待客户端发起通话请求，端口号为:" + serverSocket.getLocalPort() + "...");
                // 接受客户端socket
                server = serverSocket.accept();
                System.out.println("客户端地址为:" + server.getRemoteSocketAddress());
                //out = new DataOutputStream(server.getOutputStream());

                control = controlSocket.accept();
                controlOut = new DataOutputStream(control.getOutputStream());

                receiveThread = new ReceiveThread();
                captureThread = new CaptureThread();
                hangUpThread = new HangUpThread();
                inputThread = new InputThread();

                // 接受语音并播放
                receiveAudio();

                // 发送语音
                captureAudio();

                // 控制通话的结束
                controlConnection();


                //server.close();
            }catch (SocketTimeoutException e) {
                System.out.println("等待超时!");
                System.exit(0);
                break;
            }
            catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void receiveAudio() {
        // 接受语音并播放
        try {
            audioFormat =
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,44100F, 16, 2, 4,
                            44100F, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            receiveThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 听线程
    class ReceiveThread extends Thread {
        @Override
        public void run() {
            System.out.println("接听功能正常开启");
            try {
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();
                FloatControl fc=(FloatControl)sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
                double value=2;
                float dB = (float) (Math.log(value==0.0?0.0001:value)/Math.log(10.0)*20.0);
                fc.setValue(dB);
                int nByte = 0;
                final int bufSize=4100;
                byte[] buffer = new byte[bufSize];
                BufferedInputStream bfis = null;
                bfis = new BufferedInputStream(server.getInputStream());
                while (nByte != -1) {
                    nByte = bfis.read(buffer, 0, bufSize);
                    if (nByte != -1)
                        sourceDataLine.write(buffer, 0, nByte);
                    if (controlFlag == 1)
                        break;
                }
                sourceDataLine.stop();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
            System.out.println("说接听功能正常关闭");
        }
    }

    private void captureAudio() {
        try {
            audioFormat =
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,44100F, 16, 2, 4,
                            44100F, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            out = new DataOutputStream(server.getOutputStream());
            System.out.println("通话中...(输入n结束通话)");
            captureThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 说线程
    class CaptureThread extends Thread {
        @Override
        public void run() {
            System.out.println("说话功能正常开启");
            try {
                targetDataLine.open(audioFormat);
                targetDataLine.start();
                int nByte = 0;
                final int bufSize=4100;
                byte[] buffer = new byte[bufSize];
                //BufferedInputStream bfis = new BufferedInputStream(AudioSystem.getAudioInputStream(targetDataLine));
                while (nByte != -1) {
                    //System.in.read();
                    if (controlFlag == 1)
                        break;
                    nByte = targetDataLine.read(buffer, 0, bufSize);
                    out.write(buffer, 0, nByte);
                }
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("说话功能正常关闭");
        }
    }

    /*
        控制通话的结束
     */
    private void controlConnection() {

        // 客户端请求挂断电话
        hangUpThread.start();

        // 手动挂断电话
        inputThread.start();

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (controlFlag == 1) {
                break;
            }
        }
    }

    /*
        通过输入n来挂断电话
     */
    class InputThread extends Thread {
        @Override
        public void run() {
            while(true) {
                Scanner input = new Scanner(System.in);
                String sinput = input.next();
                if (sinput.equals("n")) {
                    hangUpThread.stop();
                    finish();
                    break;
                }
            }
        }
    }

    /*
        通过客户端传来的信息挂断
     */
    class HangUpThread extends Thread {
        @Override
        public void run() {
            try {
                InputStream controlFromClient = control.getInputStream();
                DataInputStream in = new DataInputStream(controlFromClient);
                String isFinished = in.readUTF();
                if (isFinished.equals("1")) {
                    //inputThread.interrupt();
                    finish();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /*
        结束通信
     */
    private void finish() {
        controlFlag = 1;
        try {
            controlOut.writeUTF("1");
            control.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("通话结束");
    }

}
