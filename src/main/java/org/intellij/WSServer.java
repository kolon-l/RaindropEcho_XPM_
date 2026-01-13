package org.intellij;

import burp.api.montoya.MontoyaApi;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
 * WSServer - WebSocket服务器类
 * 
 * <p>实现WebSocket服务器，用于与外部加解密服务进行通信。
 * 该服务器支持：
 * <ul>
 *   <li>多客户端并发连接</li>
 *   <li>请求-响应同步通信模式</li>
 *   <li>超时控制</li>
 *   <li>连接状态监控</li>
 * </ul>
 * 
 * <p>主要用途：
 * <ul>
 *   <li>接收加解密请求</li>
 *   <li>转发请求到外部加解密服务</li>
 *   <li>接收并返回加解密结果</li>
 *   <li>管理多个客户端连接</li>
 * </ul>
 * 
 * @author RaindropEcho
 * @version 1.0
 */
public class WSServer extends WebSocketServer {

    /**
     * 存储每个连接的响应队列
     * 
     * <p>使用ConcurrentHashMap保证线程安全，每个WebSocket连接对应一个BlockingQueue，
     * 用于存储该连接的响应消息。
     */
    private final ConcurrentHashMap<WebSocket, BlockingQueue<String>> responseQueues = new ConcurrentHashMap<>();
    
    /**
     * Burp Suite Montoya API实例
     */
    private MontoyaApi api;
    
    /**
     * 构造函数
     * 
     * @param address WebSocket服务器监听地址
     * @param montoyaApi Burp Suite Montoya API实例
     */
    public WSServer(InetSocketAddress address, MontoyaApi montoyaApi) {
        super(address);
        this.api = montoyaApi;
    }
    
    /**
     * 客户端连接建立时的回调方法
     * 
     * <p>当有新的WebSocket连接建立时，此方法会被调用。
     * 
     * @param webSocket 新建立的WebSocket连接
     * @param clientHandshake 客户端握手信息
     */
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        api.logging().logToOutput("Connected: " + webSocket.getRemoteSocketAddress());
    }
    
    /**
     * 客户端连接关闭时的回调方法
     * 
     * <p>当WebSocket连接关闭时，此方法会被调用。
     * 
     * @param webSocket 关闭的WebSocket连接
     * @param i 关闭状态码
     * @param s 关闭原因
     * @param b 是否由远程端发起关闭
     */
    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        api.logging().logToOutput("Closed: " + webSocket.getRemoteSocketAddress());
    }
    
    /**
     * 接收到客户端消息时的回调方法
     * 
     * <p>当接收到客户端发送的消息时，此方法会被调用。
     * 消息会被放入对应连接的响应队列中，供sendAndWaitResponse方法获取。
     * 
     * @param webSocket 发送消息的WebSocket连接
     * @param s 接收到的消息内容
     */
    public void onMessage(WebSocket webSocket, String s) {
        BlockingQueue<String> queue = responseQueues.get(webSocket);
        if (queue != null) {
            queue.offer(s);
        }
    }
    
    /**
     * 发生错误时的回调方法
     * 
     * <p>当WebSocket连接发生错误时，此方法会被调用。
     * 
     * @param webSocket 发生错误的WebSocket连接
     * @param e 错误异常对象
     */
    @Override
    public void onError(WebSocket webSocket, Exception e) {
        api.logging().logToError("Error: " + e.getMessage());
    }
    
    /**
     * 服务器启动时的回调方法
     * 
     * <p>当WebSocket服务器成功启动时，此方法会被调用。
     */
    @Override
    public void onStart() {}
    
    /**
     * 发送消息并等待响应
     * 
     * <p>此方法实现了同步的请求-响应模式：
     * <ul>
     *   <li>为指定连接创建或获取响应队列</li>
     *   <li>清空队列中的旧数据</li>
     *   <li>发送消息到WebSocket连接</li>
     *   <li>阻塞等待响应，直到超时</li>
     * </ul>
     * 
     * @param conn WebSocket连接
     * @param data 要发送的数据
     * @param timeout 超时时间
     * @param unit 超时时间单位
     * @return 响应数据
     * @throws InterruptedException 如果等待被中断
     * @throws TimeoutException 如果等待超时
     */
    public String sendAndWaitResponse(WebSocket conn, String data, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        BlockingQueue<String> queue = responseQueues.computeIfAbsent(conn, k -> new LinkedBlockingQueue<>());
        queue.clear();
        conn.send(data);
        String response = queue.poll(timeout, unit);
        if (response == null) {
            throw new TimeoutException("等待响应超时");
        }
        return response;
    }
}