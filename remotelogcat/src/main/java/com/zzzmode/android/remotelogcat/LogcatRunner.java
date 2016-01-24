/*
 * Copyright (C) 2016 zlcn2200@yeah.net . All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zzzmode.android.remotelogcat;

import android.os.Build;
import android.util.Log;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class LogcatRunner {
    private static final String TAG = "LogcatRunner";

    private static final int VERSION=1;

    private static LogcatRunner mLogcat;

    private ShellProcessThread mLogcatThread;

    private ShellProcessThread.ProcessOutputCallback mProcessOutputCallback;

    private AsyncHttpServer server;

    private volatile WebSocket mCurrentWebSocket;  //we only handle last connect websocket

    private int port=11229;

    private boolean isBind=false;

    private String cmd="logcat -v time";

    public static LogcatRunner getInstance(){
        if(mLogcat == null){
            mLogcat=new LogcatRunner();
        }
        return mLogcat;
    }

    private LogcatRunner(){

        server=new AsyncHttpServer();

        server.websocket("/logcat", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                if(mCurrentWebSocket != null && mCurrentWebSocket != webSocket){
                    mCurrentWebSocket.end();
                }

                mCurrentWebSocket=webSocket;

                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            if (ex != null)
                                Log.e("WebSocket", "Error");
                        } finally {
                            if(webSocket == mCurrentWebSocket){
                                mCurrentWebSocket=null;
                            }
                        }
                    }
                });

                webSocket.setStringCallback(new WebSocket.StringCallback() {


                    @Override
                    public void onStringAvailable(String s) {
                        if ("Hello Server".equals(s)){
                            webSocket.send("welcome remote logcat!  server: "+ Build.MODEL+"   "+"    "+Build.VERSION.RELEASE);
                            webSocket.send("$"+VERSION);
                        }

                    }
                });
            }
        });


        mProcessOutputCallback=new ShellProcessThread.ProcessOutputCallback() {

            @Override
            public void onReaderLine(String output) {
                if(mCurrentWebSocket != null){
                    mCurrentWebSocket.send(output);
                }
            }
        };

    }


    public LogcatRunner bind(int port) {
        if (!isBind) {
            this.port=port;
            server.listen(port);
            isBind = true;
        }
        return mLogcat;
    }

    public int getPort(){
        return port;
    }

    public void start(){
        try {
            if(!isBind){
                bind(port);
            }

            startLogThread();
        } catch (Exception e) {
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
            if(mCurrentWebSocket != null){
                mCurrentWebSocket.end();
            }

            mCurrentWebSocket=null;
            if (server != null) {
                server.stop();
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
            BufferedReader reader=null;
            try{

                exec= Runtime.getRuntime().exec(cmd);

                inputStream = exec.getInputStream();

                reader=new BufferedReader(new InputStreamReader(inputStream));

                String line=null;
                
                do {
                    line=reader.readLine();
                    
                    if(mOutputCallback != null ){
                        mOutputCallback.onReaderLine(line);
                    }

                }while (readerLogging && line != null);

                if(readerLogging){
                    exec.waitFor();
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
            void  onReaderLine(String output);
        }
    }


}
