package org.intellij;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RequestResponseHandler - HTTP请求和响应处理器
 * 
 * <p>实现Burp Suite的HttpHandler接口，拦截和处理HTTP请求和响应，
 * 对匹配配置的数据进行自动加解密处理。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>拦截Repeater和Intruder工具的HTTP请求</li>
 *   <li>根据配置对请求的路径、请求头和请求体进行加密处理</li>
 *   <li>支持单字段加密和多字段聚合加密</li>
 *   <li>拦截HTTP响应并对响应体进行解密处理</li>
 *   <li>记录处理过程和错误信息</li>
 * </ul>
 * 
 * @author RaindropEcho
 * @version 1.0
 */
public class RequestResponseHandler implements HttpHandler {

    private final AutoUtil autoUtil;
    private final MontoyaApi montoyaApi;
    private final RootPanel rootPanel;

    /**
     * 构造函数
     * 
     * @param api Burp Suite Montoya API实例
     * @param panel RootPanel实例，用于获取配置数据
     * @param util AutoUtil实例，用于执行加解密操作
     */
    public RequestResponseHandler(MontoyaApi api, RootPanel panel, AutoUtil util) {
        montoyaApi = api;
        rootPanel = panel;
        autoUtil = util;
    }

    /**
     * 处理即将发送的HTTP请求
     * 
     * <p>仅处理来自Repeater和Intruder工具的请求。
     * 根据配置对请求进行加密处理：
     * <ul>
     *   <li>如果启用参数聚合，对整个请求进行聚合加密</li>
     *   <li>否则，分别对路径、请求头和请求体进行加密</li>
     * </ul>
     * 
     * @param httpRequestToBeSent 即将发送的HTTP请求
     * @return 处理后的请求操作，如果工具不是Repeater或Intruder则返回null
     */
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {
        String toolName = httpRequestToBeSent.toolSource().toolType().toolName();
        if (!"Repeater".equals(toolName) && !"Intruder".equals(toolName)) {
            return null;
        }

        String host = httpRequestToBeSent.headerValue("Host");
        String path = httpRequestToBeSent.path();
        HttpRequest newRequest = httpRequestToBeSent;
        ArrayList<String[]> target_config = rootPanel.findConf(host, path);

        if (!target_config.isEmpty()) {
            for (String[] config : target_config) {
                String reg_config = config[ConfigIndex.JS_LIST_REGEX];
                String ende_config = config[ConfigIndex.JS_LIST_ENDE_CONFIG];
                String mode_config = config[ConfigIndex.JS_LIST_MODE];
                String tag_config = config[ConfigIndex.JS_LIST_KEEP_PREFIX_SUFFIX];
                String aggregate_config = config[ConfigIndex.JS_LIST_AGGREGATE];

                if (ende_config.isEmpty()) {
                    continue;
                }

                try {
                    if ("1".equals(aggregate_config)) {
                        newRequest = autoUtil.processAggregateRequest(newRequest, ende_config, reg_config, tag_config, mode_config);
                    } else {
                        newRequest = newRequest.withPath(autoUtil.matchFunc(newRequest.path(), ende_config, reg_config, tag_config, mode_config, true));
                        
                        List<HttpHeader> headerList = newRequest.headers();
                        for (HttpHeader header : headerList) {
                            newRequest = newRequest.withHeader(header.name(),
                                    autoUtil.matchFunc(header.name() + ": " + header.value(), ende_config, reg_config, tag_config, mode_config, true)
                                            .replace(header.name() + ": ", ""));
                        }
                        
                        String bodyString = newRequest.bodyToString();
                        newRequest = newRequest.withBody(autoUtil.matchFunc(bodyString, ende_config, reg_config, tag_config, mode_config, true));
                    }
                } catch (Exception e) {
                    montoyaApi.logging().logToError("请求处理异常: " + e.getMessage(), e);
                }
            }
            montoyaApi.logging().logToOutput(newRequest.toByteArray().toString());
        }

        return RequestToBeSentAction.continueWith(newRequest);
    }

    /**
     * 处理接收到的HTTP响应
     * 
     * <p>仅处理来自Repeater和Intruder工具的响应。
     * 根据配置对响应体进行解密处理：
     * <ul>
     *   <li>只处理配置中启用了"处理响应体"的配置</li>
     *   <li>如果启用参数聚合，对整个响应体进行聚合解密</li>
     *   <li>否则，对响应体进行常规解密</li>
     * </ul>
     * 
     * @param httpResponseReceived 接收到的HTTP响应
     * @return 处理后的响应操作，如果工具不是Repeater或Intruder则返回null
     */
    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
        String toolName = httpResponseReceived.toolSource().toolType().toolName();
        if (!"Repeater".equals(toolName) && !"Intruder".equals(toolName)) {
            return ResponseReceivedAction.continueWith(httpResponseReceived);
        }

        String host = httpResponseReceived.initiatingRequest().headerValue("Host");
        String path = httpResponseReceived.initiatingRequest().path();
        HttpResponse newResponse = httpResponseReceived;
        ArrayList<String[]> target_config = rootPanel.findConf(host, path);

        if (!target_config.isEmpty()) {
            for (String[] config : target_config) {
                String reg_config = config[ConfigIndex.JS_LIST_REGEX];
                String ende_config = config[ConfigIndex.JS_LIST_ENDE_CONFIG];
                String mode_config = config[ConfigIndex.JS_LIST_MODE];
                String tag_config = config[ConfigIndex.JS_LIST_KEEP_PREFIX_SUFFIX];
                String processResponse = config[ConfigIndex.JS_LIST_PROCESS_RESPONSE];

                if (ende_config.isEmpty() || "0".equals(processResponse)) {
                    continue;
                }

                try {
                    String bodyString = newResponse.bodyToString();
                    String result = autoUtil.matchFunc(bodyString, ende_config, reg_config, tag_config, mode_config, false);
                    newResponse = newResponse.withBody(result);
                } catch (Exception e) {
                    montoyaApi.logging().logToError("响应解密失败 - Host: " + host + ", Path: " + path + ", 错误: " + e.getMessage(), e);
                }
            }
            montoyaApi.logging().logToOutput(newResponse.bodyToString()+"\n");
        }

        return ResponseReceivedAction.continueWith(newResponse);
    }

}