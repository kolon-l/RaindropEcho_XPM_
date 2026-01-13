package org.intellij;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import org.java_websocket.WebSocket;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.net.HttpURLConnection;
/**
 * 自动化加解密工具类
 * 
 * <p>提供多种加解密处理方式，包括：
 * <ul>
 *   <li>JSFile模式：通过Node.js执行JavaScript文件进行加解密</li>
 *   <li>API模式：通过HTTP API调用进行加解密</li>
 *   <li>WebSocket模式：通过WebSocket连接进行加解密</li>
 * </ul>
 * 
 * <p>支持的功能：
 * <ul>
 *   <li>单字段加解密：对匹配正则表达式的单个字段进行加解密</li>
 *   <li>多字段聚合加解密：聚合多个标记字段后统一加解密</li>
 *   <li>右键菜单快速加解密</li>
 *   <li>自动处理HTTP请求和响应</li>
 * </ul>
 * 
 * @author RaindropEcho
 * @version 1.0
 */
public class AutoUtil {

    private final MontoyaApi api;
    
    /**
     * 构造函数
     * @param montoyaApi Burp Suite Montoya API实例
     */
    AutoUtil(MontoyaApi montoyaApi) {
        api = montoyaApi;
    }
    
    /**
     * 处理右键菜单来源的加解密请求（简化版）
     * 
     * <p>此方法用于处理用户通过右键菜单触发的加解密操作，不使用正则匹配，
     * 直接对整个数据进行加解密处理。
     * 
     * @param data 待处理的数据
     * @param ende_config 加解密配置（API地址、JS文件路径或WebSocket地址）
     * @param mode_config 处理模式：API、JSFile 或 WebSocket
     * @param isencrypt 是否为加密操作（true=加密，false=解密）
     * @param isFromRightButton 是否来自右键菜单
     * @return 处理后的数据
     */
    public String matchFunc(String data, String ende_config, String mode_config, 
                           boolean isencrypt, boolean isFromRightButton) {
        String result = data;
        result = matchFunc(data, ende_config, "", "", mode_config, isencrypt, isFromRightButton, false);
        return result;
    }

    /**
     * 处理HTTP请求/响应拦截来源的加解密请求（简化版）
     * 
     * <p>此方法用于处理HTTP请求和响应拦截时的加解密操作，支持正则表达式匹配
     * 对特定字段进行加解密。
     * 
     * @param data 待处理的数据
     * @param ende_config 加解密配置（API地址、JS文件路径或WebSocket地址）
     * @param reg 正则表达式，用于匹配需要加解密的数据
     * @param tag 标签参数（"0"=不保留前后缀，其他=保留前后缀）
     * @param mode_config 处理模式：API、JSFile 或 WebSocket
     * @param isencrypt 是否为加密操作（true=加密，false=解密）
     * @return 处理后的数据
     */
    public String matchFunc(String data, String ende_config, String reg, String tag, 
                           String mode_config, boolean isencrypt) {
        String result = data;
        result = matchFunc(data, ende_config, reg, tag, mode_config, isencrypt, false, false);
        return result;
    }

