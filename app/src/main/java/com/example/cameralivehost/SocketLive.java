package com.example.cameralivehost;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SocketLive {
    private String TAG = "hucaihua";
    public interface SocketCallback {
        void callBack(byte[] data);
    }
    private WebSocket webSocket;
    private SocketCallback socketCallback;
    public SocketLive(SocketCallback socketCallback ) {
        this.socketCallback = socketCallback;
    }
    public void start() {
        webSocketServer.start();
    }
    public void close() {
        try {
            webSocketServer.stop();
            if (webSocket != null){
                webSocket.close();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(bytes);
        }
    }

    private WebSocketServer webSocketServer = new WebSocketServer(new InetSocketAddress(7001)) {
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            Log.d(TAG , "WebSocketServer onOpen");
            SocketLive.this.webSocket = conn;
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        }

        @Override
        public void onMessage(WebSocket conn, String message) {

        }
        //老张发送过来
        @Override
        public void onMessage(WebSocket conn, ByteBuffer bytes) {
            Log.i("David", "消息长度  : " + bytes.remaining());
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            socketCallback.callBack(buf);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            ex.printStackTrace();
        }

        @Override
        public void onStart() {

        }
    };
}
