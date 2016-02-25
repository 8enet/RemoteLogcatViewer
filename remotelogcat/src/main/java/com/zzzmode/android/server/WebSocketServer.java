package com.zzzmode.android.server;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by zl on 16/1/31.
 */
public class WebSocketServer {

    private ServerSocket mServerSocket;
    private WebSocketServerCallback mCallback;
    private int port=0;
    private volatile boolean isActive=false;

    private boolean wsCanRead=true;

    private static final Executor sExecutor= Executors.newCachedThreadPool();
    private List<WeakReference<WebSocket>> mListWebSocket=new ArrayList<WeakReference<WebSocket>>();
    private String mWebSocketPrefix;

    public WebSocketServer(int port,String prefix) throws IOException {
        this.port=port;
        mWebSocketPrefix=prefix;
    }

    public void setWsCanRead(boolean b){
        wsCanRead=b;
    }

    public void setWebSocketServerCallback(WebSocketServerCallback mCallback) {
        this.mCallback = mCallback;
    }


    public void start(){
        if(isActive){
            return;
        }

        sExecutor.execute(new Runnable() {
            @Override
            public void run() {
                innerStart();
            }
        });

    }

    private void innerStart(){
        try {
            mServerSocket=new ServerSocket(port);

            final InetAddress hostLANAddress = NetworkUtils.getLocalHostLANAddress();
            if(hostLANAddress != null){
                Log.e("WebSocketServer", "Server start success! Connection IP : "+hostLANAddress.getHostAddress()+":"+port);
            }else {
                Log.e("WebSocketServer", "Server start success! But unknow local ip address !");
            }

            isActive=true;
            while (isActive){
                handleSocket(mServerSocket.accept());
            }
        } catch (Exception e) {
            if(mCallback != null){
                mCallback.onClosed();
            }

            e.printStackTrace();
        }
    }


    public void stop(){
        isActive=false;
        try {
            for (WeakReference<WebSocket> webSockets:mListWebSocket){
                try{
                    WebSocket socket = webSockets.get();
                    if(socket != null){
                        socket.close();
                    }
                }catch (Exception e){
                }
            }

            if(mCallback != null){
                mCallback.onClosed();
            }

            if(mServerSocket!=null){
                mServerSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSocket(final Socket socket) {
        if(!isActive){
            return;
        }
        boolean handled=false;
        try {
            boolean isHttp=false;
            boolean isWebsocket=false;
            final Map<String, String> headerMap=parseHeader(socket.getInputStream());
            String s = headerMap.get(WebSocket.REQUEST_LINE);
            if(s!= null && s.startsWith("GET /")){
                isHttp=true;
                isWebsocket=s.startsWith("GET "+mWebSocketPrefix);
            }
            final Map<String, String> headers=headerMap;

            if(isWebsocket){

                sExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        handleAsWebsocket(socket,headers);
                    }
                });
                handled=true;
            }else if(isHttp){
                sExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        handleAsHttp(socket,headers);
                    }
                });
                handled=true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                //未知协议，不处理
                if (!handled) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void handleAsWebsocket(final Socket socket, final Map<String, String> headerMap) {

        try {

            WebSocket webSocket = new WebSocketImpl(socket, headerMap);
            mListWebSocket.add(new WeakReference<WebSocket>(webSocket));

            if (mCallback != null) {
                mCallback.onConnected(webSocket);
            }

            while (wsCanRead && !webSocket.isClosed()) {
                try {
                    webSocket.readFrame();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void handleAsHttp(final Socket socket, final Map<String, String> headerMap) {

        try {
            HttpResponse.handle(socket, headerMap);
            socket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static Map<String, String> parseHeader(InputStream inputStream) {
        LineInputStream lis = new LineInputStream(inputStream);
        Map<String, String> headerMap = new HashMap<String, String>();
        try {
            String line = lis.readLine();
            while (line != null && line.isEmpty()) {
                line = lis.readLine();
            }
            headerMap.put(WebSocket.REQUEST_LINE, line);
            line = lis.readLine();
            while (line != null && !line.isEmpty()) {
                int firstColonPos = line.indexOf(":");
                if (firstColonPos > 0) {
                    String key = line.substring(0, firstColonPos).trim();
                    int length = line.length();
                    String value = line.substring(firstColonPos + 1, length).trim();
                    if (!key.isEmpty() && !value.isEmpty()) {
                        headerMap.put(key, value);
                        headerMap.put(key.toLowerCase(), value);
                    }
                }
                line = lis.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.unmodifiableMap(headerMap);
    }

    public interface WebSocketServerCallback{
        void onConnected(WebSocket webSocket);
        void onClosed();
    }
}