    /**
     * 核心加解密处理方法（完整版）
     * 
     * <p>此方法是所有加解密操作的统一入口，根据不同的参数组合执行不同的处理逻辑：
     * <ul>
     *   <li>isMuti=true：执行多字段聚合加解密</li>
     *   <li>isFromRightButton=true：执行右键菜单加解密，不使用正则匹配</li>
     *   <li>isFromRightButton=false：执行正则匹配加解密，对匹配的字段进行处理</li>
     * </ul>
     * 
     * @param data 待处理的数据
     * @param ende_config 加解密配置（API地址、JS文件路径或WebSocket地址）
     * @param reg 正则表达式，用于匹配需要加解密的数据
     * @param tag 标签参数（"0"=不保留前后缀，其他=保留前后缀）
     * @param mode_config 处理模式：API、JSFile 或 WebSocket
     * @param isencrypt 是否为加密操作（true=加密，false=解密）
     * @param isFromRightButton 是否来自右键菜单
     * @param isMuti 是否为多字段聚合模式
     * @return 处理后的数据
     */
    public String matchFunc(String data, String ende_config, String reg, String tag, 
                           String mode_config, boolean isencrypt, boolean isFromRightButton, boolean isMuti) {
        String result = data;

        if (isMuti) {
            return aggregateAndProcess(data, ende_config, reg, tag, mode_config, isencrypt, "0".equals(tag));
        }

        if (isFromRightButton) {
            if (isencrypt) {
                try {
                    if ("API".equals(mode_config)) {
                        result = call_http_encryption_function(ende_config, data, true);
                        result = result.substring(0, result.length() - 1);
                    } else if ("JSFile".equals(mode_config)) {
                        result = call_js_endecryption_function(ende_config, data, true);
                    } else if ("WebSocket".equals(mode_config)) {
                        result = call_websocket_endecryption_function(ende_config, data, true);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    if ("API".equals(mode_config)) {
                        result = call_http_encryption_function(ende_config, data, false, isFromRightButton);
                        result = result.substring(0, result.length() - 1);
                    } else if ("JSFile".equals(mode_config)) {
                        result = call_js_endecryption_function(ende_config, data, false, isFromRightButton);
                    } else if ("WebSocket".equals(mode_config)) {
                        result = call_websocket_endecryption_function(ende_config, data, false, isFromRightButton);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return result;
        } else {
            Pattern pattern = Pattern.compile(reg);
            Matcher matche = pattern.matcher(data);

            if (isencrypt) {
                StringBuffer resultBuffer = new StringBuffer();
                while (matche.find()) {
                    if ("0".equals(tag)) {
                        String encrypt = null;
                        String g1 = matche.group(1);
                        try {
                            if ("API".equals(mode_config)) {
                                encrypt = call_http_encryption_function(ende_config, g1, true);
                                encrypt = encrypt.substring(0, encrypt.length() - 1);
                            } else if ("JSFile".equals(mode_config)) {
                                encrypt = call_js_endecryption_function(ende_config, g1, true);
                            } else if ("WebSocket".equals(mode_config)) {
                                encrypt = call_websocket_endecryption_function(ende_config, g1, true);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        matche.appendReplacement(resultBuffer, Matcher.quoteReplacement(encrypt));
                    } else {
                        String encrypt = null;
                        try {
                            if ("API".equals(mode_config)) {
                                encrypt = call_http_encryption_function(ende_config, matche.group(1), true);
                                encrypt = encrypt.substring(0, encrypt.length() - 1);
                            } else if ("JSFile".equals(mode_config)) {
                                encrypt = call_js_endecryption_function(ende_config, matche.group(1), true);
                            } else {
                                encrypt = call_websocket_endecryption_function(ende_config, matche.group(1), true);
                            }
                            String S1 = matche.group(1);
                            String S2 = matche.group(0);
                            matche.appendReplacement(resultBuffer, Matcher.quoteReplacement((S2 + S1).split(S1)[0] + encrypt + (S2 + S1).split(S1)[1]));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                matche.appendTail(resultBuffer);
                return resultBuffer.toString();
            } else {
                result = "body处理失败";
                try {
                    if ("API".equals(mode_config)) {
                        result = call_http_encryption_function(ende_config, data, false);
                        result = result.substring(0, result.length() - 1);
                    } else if ("JSFile".equals(mode_config)) {
                        result = call_js_endecryption_function(ende_config, data, false);
                    } else {
                        result = call_websocket_endecryption_function(ende_config, data, false);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return result;
            }
        }
    }
    
    /**
     * 调用JavaScript加解密函数（简化版）
     * 
     * <p>通过Node.js执行指定的JavaScript文件进行加解密操作。
     * 
     * @param jsFile JavaScript文件路径
     * @param data 待处理的数据
     * @param isencrypt 是否为加密操作（true=加密，false=解密）
     * @return 处理后的数据
     * @throws Exception 执行失败时抛出异常
     */
    public String call_js_endecryption_function(String jsFile, String data, Boolean isencrypt) throws Exception {
        return call_js_endecryption_function(jsFile, data, isencrypt, false);
    }

    /**
     * 调用JavaScript加解密函数（完整版）
     * 
     * <p>通过Node.js执行指定的JavaScript文件进行加解密操作。
     * 支持不同的调用模式：右键菜单、自动拦截等。
     * 
     * @param jsFile JavaScript文件路径
     * @param data 待处理的数据
     * @param isencrypt 是否为加密操作（true=加密，false=解密）
     * @param isFromRightButton 是否来自右键菜单
     * @return 处理后的数据
     * @throws Exception 执行失败时抛出异常
     */
    public String call_js_endecryption_function(String jsFile, String data, Boolean isencrypt, Boolean isFromRightButton) throws Exception {

        List<String> command = new ArrayList<>();
        command.add("node");
        command.add(jsFile);
        if (isencrypt) {
            command.add("encrypt_c");
        } else {
            if (isFromRightButton) {
                command.add("decrypt_c_RB");
            } else {
                command.add("decrypt_c");
            }
        }
        command.add("\"" + data.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        String outData = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String stdout = reader.lines().collect(Collectors.joining("\n"));
        String stderr = errorReader.lines().collect(Collectors.joining("\n"));
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("call JS function:" + jsFile + ". Error: " + stderr);
        }
        outData = stdout;

        return outData;
    }

    /**
     * 发送HTTP POST请求
     * 
     * <p>向指定的URL发送POST请求，并返回响应内容。
     * 
     * @param targetUrl 目标URL
     * @param data 请求体数据
     * @return 响应内容
     * @throws IOException 网络异常
     */
    public String sendPost(String targetUrl, String data) throws IOException {

        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = data.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int status = conn.getResponseCode();
        InputStream inputStream;
        if (status >= 200 && status < 300) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();
        }

        String response = inputStreamToString(inputStream);
        conn.disconnect();
        return response;
    }

    /**
     * 将输入流转换为字符串
     * 
     * @param inputStream 输入流
     * @return 字符串内容
     * @throws IOException IO异常
     */
    private String inputStreamToString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line).append(System.lineSeparator());
            }
        }
        if (response.length() > 0) {
            response.setLength(response.length() - 1);
        }
        return response.toString();
    }

    /**
     * 调用HTTP API加解密函数（简化版）
     * 
     * <p>通过HTTP API调用进行加解密操作。
     * 
     * @param url API地址
     * @param data 待处理的数据
     * @param isencrypt 是否为加密操作（true=加密，false=解密）
     * @return 处理后的数据
     * @throws Exception 调用失败时抛出异常
     */
    public String call_http_encryption_function(String url, String data, Boolean isencrypt) throws Exception {
        return call_http_encryption_function(url, data, isencrypt, false);
    }

    /**
     * 调用HTTP API加解密函数（完整版）
     * 
     * <p>通过HTTP API调用进行加解密操作。
     * 支持不同的调用模式：右键菜单、自动拦截等。
     * 
     * @param url API地址
     * @param data 待处理的数据
     * @param isencrypt 是否为加密操作（true=加密，false=解密）
     * @param isFromRightButton 是否来自右键菜单
     * @return 处理后的数据
     * @throws Exception 调用失败时抛出异常
     */
    public String call_http_encryption_function(String url, String data, Boolean isencrypt, Boolean isFromRightButton) throws Exception {
        String outData = "";
        try {
            if (isencrypt) {
                outData = sendPost(url + "/encrypt", data);
            } else {
                if (isFromRightButton) {
                    outData = sendPost(url + "/decrypt_RB", data);
                } else {
                    outData = sendPost(url + "/decrypt", data);
                }
            }
        } catch (Exception e) {
            api.logging().logToError("HTTP调用失败: " + url + ", 错误: " + e.toString());
        }
        return outData;
    }

//  存储所有启用的websocket服务端
    private ArrayList<WSServer> wsServers = new ArrayList<WSServer>();

    public boolean addWSServer(String target)  {
        try {
            WebSocketURLParser.WebSocketURLInfo url =WebSocketURLParser.parseWebSocketURL(target);
            InetSocketAddress addr = new InetSocketAddress(url.getHost(), url.getPort());
            WSServer wsServer = new WSServer(addr, api);
            wsServer.start();
            wsServers.add(wsServer);
            return true;
        }
        catch (Exception e) {
            api.logging().logToError("添加WebSocket服务器失败: " + target + ", 错误: " + e.toString());
            return false;
        }
    }

    public boolean deleteWSServer(String target){
        try {
            WebSocketURLParser.WebSocketURLInfo url =WebSocketURLParser.parseWebSocketURL(target);
            InetSocketAddress addr = new InetSocketAddress(url.getHost(), url.getPort());
            WSServer wsServer = new WSServer(addr, api);
            for (WSServer server : wsServers) {
                if (server.getAddress().getHostString().equals(wsServer.getAddress().getHostString())) {
                    server.stop();
                    return true;
                }
            }
            throw new Exception("Server not found");
        }
        catch (Exception e) {
            api.logging().logToError("删除WebSocket服务器失败: " + target + ", 错误: " + e.toString());
            return false;
        }
    }

    public boolean checkWSServer(String target)  {
        try {
            WebSocketURLParser.WebSocketURLInfo url =WebSocketURLParser.parseWebSocketURL(target);
            InetSocketAddress addr = new InetSocketAddress(url.getHost(), url.getPort());
            WSServer wsServer = new WSServer(addr, api);
            for (WSServer server : wsServers) {
                if (server.getAddress().getHostString().equals(wsServer.getAddress().getHostString())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            api.logging().logToError("检查WebSocket服务器失败: " + target + ", 错误: " + e.toString());
            return false;
        }
    }

    public void deleteWSServers() throws InterruptedException {
        for (WSServer wsServer : wsServers) {
            wsServer.stop();
        }
    }

    public void listWSServers() {
        for (WSServer wsServer : wsServers) {
            api.logging().logToOutput("WebSocket服务器: " + wsServer.getAddress().getHostString());
        }
    }

    public class WebSocketURLParser {

        // 定义正则表达式匹配 WebSocket URL
        private static final Pattern WS_URL_PATTERN = Pattern.compile(
                "^(wss?://)?" +                     // 协议（可选）
                        "([^:/?#]+)" +                       // 主机（IPv4、域名等）
                        "(?::(\\d+))?" +                     // 端口（可选）
                        "([/?#].*)?$"                        // 路径、查询参数等（忽略）
        );

        // 解析 URL 并返回结果对象
        public static WebSocketURLInfo parseWebSocketURL(String url) {
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("WebSocket URL is null or empty");
            }

            // 强制小写处理协议部分（避免大小写混合输入）
            url = url.toLowerCase();

            // 匹配正则表达式
            Matcher matcher = WS_URL_PATTERN.matcher(url);
            if (!matcher.find()) {
                throw new IllegalArgumentException(" WebSocket URL Type Error");
            }

            // 提取协议（默认 ws）
            String protocol = "ws";
            if (url.startsWith("wss://")) {
                protocol = "wss";
            } else if (!url.startsWith("ws://")) {
                throw new IllegalArgumentException("Protocol is not ws:// or wss://");
            }

            // 提取主机和端口
            String host;
            int port;

            // 截取协议后的部分（主机:端口/路径）
            String hostPortPart = url.substring(protocol.length() + 3); // 跳过 "ws://" 或 "wss://"

            // 分离主机和端口
            int portSeparatorIndex = hostPortPart.indexOf(':');
            int pathSeparatorIndex = hostPortPart.indexOf('/');

            if (portSeparatorIndex != -1) {
                // 存在端口号
                host = hostPortPart.substring(0, portSeparatorIndex);
                String portStr = hostPortPart.substring(portSeparatorIndex + 1,
                        (pathSeparatorIndex != -1) ? pathSeparatorIndex : hostPortPart.length());
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("端口号必须是数字");
                }
            } else {
                // 无端口号，使用默认端口
                host = (pathSeparatorIndex != -1) ?
                        hostPortPart.substring(0, pathSeparatorIndex) : hostPortPart;
                port = protocol.equals("wss") ? 443 : 80;
            }

            return new WebSocketURLInfo(protocol, host, port);
        }

        // 结果封装对象
        public static class WebSocketURLInfo {
            private final String protocol;
            private final String host;
            private final int port;

            public WebSocketURLInfo(String protocol, String host, int port) {
                this.protocol = protocol;
                this.host = host;
                this.port = port;
            }

            public String getProtocol() {
                return protocol;
            }

            public String getHost() {
                return host;
            }

            public int getPort() {
                return port;
            }

            @Override
            public String toString() {
                return String.format("%s://%s:%d", protocol, host, port);
            }
        }
    }


    public String call_websocket_endecryption_function(String target, String data,Boolean isencrypt) throws Exception {
        return call_websocket_endecryption_function(target,data,isencrypt,false);
    }
    public String call_websocket_endecryption_function(String target, String data,Boolean isencrypt,Boolean isFromRightButton) throws Exception {

        WebSocketURLParser.WebSocketURLInfo url =WebSocketURLParser.parseWebSocketURL(target);

        InetSocketAddress address = new InetSocketAddress(url.getHost(), url.getPort());

        String output = "";
        WSServer server_now = new WSServer(address,api);

        listWSServers();
        for (WSServer server : wsServers) {
            if (server.getAddress().getHostString().equals(server_now.getAddress().getHostString())) {
                for (WebSocket conn : server.getConnections()) {

                    if (conn.isOpen()) {
                        try {
                            // 根据条件发送消息并等待响应

                            if (isencrypt && conn.getResourceDescriptor().contains("encrypt")) {
                                String response = data;
                                synchronized(conn) {
                                    response = server.sendAndWaitResponse(conn, data, 30, TimeUnit.SECONDS);
                                }
                                output = response;
                                break; // 获取第一个响应后退出
                            }
                            if (!isencrypt && conn.getResourceDescriptor().contains("decrypt_RB")) {
                                String response = data;
                                synchronized(conn) {
                                    response = server.sendAndWaitResponse(conn, data, 30, TimeUnit.SECONDS);
                                }
                                output = response;
                                break;
                            }
                            if (!isencrypt && conn.getResourceDescriptor().contains("decrypt")) {
                                String response = data;
                                synchronized(conn) {
                                    response = server.sendAndWaitResponse(conn, data, 30, TimeUnit.SECONDS);
                                }
                                output = response;
                                break;
                            }
                        } catch (InterruptedException | TimeoutException e) {
                            api.logging().logToError("WebSocket响应获取失败: " + e.getMessage());
                        }
                    }
                }
            }
        }
        return output;
    }

    // 聚合插入位置标记，用于标识聚合结果插入的位置
    private static final String AGGREGATE_INSERT_MARKER = "(-AGGREGATE-)";
    // 聚合数据分隔符，用于分隔多个待聚合的数据项
    private static final String AGGREGATE_SEPARATOR = "§";

    /**
     * 标记数据内部类，用于存储从原始数据中提取的标记信息
     */
    private static class MarkedData {
        String fullMatch;      // 完整匹配的字符串
        String data;           // 提取出的核心数据
        String prefix;         // 数据前缀
        String suffix;         // 数据后缀
        int startIndex;        // 在原始数据中的起始位置
        int endIndex;          // 在原始数据中的结束位置

        MarkedData(String fullMatch, String data, String prefix, String suffix, int startIndex, int endIndex) {
            this.fullMatch = fullMatch;
            this.data = data;
            this.prefix = prefix;
            this.suffix = suffix;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    /**
     * 聚合并处理数据
     * @param data 原始数据，必须包含聚合插入位置标记
     * @param ende_config 加解密配置（API地址、JS文件路径或WebSocket地址）
     * @param reg 正则表达式，用于匹配需要聚合的数据
     * @param tag 标签参数
     * @param mode_config 处理模式：API、JSFile 或 WebSocket
     * @param isencrypt 是否为加密操作（true=加密，false=解密）
     * @param keepPrefixSuffix 是否保留前后缀
     * @return 处理后的数据，聚合结果会插入到标记位置
     */
    public String aggregateAndProcess(String data, String ende_config, String reg, 
                                   String tag, String mode_config, boolean isencrypt, boolean keepPrefixSuffix) {
        // 检查数据中是否包含聚合插入位置标记
        if (!data.contains(AGGREGATE_INSERT_MARKER)) {
            api.logging().logToError("聚合处理失败：未找到插入位置标记 " + AGGREGATE_INSERT_MARKER);
            return data;
        }

        // 从原始数据中提取所有标记的数据
        List<MarkedData> markedDataList = extractAllMarkedData(data, reg, tag);

        // 如果没有找到任何标记数据，直接返回原始数据
        if (markedDataList.isEmpty()) {
            return data;
        }

        // 将所有标记的数据聚合为一个字符串
        String aggregatedData = aggregateMarkedData(markedDataList);

        // 根据配置模式调用相应的加解密函数处理聚合数据
        String processedResult;
        try {
            if ("API".equals(mode_config)) {
                processedResult = call_http_encryption_function(ende_config, aggregatedData, isencrypt);
                processedResult = processedResult.substring(0, processedResult.length() - 1);
            } else if ("JSFile".equals(mode_config)) {
                processedResult = call_js_endecryption_function(ende_config, aggregatedData, isencrypt);
            } else if ("WebSocket".equals(mode_config)) {
                processedResult = call_websocket_endecryption_function(ende_config, aggregatedData, isencrypt);
            } else {
                throw new IllegalArgumentException("Unknown mode: " + mode_config);
            }
        } catch (Exception e) {
            api.logging().logToError("聚合处理失败: " + e.getMessage());
            throw new RuntimeException("聚合处理失败", e);
        }

        // 将处理结果插入到原始数据的标记位置
        return insertAggregatedResult(data, markedDataList, processedResult, tag, keepPrefixSuffix);
    }

    /**
     * 从原始数据中提取所有符合正则表达式的标记数据
     * @param data 原始数据
     * @param reg 正则表达式，用于匹配数据
     * @param tag 标签参数（当前未使用）
     * @return 标记数据列表
     */
    private List<MarkedData> extractAllMarkedData(String data, String reg, String tag) {
        List<MarkedData> markedDataList = new ArrayList<>();
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(data);

        // 遍历所有匹配项
        while (matcher.find()) {
            String fullMatch = matcher.group(0);      // 完整匹配的字符串
            String extractedData = matcher.group(1);  // 第一个捕获组（核心数据）
            // 提取前缀和后缀
            String prefix = fullMatch.substring(0, fullMatch.indexOf(extractedData));
            String suffix = fullMatch.substring(fullMatch.indexOf(extractedData) + extractedData.length());

            // 排除聚合插入位置标记本身
            if (!fullMatch.equals(AGGREGATE_INSERT_MARKER)) {
                markedDataList.add(new MarkedData(fullMatch, extractedData, prefix, suffix, 
                                                 matcher.start(), matcher.end()));
            }
        }

        return markedDataList;
    }

    /**
     * 将标记数据列表中的核心数据聚合为一个字符串
     * @param markedDataList 标记数据列表
     * @return 聚合后的字符串，各数据项之间用分隔符连接
     */
    private String aggregateMarkedData(List<MarkedData> markedDataList) {
        return markedDataList.stream()
                .map(md -> md.data)  // 提取每个标记数据的核心数据
                .collect(Collectors.joining(AGGREGATE_SEPARATOR));  // 使用分隔符连接
    }

    /**
     * 将处理后的聚合结果插入到原始数据中
     * @param data 原始数据
     * @param markedDataList 标记数据列表
     * @param processedResult 处理后的聚合结果
     * @param tag 标签参数（当前未使用）
     * @param keepPrefixSuffix 是否保留前后缀
     * @return 插入聚合结果后的完整数据
     */
    private String insertAggregatedResult(String data, List<MarkedData> markedDataList, 
                                         String processedResult, String tag, boolean keepPrefixSuffix) {
        StringBuilder result = new StringBuilder(data);

        // 从后向前遍历标记数据，避免索引变化问题
        for (int i = markedDataList.size() - 1; i >= 0; i--) {
            MarkedData md = markedDataList.get(i);
            if (keepPrefixSuffix) {
                // 保留前后缀，替换为原始完整匹配
                result.replace(md.startIndex, md.endIndex, md.prefix + md.data + md.suffix);
            } else {
                // 不保留前后缀，只保留核心数据
                result.replace(md.startIndex, md.endIndex, md.data);
            }
        }

        // 查找并替换所有聚合插入位置标记
        String resultString = result.toString();
        resultString = resultString.replace(AGGREGATE_INSERT_MARKER, processedResult);

        return resultString;
    }

    /**
     * 标记位置信息内部类，用于存储在请求中找到的标记位置
     */
    private static class MarkedLocation {
        boolean isHeader;      // 是否为请求头
        String name;           // 位置名称（PATH或header名称）
        int start;             // 起始位置
        int end;               // 结束位置
        String prefix;         // 前缀
        String suffix;         // 后缀
        String data;           // 提取的数据

        MarkedLocation(boolean isHeader, String name, int start, int end, String prefix, String suffix, String data) {
            this.isHeader = isHeader;
            this.name = name;
            this.start = start;
            this.end = end;
            this.prefix = prefix;
            this.suffix = suffix;
            this.data = data;
        }
    }

    /**
     * 请求级别的聚合加密处理
     * 
     * <p>此方法用于处理HTTP请求的聚合加密，从请求的path、headers、body中提取所有标记数据，
     * 聚合后统一加密，然后将加密结果替换回所有原始标记位置。
     * 
     * <p>与字符串级别的聚合处理不同，此方法不依赖 (-AGGREGATE-) 标记，而是将加密结果
     * 替换到所有原始标记的位置。
     * 
     * @param request 待处理的HTTP请求
     * @param ende_config 加解密配置（API地址、JS文件路径或WebSocket地址）
     * @param reg_config 正则表达式，用于匹配需要聚合的数据
     * @param tag_config 标签配置（"0"=不保留前后缀，其他=保留前后缀）
     * @param mode_config 处理模式：API、JSFile 或 WebSocket
     * @return 处理后的HTTP请求
     */
    public HttpRequest processAggregateRequest(HttpRequest request, String ende_config, String reg_config, 
                                                String tag_config, String mode_config) {
        boolean keepPrefixSuffix = "1".equals(tag_config);
        
        try {
            List<MarkedLocation> markedLocations = new ArrayList<>();
            Pattern pattern = Pattern.compile(reg_config);
            
            HttpRequest tempRequest = request;
            
            // 从请求路径中提取标记数据
            String pathString = tempRequest.path();
            Matcher pathMatcher = pattern.matcher(pathString);
            while (pathMatcher.find()) {
                String fullMatch = pathMatcher.group(0);
                String extractedData = pathMatcher.group(1);
                String prefix = fullMatch.substring(0, fullMatch.indexOf(extractedData));
                String suffix = fullMatch.substring(fullMatch.indexOf(extractedData) + extractedData.length());
                
                if (!fullMatch.equals("(-AGGREGATE-)")) {
                    markedLocations.add(new MarkedLocation(false, "PATH", pathMatcher.start(), pathMatcher.end(), prefix, suffix, extractedData));
                }
            }
            
            // 从请求头中提取标记数据
            for (HttpHeader header : tempRequest.headers()) {
                String headerValue = header.value();
                Matcher matcher = pattern.matcher(headerValue);
                
                while (matcher.find()) {
                    String fullMatch = matcher.group(0);
                    String extractedData = matcher.group(1);
                    String prefix = fullMatch.substring(0, fullMatch.indexOf(extractedData));
                    String suffix = fullMatch.substring(fullMatch.indexOf(extractedData) + extractedData.length());
                    
                    if (!fullMatch.equals("(-AGGREGATE-)")) {
                        markedLocations.add(new MarkedLocation(true, header.name(), matcher.start(), matcher.end(), prefix, suffix, extractedData));
                    }
                }
            }
            
            // 从请求体中提取标记数据
            String bodyString = tempRequest.bodyToString();
            Matcher bodyMatcher = pattern.matcher(bodyString);
            while (bodyMatcher.find()) {
                String fullMatch = bodyMatcher.group(0);
                String extractedData = bodyMatcher.group(1);
                String prefix = fullMatch.substring(0, fullMatch.indexOf(extractedData));
                String suffix = fullMatch.substring(fullMatch.indexOf(extractedData) + extractedData.length());
                
                if (!fullMatch.equals("(-AGGREGATE-)")) {
                    markedLocations.add(new MarkedLocation(false, "", bodyMatcher.start(), bodyMatcher.end(), prefix, suffix, extractedData));
                }
            }
            
            // 如果没有找到标记数据，返回原始请求
            if (markedLocations.isEmpty()) {
                return request;
            }
            
            // 聚合所有标记数据
            List<String> markedDataList = markedLocations.stream()
                .map(loc -> loc.data)
                .collect(Collectors.toList());
            
            String aggregatedData = String.join("§", markedDataList);
            api.logging().logToOutput("聚合加密 - 数据: " + aggregatedData + ", 模式: " + mode_config);
            
            // 调用加密函数处理聚合数据
            String processedResult;
            try {
                if ("API".equals(mode_config)) {
                    processedResult = call_http_encryption_function(ende_config, aggregatedData, true);
                    processedResult = processedResult.substring(0, processedResult.length() - 1);
                } else if ("JSFile".equals(mode_config)) {
                    processedResult = call_js_endecryption_function(ende_config, aggregatedData, true);
                } else if ("WebSocket".equals(mode_config)) {
                    processedResult = call_websocket_endecryption_function(ende_config, aggregatedData, true);
                } else {
                    throw new IllegalArgumentException("Unknown mode: " + mode_config);
                }
            } catch (Exception e) {
                api.logging().logToError("聚合加密失败: " + e.getMessage(), e);
                return request;
            }
            
            api.logging().logToOutput("聚合加密结果: " + processedResult);

            
            // 标记点前后缀处理
            for (MarkedLocation location : markedLocations) {
                if ("PATH".equals(location.name)) {
                    String newPath;
                    if (keepPrefixSuffix) {
                        newPath = pathString.substring(0, location.start) + location.prefix + location.data + location.suffix + pathString.substring(location.end);
                    } else {
                        newPath = pathString.substring(0, location.start) + location.data + pathString.substring(location.end);
                    }
                    pathString = newPath;
                    tempRequest = tempRequest.withPath(pathString);
                } else if (location.isHeader) {
                    String headerValue = tempRequest.headerValue(location.name);
                    String newValue;
                    if (keepPrefixSuffix) {
                        newValue = headerValue.substring(0, location.start) + location.prefix + location.data + location.suffix + headerValue.substring(location.end);
                    } else {
                        newValue = headerValue.substring(0, location.start) + location.data + headerValue.substring(location.end);
                    }
                    tempRequest = tempRequest.withHeader(location.name, newValue);
                } else {
                    String newBody;
                    if (keepPrefixSuffix) {
                        newBody = bodyString.substring(0, location.start) + location.prefix + location.data + location.suffix + bodyString.substring(location.end);
                    } else {
                        newBody = bodyString.substring(0, location.start) + location.data + bodyString.substring(location.end);
                    }
                    bodyString = newBody;
                    tempRequest = tempRequest.withBody(bodyString);
                }
            }

            // 查找并替换聚合标记 (-AGGREGATE-)
            boolean foundAggregateMarker = false;

            if (pathString.contains("(-AGGREGATE-)")) {
                String newPath = pathString.replace("(-AGGREGATE-)", processedResult);
                pathString = newPath;
                tempRequest = tempRequest.withPath(pathString);
                foundAggregateMarker = true;
            }

            for (HttpHeader header : tempRequest.headers()) {
                String headerValue = header.value();
                if (headerValue.contains("(-AGGREGATE-)")) {
                    String newValue = headerValue.replace("(-AGGREGATE-)", processedResult);
                    tempRequest = tempRequest.withHeader(header.name(), newValue);
                    foundAggregateMarker = true;
                }
            }

            if (bodyString.contains("(-AGGREGATE-)")) {
                String newBody = bodyString.replace("(-AGGREGATE-)", processedResult);
                tempRequest = tempRequest.withBody(newBody);
                foundAggregateMarker = true;
            }

            if (!foundAggregateMarker) {
                api.logging().logToError("未找到聚合输出位置标记 (-AGGREGATE-)");
                return request;
            }

            return tempRequest;
        } catch (Exception e) {
            api.logging().logToError("请求聚合处理异常: " + e.getMessage(), e);
            return request;
        }
    }
}
