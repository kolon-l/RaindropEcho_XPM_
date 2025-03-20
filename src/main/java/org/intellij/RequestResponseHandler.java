package org.intellij;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestResponseHandler implements HttpHandler {

    private AutoUtil autoUtil;
    private MontoyaApi montoyaApi;
    private RootPanel rootPanel;

    public RequestResponseHandler(MontoyaApi api, RootPanel panel, AutoUtil util) {
        montoyaApi = api;
        rootPanel = panel;
        autoUtil = util;
    }

    private String matchFunc(String data,String ende_config,String reg,String tag,String mode_config, boolean isencrypt){
        Pattern pattern = Pattern.compile(reg);
        Matcher matche = pattern.matcher(data);

        if(isencrypt){
            StringBuffer result = new StringBuffer() ;
            while (matche.find()) {
                if (tag=="0"){
                    String encrypt= null;
                    String g1=matche.group(1);
                    try {
                        if (mode_config=="API") encrypt = autoUtil.call_http_encryption_function(ende_config,matche.group(1),true);
                        else if(mode_config == "JSFile") encrypt = autoUtil.call_js_endecryption_function(ende_config,matche.group(1),true);
                        else encrypt = autoUtil.call_websocket_endecryption_function(ende_config,matche.group(1),true);
                    }catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    matche.appendReplacement(result, encrypt);
                }
                else{
                    String encrypt= null;
                    try {
//                        encrypt = autoUtil.call_js_endecryption_function(ende_config,matche.group(1),true);
                        if (mode_config=="API") encrypt = autoUtil.call_http_encryption_function(ende_config,matche.group(1),true);
                        else if(mode_config == "JSFile") encrypt = autoUtil.call_js_endecryption_function(ende_config,matche.group(1),true);
                        else encrypt = autoUtil.call_websocket_endecryption_function(ende_config,matche.group(1),true);
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
                if (mode_config=="API") result = autoUtil.call_http_encryption_function(ende_config,data,false);
                else if (mode_config=="JSfile") result = autoUtil.call_js_endecryption_function(ende_config,data,false);
                else result = autoUtil.call_websocket_endecryption_function(ende_config,data,false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return result;
        }
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {
        if (httpRequestToBeSent.toolSource().toolType().toolName().equals("Repeater") || httpRequestToBeSent.toolSource().toolType().toolName().equals("Intruder")) {
            String host = httpRequestToBeSent.headerValue("Host");
            String path = httpRequestToBeSent.path();
            HttpRequest newRequest = httpRequestToBeSent;
            String[] config = rootPanel.findConf(host, path);

            if (config != null) {
                String reg_config = config[2];
                String tag_config = config[3];
                String mode_config = config[4];
                String ende_config = config[5];
                if(ende_config==""){return null;}
                try {
//                        提取urlpath
                    newRequest = newRequest.withPath(matchFunc(newRequest.path(),ende_config,reg_config,tag_config,mode_config,true));
//                        提取header，匹配并替换加密结果
                    List<HttpHeader> headerList = newRequest.headers();
                    for (HttpHeader header : headerList) {
                        newRequest = newRequest.withHeader(header.name(),
                                matchFunc(header.name()+": "+header.value(),ende_config,reg_config,tag_config,mode_config,true).replace(header.name()+": ",""));
                    }
                    String bodyString = newRequest.bodyToString();
                    newRequest = newRequest.withBody(matchFunc(bodyString,ende_config,reg_config,tag_config,mode_config,true));
                } catch (Exception e) {
                    montoyaApi.logging().logToError(e.getMessage());
                }
            }
            return RequestToBeSentAction.continueWith(newRequest);
        }
        return null;
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
        if (httpResponseReceived.toolSource().toolType().toolName().equals("Repeater") || httpResponseReceived.toolSource().toolType().toolName().equals("Intruder")) {
            String host = httpResponseReceived.initiatingRequest().headerValue("Host");
            String path = httpResponseReceived.initiatingRequest().path();
            String[] config = rootPanel.findConf(host, path);

            if (config != null) {
                String reg_config = config[2];
                String tag_config = config[3];
                String mode_config = config[4];
                String ende_config = config[5];
                if(ende_config==""){return null;}
                try {
                    String bodyString = httpResponseReceived.bodyToString();
                    String result = matchFunc(bodyString,ende_config,reg_config,tag_config,mode_config,false);
                    HttpResponse newResponse = httpResponseReceived.withBody(result);

                    montoyaApi.logging().logToOutput(httpResponseReceived.initiatingRequest().toByteArray().toString()+"\n"+newResponse.bodyToString());
                    return ResponseReceivedAction.continueWith(newResponse);
                }
                catch (Exception e) {
                    montoyaApi.logging().logToOutput(httpResponseReceived.initiatingRequest().toByteArray().toString()+"\n"+httpResponseReceived.bodyToString());
                    montoyaApi.logging().logToError("响应体解密异常,是否为空: "+e.getMessage());
                }
            }
        }
        return null;
    }
}