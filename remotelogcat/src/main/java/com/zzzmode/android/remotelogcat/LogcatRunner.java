
package com.zzzmode.android.remotelogcat;

import android.util.Log;

import com.zzzmode.android.server.WebSocket;
import com.zzzmode.android.server.WebSocketServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class LogcatRunner {
    private static final String TAG = "LogcatRunner";

    private static final int VERSION=1;

    private static LogcatRunner sLogcatRunner;

    private ShellProcessThread mLogcatThread;

    private ShellProcessThread.ProcessOutputCallback mProcessOutputCallback;

    private WebSocketServer mWebSocketServer;

    private WebSocket mCurrWebSocket;

    private int port=11229;

    private static final String cmd="logcat -v time";

    public static volatile long byteCount=0;


    public static LogcatRunner getInstance(){
        if(sLogcatRunner == null){
            synchronized (LogcatRunner.class){
                if(sLogcatRunner == null){
                    sLogcatRunner =new LogcatRunner();
                }
            }
        }
        return sLogcatRunner;
    }

    private LogcatRunner(){

    }

    private void init() throws IOException {
        mWebSocketServer=new WebSocketServer(port,"/logcat");

        mWebSocketServer.setWebSocketServerCallback(new WebSocketServer.WebSocketServerCallback() {
            @Override
            public void onConnected(WebSocket webSocket) {
                try {
                    if(mCurrWebSocket != null){
                        mCurrWebSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mCurrWebSocket=webSocket;

                if(mCurrWebSocket != null){

                    mCurrWebSocket.setWebSocketCallback(new WebSocket.WebSocketCallback() {
                        @Override
                        public void onReceivedFrame(byte[] bytes) {
                            Log.e(TAG, "onReceivedFrame --> "+new String(bytes));
                        }

                        @Override
                        public void onClosed() {
                            Log.e(TAG, "onClosed --> "+mCurrWebSocket);
                            mCurrWebSocket=null;
                        }
                    });
                }
            }

            @Override
            public void onClosed() {

            }
        });

        mProcessOutputCallback=new ShellProcessThread.ProcessOutputCallback() {

            @Override
            public void onReaderLine(String line) {
                if(mCurrWebSocket != null && line != null){
                    try {
                        mCurrWebSocket.send(line);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }


    public int getPort(){
        return port;
    }

    public void start() throws IOException {
        init();
        mWebSocketServer.start();
        startLogThread();
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

            if(mWebSocketServer != null){
                mWebSocketServer.stop();
            }

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
            BufferedReader reader=null;
            try{

                exec= Runtime.getRuntime().exec(cmd);
                inputStream = exec.getInputStream();

                reader=new BufferedReader(new InputStreamReader(inputStream));
                while (readerLogging){
                    String line=reader.readLine();
                    if(mOutputCallback != null){
                        mOutputCallback.onReaderLine(line);
                    }

                }

            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try {
                    if(reader != null){
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

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
            void  onReaderLine(String line);
        }
    }

}
