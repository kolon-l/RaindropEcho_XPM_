package org.intellij;

import burp.api.montoya.MontoyaApi;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

public class WSServer extends WebSocketServer {

    // 存储每个连接的响应队列
    private final ConcurrentHashMap<WebSocket, BlockingQueue<String>> responseQueues = new ConcurrentHashMap<>();
    private MontoyaApi api;
    public WSServer(InetSocketAddress address, MontoyaApi montoyaApi) {
        super(address);
        this.api = montoyaApi;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        api.logging().logToOutput("Connected: "+webSocket.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        api.logging().logToOutput("Closed: "+webSocket.getRemoteSocketAddress());
    }


    public void onMessage(WebSocket webSocket, String s) {
        api.logging().logToOutput("Message: "+s);
        BlockingQueue<String> queue = responseQueues.get(webSocket);
        if (queue != null) {
            queue.offer(s); // 非阻塞添加
        }
    }


    @Override
    public void onError(WebSocket webSocket, Exception e) {
        api.logging().logToError("Error: "+e.getMessage());
    }

    @Override
    public void onStart() {
    }

    public String sendAndWaitResponse(WebSocket conn, String data, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        // 创建或获取该连接的队列
        BlockingQueue<String> queue = responseQueues.computeIfAbsent(conn, k -> new LinkedBlockingQueue<>());
        // 清空队列中旧数据（避免历史消息干扰）
        queue.clear();
        // 发送消息
        conn.send(data);
        // 阻塞等待响应（带超时）
        String response = queue.poll(timeout, unit);
        if (response == null) {
            throw new TimeoutException("等待响应超时");
        }
        return response;
    }
}