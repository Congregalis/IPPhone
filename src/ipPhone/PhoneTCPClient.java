package ipPhone;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class PhoneTCPClient {

    private int controlFlag;

    private AudioFormat audioFormat;

    private SourceDataLine sourceDataLine;

    private TargetDataLine targetDataLine;

    private OutputStream outToServer;

    private DataOutputStream out;

    private DataOutputStream controlOut;

    private Socket client;

    private Socket control;

    private HangUpThread hangUpThread;

    public static void main(String[] args) { new PhoneTCPClient();}

    private PhoneTCPClient() {

        String serverName = "localhost";
        int port = Integer.parseInt("8000");
        int controlPort = Integer.parseInt("8001");
        controlFlag = 0;

        try {
            System.out.println("连接到:" + serverName + " , 端口号为:" + port);
            client = new Socket(serverName, port);
            System.out.println("远程host地址为:" + client.getRemoteSocketAddress());
            control = new Socket(serverName, controlPort);

            outToServer = client.getOutputStream();
            out = new DataOutputStream(outToServer);
            controlOut = new DataOutputStream(control.getOutputStream());

            hangUpThread = new HangUpThread();

            // 录制语音并发送到服务器
            captureAudio();

            // 接受服务器的语音
            receiveAudio();

            // 控制是否关闭通话
            controlConnection();

            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /*
        开启接听线程，在听线程里接受客户端传来的语音
     */
    private void receiveAudio() {
        try {
            audioFormat =
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,44100F, 16, 2, 4,
                            44100F, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
             new ReceiveThread().start();

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
                bfis = new BufferedInputStream(client.getInputStream());
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
            System.out.println("接听功能正常关闭");
        }
    }

    /*
        开启说话线程，在说线程里传语音给客户端
     */
    private void captureAudio() {
        try {
            audioFormat =
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,44100F, 16, 2, 4,
                            44100F, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            System.out.println("通话中...(输入n结束通话)");
            new CaptureThread().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                    nByte = targetDataLine.read(buffer, 0, bufSize);
                    out.write(buffer, 0, nByte);
                    if (controlFlag == 1)
                        break;
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
        new InputThread().start();

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
        通过服务端传来的信息挂断
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
