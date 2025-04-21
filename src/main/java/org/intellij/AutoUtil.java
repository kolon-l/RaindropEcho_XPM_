package org.intellij;
import burp.api.montoya.MontoyaApi;
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


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class AutoUtil {

    private final MontoyaApi api;
    AutoUtil(MontoyaApi montoyaApi) {
        api = montoyaApi;
    }
    public String matchFunc(String data,String ende_config,String mode_config, boolean isencrypt){
        return matchFunc(data,ende_config,mode_config,isencrypt,false);
    }


    public String matchFunc(String data,String ende_config,String mode_config, boolean isencrypt, boolean isFromRightButton){
        String result = data;
        if(isencrypt){
            try {
                if (mode_config=="API") {
                    result = call_http_encryption_function(ende_config,data,true);
                    result = result.substring(0,result.length()-1);
                }
                else if(mode_config == "JSFile") result = call_js_endecryption_function(ende_config,data,true);
                else if (mode_config=="WebSocket") result = call_websocket_endecryption_function(ende_config,data,true);
            }catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else {
            if (isFromRightButton){
                try {
                    if (mode_config=="API") {
                        result = call_http_encryption_function(ende_config,data,false,isFromRightButton);
                        result = result.substring(0,result.length()-1);
                    }
                    else if (mode_config=="JSFile") result = call_js_endecryption_function(ende_config,data,false,isFromRightButton);
                    else if (mode_config=="WebSocket") result = call_websocket_endecryption_function(ende_config,data,false,isFromRightButton);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    if (mode_config=="API") {
                        result = call_http_encryption_function(ende_config,data,false);
                        result = result.substring(0,result.length()-1);
                    }
                    else if (mode_config=="JSFile") result = call_js_endecryption_function(ende_config,data,false);
                    else if (mode_config=="WebSocket") result = call_websocket_endecryption_function(ende_config,data,false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return result;
    }


    public String matchFunc(String data,String ende_config,String reg,String tag,String mode_config, boolean isencrypt){
        Pattern pattern = Pattern.compile(reg);
        Matcher matche = pattern.matcher(data);

        if(isencrypt){
            StringBuffer result = new StringBuffer() ;
            while (matche.find()) {
                if (tag=="0"){
                    String encrypt= null;
                    String g1=matche.group(1);
                    try {
                        if (mode_config=="API") {
                            encrypt = call_http_encryption_function(ende_config,matche.group(1),true);
                            encrypt = encrypt.substring(0,encrypt.length()-1);
                        }
                        else if(mode_config == "JSFile") encrypt = call_js_endecryption_function(ende_config,matche.group(1),true);
                        else encrypt = call_websocket_endecryption_function(ende_config,matche.group(1),true);
                    }catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    matche.appendReplacement(result, encrypt);
                }
                else{
                    String encrypt= null;
                    try {
                        if (mode_config=="API") {
                            encrypt = call_http_encryption_function(ende_config,matche.group(1),true);
                            encrypt = encrypt.substring(0,encrypt.length()-1);
                        }
                        else if(mode_config == "JSFile") encrypt = call_js_endecryption_function(ende_config,matche.group(1),true);
                        else encrypt = call_websocket_endecryption_function(ende_config,matche.group(1),true);
                        String S1 = matche.group(1);
                        String S2 = matche.group(0);
                        matche.appendReplacement(result, (S2+S1).split(S1)[0]+encrypt+(S2+S1).split(S1)[1]);
                    }catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            matche.appendTail(result);
            return result.toString();
        }
        else{

            String result="body处理失败";
            try {
                if (mode_config=="API") {
                    result = call_http_encryption_function(ende_config,data,false);
                    result = result.substring(0,result.length()-1);
                }
                else if (mode_config=="JSFile") result = call_js_endecryption_function(ende_config,data,false);
                else result = call_websocket_endecryption_function(ende_config,data,false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return result;
        }
    }


    public String call_js_endecryption_function(String jsFile, String data,Boolean isencrypt) throws Exception {
        return call_js_endecryption_function(jsFile,data,isencrypt,false);
    }

    public  String call_js_endecryption_function(String jsFile, String data,Boolean isencrypt,Boolean isFromRightButton) throws Exception {

        // 构建执行 Node.js 脚本的命令
        List<String> command = new ArrayList<>();
        command.add("node");
        command.add(jsFile);
        if(isencrypt){command.add("encrypt_c");}
        else{
            if(isFromRightButton){
                command.add("decrypt_c_RB");
            }
            else {
                command.add("decrypt_c");
            }
        }
        command.add("\""+data.replace("\\","\\\\").replace("\"","\\\"")+"\"");
        // 执行 Node.js 脚本
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
        outData=stdout;

        return outData;
    }

    public  String sendPost(String targetUrl, String data) throws IOException {

        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        // 发送请求体
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = data.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 获取响应
        int status = conn.getResponseCode();
        InputStream inputStream;
        if (status >= 200 && status < 300) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();
        }

        // 读取响应内容
        String response = inputStreamToString(inputStream);
        conn.disconnect(); // 断开连接
        return response;
    }

    private  String inputStreamToString(InputStream inputStream) throws IOException {
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
        // 移除最后一个多余的换行符
        if (response.length() > 0) {
            response.setLength(response.length() - 1);
        }
        return response.toString();
    }

    public String call_http_encryption_function(String url, String data,Boolean isencrypt) throws Exception {
        return call_http_encryption_function(url,data,isencrypt,false);
    }
    public String call_http_encryption_function(String url, String data,Boolean isencrypt,Boolean isFromRightButton) throws Exception {
        String outData = "";
        try{
            if(isencrypt){outData=sendPost(url+"/encrypt",data);}
            else {
                if(isFromRightButton){
                    outData=sendPost(url+"/decrypt_RB",data);
                }
                else {
                    outData=sendPost(url+"/decrypt",data);
                }
            }
        }
        catch (Exception e){
            api.logging().logToError("call http function :"+url+". Error: "+e.toString());
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
            api.logging().logToOutput("Websocket Server:" + addr.toString() + " start");
            wsServers.add(wsServer);
            return true;
        }
        catch (Exception e) {
            api.logging().logToError("Add Websocket Server: "+ target +". Error:"+e.toString());
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
                    api.logging().logToOutput("Websocket Server: " + addr.toString() + " stop");

                    listWSServers();

                    return true;
                }
            }
            throw new Exception("Server not found");
        }
        catch (Exception e) {
            api.logging().logToError("Delete Websocket Server: "+target+". Error: "+e.toString());
            return false;
        }
    }

    public boolean checkWSServer(String target)  {
        try {
            WebSocketURLParser.WebSocketURLInfo url =WebSocketURLParser.parseWebSocketURL(target);
            InetSocketAddress addr = new InetSocketAddress(url.getHost(), url.getPort());
            api.logging().logToError("Websocket Server:" + addr.getHostString() );
            WSServer wsServer = new WSServer(addr, api);
            for (WSServer server : wsServers) {
                if (server.getAddress().getHostString().equals(wsServer.getAddress().getHostString())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            api.logging().logToError("Check Websocket Server: "+ target +". Error:"+e.toString());
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

            api.logging().logToOutput("Websocket Server: " + wsServer.getAddress().getHostString());
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
                            api.logging().logToError("get websocket response error: " + e.getMessage());
                        }
                    }
                }
            }
        }
        return output;
    }
}
