
package com.zzzmode.android.remotelogcat;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;


public class LogcatRunner2 {
    private static final String TAG = "LogcatRunner";

    private static final int VERSION=1;

    private static LogcatRunner2 mLogcat;

    private ShellProcessThread mLogcatThread;

    private ShellProcessThread.ProcessOutputCallback mProcessOutputCallback;

    private OutputServer mOutputServer;

    private int port=11229;

    private boolean isBind=false;

    private static final String cmd="logcat -v time";

    public static volatile long byteCount=0;


    public static LogcatRunner2 getInstance(){
        if(mLogcat == null){
            mLogcat=new LogcatRunner2();
        }
        return mLogcat;
    }

    private LogcatRunner2(){

        mOutputServer=new OutputServer(port);

        mProcessOutputCallback=new ShellProcessThread.ProcessOutputCallback() {

            @Override
            public void onReaderLine(byte[] output,int start,int byteCount) {
                if(mOutputServer != null){
                    mOutputServer.send(output,start,byteCount);
                }
            }
        };

    }

    public int getPort(){
        return port;
    }

    public void start(){
        try {
            if(!isBind){
                mOutputServer.start();
            }

            startLogThread();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ServerSocketChannel socketChannel=ServerSocketChannel.open();
            socketChannel.socket().bind(new InetSocketAddress(44560));
            socketChannel.configureBlocking(false);
            final Selector selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private void startLogThread(){
        try {
            if(mLogcatThread !=null && mLogcatThread.isAlive()){
                mLogcatThread.stopReader();
                mLogcatThread.setOutputCallback(null);
                mLogcatThread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mLogcatThread=new ShellProcessThread(cmd);
        mLogcatThread.setOutputCallback(mProcessOutputCallback);
        mLogcatThread.start();

    }

    public void stop(){
        try {

            if(mOutputServer != null){
                mOutputServer.stop();
            }

            isBind=false;

            if (mLogcatThread != null && mLogcatThread.isAlive()) {
                mLogcatThread.stopReader();
                mLogcatThread.setOutputCallback(null);
                mLogcatThread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static class ShellProcessThread extends Thread {

        private String cmd;
        private volatile boolean readerLogging =true;
        private ProcessOutputCallback mOutputCallback;


        public ShellProcessThread(String cmd){
            this.cmd=cmd;
        }

        public void setOutputCallback(ProcessOutputCallback outputCallback) {
            mOutputCallback = outputCallback;
        }

        @Override
        public void run() {
            Process exec=null;
            InputStream inputStream=null;

            try{

                exec= Runtime.getRuntime().exec(cmd);
                inputStream = exec.getInputStream();

                byte[] buff=new byte[4096];

                while (readerLogging){

                    int len = inputStream.read(buff);
                    byteCount+=len;

                    if(mOutputCallback != null && len != -1){
                        mOutputCallback.onReaderLine(buff,0,len);
                    }
                }

                if(readerLogging){
                    exec.waitFor();
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {


                try {
                    if(inputStream != null){
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(exec != null){
                    exec.destroy();
                }
            }
        }

        public void stopReader(){
            readerLogging =false;
        }


        interface ProcessOutputCallback{
            void  onReaderLine(byte[] output,int start,int byteCount);
        }
    }



    private static class OutputServer{

        private int port;
        private ServerSocket mServerSocket;
        private Socket mSocket;
        private Thread mThread;
        private boolean isActive;

        OutputServer(int port){
            this.port=port;

        }

        public void start() throws IOException {

            isActive=true;
            try {
                if(mThread != null && mThread.isAlive()){
                    mThread.interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            mThread=new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mServerSocket=new ServerSocket(port);
                        while (isActive) {
                            Socket socket = mServerSocket.accept();
                            finshSocket(mSocket);
                            mSocket=socket;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            mThread.start();
        }

        private void finshSocket(Socket socket){
            try {
                if(socket != null){
                    socket.shutdownOutput();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if(socket != null){
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void finshServer(){
            try {
                if(mServerSocket != null){
                    mServerSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void send(byte[] bytes){
            send(bytes,0,bytes.length);
        }

        public void send(byte[] bytes,int start, int byteCount){
            try {
                if(mSocket != null && mSocket.isConnected() && !mSocket.isOutputShutdown()){
                    mSocket.getOutputStream().write(bytes,start,byteCount);
                }else {
                    mSocket=null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                if(e instanceof SocketException || e instanceof IOException){
                    finshSocket(mSocket);
                    mSocket=null;
                }
            }
        }

        public void stop(){

            try {
                isActive=false;

                finshSocket(mSocket);
                finshServer();

                if(mThread != null && mThread.isAlive()){
                    mThread.interrupt();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
