package org.intellij;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.util.List;

public class RequestResponseHandler implements HttpHandler {

    private AutoUtil autoUtil;
    private MontoyaApi montoyaApi;
    private RootPanel rootPanel;

    public RequestResponseHandler(MontoyaApi api, RootPanel panel, AutoUtil util) {
        montoyaApi = api;
        rootPanel = panel;
        autoUtil = new AutoUtil(api);
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
                    newRequest = newRequest.withPath(autoUtil.matchFunc(newRequest.path(),ende_config,reg_config,tag_config,mode_config,true));
//                        提取header，匹配并替换加密结果
                    List<HttpHeader> headerList = newRequest.headers();
                    for (HttpHeader header : headerList) {
                        newRequest = newRequest.withHeader(header.name(),
                                autoUtil.matchFunc(header.name()+": "+header.value(),ende_config,reg_config,tag_config,mode_config,true).replace(header.name()+": ",""));
                    }
                    String bodyString = newRequest.bodyToString();
                    newRequest = newRequest.withBody(autoUtil.matchFunc(bodyString,ende_config,reg_config,tag_config,mode_config,true));
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
                    String result = autoUtil.matchFunc(bodyString,ende_config,reg_config,tag_config,mode_config,false);
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