package com.zzzmode.android.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Map;

/**
 * Created by zl on 16/1/31.
 */
class WebSocketImpl implements WebSocket {

    private Socket mSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private boolean handshakeComplete = false;
    private boolean closed = false;
    private boolean closeSent = false;

    private WebSocketCallback mCallback;

    public WebSocketImpl(Socket socket,Map<String, String> headers) throws IOException {
        this.mSocket = socket;
        init(headers);
    }

    private void init(Map<String, String> headers) throws IOException {
        inputStream = mSocket.getInputStream();
        outputStream = mSocket.getOutputStream();
        shakeHands(headers);
    }

    //先处理websocket握手
    private void shakeHands(Map<String, String> headers) throws IOException {
        //Map<String, String> headers = parseHeader(inputStream);
        String requestLine = headers.get(REQUEST_LINE);

        handshakeComplete = checkStartsWith(requestLine, "GET /")
                && checkContains(requestLine, "HTTP/")
                && headers.get("Host") != null
                && checkContains(headers.get("upgrade"), "websocket")
                && checkContains(headers.get("connection"), "Upgrade")
                && "13".equals(headers.get("sec-websocket-version"))
                && headers.get("sec-websocket-key") != null;
        String nonce = headers.get("sec-websocket-key");
        //根据客户端请求的seckey生成一个acceptkey,
        // 具体做法是先提取seckey+[websocket公开的一个uuid,这个uuid是固定的]
        // 拼接成一个字符串计算sha1值

        if (handshakeComplete) {
            byte[] nonceBytes = Base64.decode(nonce);
            if (nonceBytes == null ||nonceBytes.length != 16) {
                handshakeComplete = false;
            }
        }
        // if we have met all the requirements
        if (handshakeComplete) {
            //返回请求头，表示握手完成，之后每次以frame为单位传输数据
            outputStream.write(asUTF8("HTTP/1.1 101 Switching Protocols\r\n"));
            outputStream.write(asUTF8("Upgrade: websocket\r\n"));
            outputStream.write(asUTF8("Connection: upgrade\r\n"));
            outputStream.write(asUTF8("Sec-WebSocket-Accept: "));
            byte[] hashByte = EncodeUtils.sha1(nonce, WEBSOCKET_ACCEPT_UUID);
            String acceptKey = Base64.encode(hashByte);
            outputStream.write(asUTF8(acceptKey));
            outputStream.write(asUTF8("\r\n\r\n"));
        }

    }

    private void readFully(byte[] b) throws IOException {
        int readen = 0;
        while (readen < b.length) {
            int r = inputStream.read(b, readen, b.length - readen);
            if (r == -1)
                break;
            readen += r;
        }
    }

    @Override
    public byte[] readFrame() throws IOException {

        int opcode = inputStream.read();
        //boolean whole = (opcode & 0b10000000) !=0;
        opcode = opcode & 0xF;

        if (opcode != 1) {
            finshSocket();
            throw new IOException("Wrong opcode: " + opcode);
        }
        //读取消息内容长度的有多种情况
        int len = inputStream.read();
        boolean encoded = (len >= 128);

        if (encoded) {
            len -= 128;
        }

        if (len == 127) {
            len = (inputStream.read() << 16) | (inputStream.read() << 8) | inputStream.read();
        } else if (len == 126) {
            len = (inputStream.read() << 8) | inputStream.read();
        }

        byte[] key = null;

        if (encoded) {
            key = new byte[4];
            readFully(key);
        }

        byte[] frame = new byte[len];

        readFully(frame);

        if (encoded) {
            for (int i = 0; i < frame.length; i++) {
                frame[i] = (byte) (frame[i] ^ key[i % 4]);
            }
        }
        if (mCallback != null) {
            mCallback.onReceivedFrame(frame);
        }
        return frame;
    }


    private void writeData(byte[] bytes, int opcode) throws IOException {
        try {
            int binLength = bytes.length;
            outputStream.write(opcode);
            if (binLength < LENGTH_16_MIN) {
                outputStream.write(binLength);
            } else if (binLength < LENGTH_64_MIN) {
                outputStream.write(LENGTH_16);
                outputStream.write(
                        (binLength & MASK_LOW_WORD_HIGH_BYTE) >> OCTET_ONE);
                outputStream.write(binLength & MASK_LOW_WORD_LOW_BYTE);
            } else {
                outputStream.write(LENGTH_64);
                outputStream.write(0x00);
                outputStream.write(0x00);
                outputStream.write(0x00);
                outputStream.write(0x00);
                outputStream.write(
                        (binLength & MASK_HIGH_WORD_HIGH_BYTE_NO_SIGN) >> OCTET_THREE);
                outputStream.write(
                        (binLength & MASK_HIGH_WORD_LOW_BYTE) >> OCTET_TWO);
                outputStream.write(
                        (binLength & MASK_LOW_WORD_HIGH_BYTE) >> OCTET_ONE);
                outputStream.write(binLength & MASK_LOW_WORD_LOW_BYTE);
            }
            outputStream.write(bytes);
        } catch (IOException e) {
            finshSocket();
            throw e;
        }
    }

    public final void writeClose() throws IOException {
        if (!closeSent) {
            closeSent = true;
            outputStream.write(new byte[]{
                    (byte) OPCODE_FRAME_CLOSE, (byte) 0x00
            });
        }
    }


    @Override
    public void send(byte[] bytes) throws IOException {
        writeData(bytes, OPCODE_FRAME_BINARY);
    }

    @Override
    public void send(String text) throws IOException {
        byte[] utfBytes = asUTF8(text);
        writeData(utfBytes, OPCODE_FRAME_TEXT);
    }

    @Override
    public void close() {
        finshSocket();
    }

    public boolean isClosed() {
        return closed || closeSent;
    }

    @Override
    public boolean isConnected() {
        return handshakeComplete;
    }

    @Override
    public void setWebSocketCallback(WebSocketCallback callback) {
        mCallback = callback;
    }


    public final void finshSocket() {
        closed = true;
        handshakeComplete = false;

        if (mCallback != null) {
            mCallback.onClosed();
        }

        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static byte[] asUTF8(final String s) {
        try {
            return s.getBytes(DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public static boolean checkContains(final String s1, final String s2) {
        return s1 != null && s1.contains(s2);
    }

    public static boolean checkStartsWith(final String s1, final String s2) {
        return s1 != null && s1.startsWith(s2);
    }

}
