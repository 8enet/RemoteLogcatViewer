
package com.zzzmode.android.remotelogcat;

import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import com.zzzmode.android.server.WebSocket;
import com.zzzmode.android.server.WebSocketServer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class LogcatRunner {
    private static final String TAG = "LogcatRunner";

    private static final int VERSION = 1;

    private static LogcatRunner sLogcatRunner;

    private ShellProcessThread mLogcatThread;

    private ShellProcessThread.ProcessOutputCallback mProcessOutputCallback;

    private WebSocketServer mWebSocketServer;

    private WebSocket mCurrWebSocket;

    private LogConfig mLogConfig;


    public static LogcatRunner getInstance() {
        if (sLogcatRunner == null) {
            synchronized (LogcatRunner.class) {
                if (sLogcatRunner == null) {
                    sLogcatRunner = new LogcatRunner();
                }
            }
        }
        return sLogcatRunner;
    }


    private LogcatRunner() {

    }


    private void init() throws IOException {
        if (mLogConfig == null) {
            mLogConfig = LogConfig.builder();
        }

        mWebSocketServer = new WebSocketServer(mLogConfig.port, mLogConfig.wsPrefix);
        mWebSocketServer.setWsCanRead(mLogConfig.wsCanReceiveMsg);

        mWebSocketServer.setWebSocketServerCallback(new WebSocketServer.WebSocketServerCallback() {
            @Override
            public void onConnected(WebSocket webSocket) {
                try {
                    if (mCurrWebSocket != null) {
                        mCurrWebSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mCurrWebSocket = webSocket;

                if (mCurrWebSocket != null) {

                    mCurrWebSocket.setWebSocketCallback(new WebSocket.WebSocketCallback() {
                        @Override
                        public void onReceivedFrame(byte[] bytes) {
                            Log.e(TAG, "onReceivedFrame --> " + new String(bytes));
                        }

                        @Override
                        public void onClosed() {
                            Log.e(TAG, "onClosed --> " + mCurrWebSocket);
                            mCurrWebSocket = null;
                        }
                    });
                }
            }

            @Override
            public void onClosed() {

            }
        });

        mProcessOutputCallback = new ShellProcessThread.ProcessOutputCallback() {

            @Override
            public void onReaderLine(String line) {
                if (mCurrWebSocket != null && line != null) {
                    try {
                        mCurrWebSocket.send(line);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }


    public int getPort() {
        return mLogConfig.port;
    }


    public LogcatRunner config(LogConfig config) {
        mLogConfig = config;
        return this;
    }


    public void start() throws IOException {
        init();
        mWebSocketServer.start();
        startLogThread();
    }


    private void startLogThread() {
        try {
            if (mLogcatThread != null && mLogcatThread.isAlive()) {
                mLogcatThread.stopReader();
                mLogcatThread.setOutputCallback(null);
                mLogcatThread.interrupt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mLogcatThread = new ShellProcessThread(mLogConfig);
        mLogcatThread.setOutputCallback(mProcessOutputCallback);
        mLogcatThread.start();

    }

    public void stop() {
        try {

            if (mWebSocketServer != null) {
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

        private volatile boolean readerLogging = true;
        private ProcessOutputCallback mOutputCallback;
        private LogConfig logConfig;

        public ShellProcessThread(LogConfig logConfig) {
            this.logConfig = logConfig;
        }

        public void setOutputCallback(ProcessOutputCallback outputCallback) {
            mOutputCallback = outputCallback;
        }

        private File getLogFile() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String s = sdf.format(new Date());
            File f = new File(logConfig.logFileDir + "/logcat-" + s + ".log");
            if (!f.exists()) {
                try {
                    f.getParentFile().mkdir();
                    int retry = 3;
                    while (!f.createNewFile()) {
                        if (retry < 0) {
                            break;
                        }
                        SystemClock.sleep(100);
                        retry--;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return f;
        }

        @Override
        public void run() {
            Process exec = null;
            InputStream inputStream = null;
            BufferedReader reader = null;

            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            try {

                exec = Runtime.getRuntime().exec(logConfig.logcatCMD);
                inputStream = exec.getInputStream();

                try {
                    if (logConfig.write2File) {
                        File f = getLogFile();
                        Log.e(TAG, "--> write to file " + f);
                        fos = new FileOutputStream(f, true);
                        bos = new BufferedOutputStream(fos);
                        StringBuilder sb = new StringBuilder("\n\n\n---------------\n\n");
                        sb.append(new Date().toLocaleString());
                        sb.append("\n\n-------------------\n\n\n");
                        bos.write(sb.toString().getBytes());

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] newLine = "\n".getBytes();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                while (readerLogging) {
                    String line = reader.readLine();
                    if (mOutputCallback != null) {
                        mOutputCallback.onReaderLine(line);
                    }

                    try {
                        if (logConfig.write2File && bos != null && line != null) {
                            bos.write(line.getBytes());
                            bos.write(newLine);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (bos != null) {
                    bos.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fos != null && fos.getFD().valid()) {
                        fos.getFD().sync();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (bos != null) {
                        bos.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (bos != null) {
                        bos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (exec != null) {
                    exec.destroy();
                }
            }
        }

        public void stopReader() {
            readerLogging = false;
        }


        interface ProcessOutputCallback {
            void onReaderLine(String line);
        }
    }


    public static class LogConfig {

        private int port = 11229;
        private boolean write2File = false;
        private String logFileDir = Environment.getExternalStorageDirectory() + "/log";
        private String wsPrefix = "/logcat";
        private String logcatCMD = "logcat -v time";
        private boolean wsCanReceiveMsg=false;

        public static LogConfig builder() {
            return new LogConfig();
        }

        public LogConfig port(int port) {
            this.port = port;
            return this;
        }

        public LogConfig write2File(boolean write2File) {
            this.write2File = write2File;
            return this;
        }

        public LogConfig setLogFileDir(String logFileDir) {
            this.logFileDir = logFileDir;
            return this;
        }

        public LogConfig setWebsocketPrefix(String prefix) {
            this.wsPrefix = prefix;
            return this;
        }

        public LogConfig setWsCanReceiveMsg(boolean b){
            this.wsCanReceiveMsg=b;
            return this;
        }
    }
}
