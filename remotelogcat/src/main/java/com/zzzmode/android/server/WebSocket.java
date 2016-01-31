package com.zzzmode.android.server;

import java.io.IOException;

/**
 * Created by zl on 16/1/31.
 */
public interface WebSocket {
    public static final String REQUEST_LINE = "REQUEST_LINE";

    public static final int LENGTH_16 = 0x7E;

    public static final int LENGTH_16_MIN = 126;


    public static final int LENGTH_64 = 0x7F;


    public static final int LENGTH_64_MIN = 0x10000;


    public static final int MASK_HIGH_WORD_HIGH_BYTE_NO_SIGN = 0x7f000000;


    public static final int MASK_HIGH_WORD_LOW_BYTE = 0x00ff0000;


    public static final int MASK_LOW_WORD_HIGH_BYTE = 0x0000ff00;


    public static final int MASK_LOW_WORD_LOW_BYTE = 0x000000ff;


    public static final int OCTET_ONE = 8;


    public static final int OCTET_TWO = 16;


    public static final int OCTET_THREE = 24;


    public static final int OPCODE_FRAME_BINARY = 0x82;


    public static final int OPCODE_FRAME_CLOSE = 0x88;


    public static final int OPCODE_FRAME_PONG = 0x8A;


    public static final int OPCODE_FRAME_TEXT = 0x81;

    public static final String WEBSOCKET_ACCEPT_UUID =
            "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";


    public static final String DEFAULT_CHARSET = "UTF-8";


    void send(byte[] bytes) throws IOException;
    void send(String text)throws IOException;
    byte[] readFrame()throws IOException;
    void close() throws IOException;
    boolean isClosed();
    boolean isConnected();

    void setWebSocketCallback(WebSocketCallback callback);

    public interface WebSocketCallback{
        void onReceivedFrame(byte[] bytes);
        void onClosed();
    }
}
