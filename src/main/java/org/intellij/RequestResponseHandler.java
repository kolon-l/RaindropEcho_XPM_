package org.intellij;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.utilities.Utilities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestResponseHandler implements HttpHandler {

    private UsuallyJS jsHandler;
    private MontoyaApi montoyaApi;
    private RootPanel rootPanel;

    public RequestResponseHandler(MontoyaApi api, RootPanel panel, UsuallyJS usuallyJS) {
        montoyaApi = api;
        rootPanel = panel;
        jsHandler = usuallyJS;
    }

    private String matchFunc(String string,String file_js){
        StringBuffer result = new StringBuffer() ;
        Pattern pattern = Pattern.compile(rootPanel.getRegex());
        int tag = rootPanel.getTag();
        Matcher matche = pattern.matcher(string);
        while (matche.find()) {
            if (tag==1){
                String encrypt= null;
                try {
                    encrypt = jsHandler.call_js_endecryption_function(file_js,matche.group(1),true);
                }catch (Exception e) {
                    throw new RuntimeException(e);
                }
                matche.appendReplacement(result, encrypt);
            }
            else{
                String encrypt= null;
                try {
                    encrypt = jsHandler.call_js_endecryption_function(file_js,matche.group(1),true);
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

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent httpRequestToBeSent) {
        if (httpRequestToBeSent.toolSource().toolType().toolName().equals("Repeater") || httpRequestToBeSent.toolSource().toolType().toolName().equals("Intruder")) {
            String domain = httpRequestToBeSent.httpService().host();
            String path = httpRequestToBeSent.path();
            HttpRequest newRequest = httpRequestToBeSent;
            Utilities utilities = montoyaApi.utilities();
            for (int i = 0; i < rootPanel.getModel().getSize(); i++) {
                String domain_js = utilities.jsonUtils().readString(rootPanel.getModel().getElementAt(i).toString(), ("domain"));
                String path_js = utilities.jsonUtils().readString(rootPanel.getModel().getElementAt(i).toString(), ("path"));
                String file_js = utilities.jsonUtils().readString(rootPanel.getModel().getElementAt(i).toString(), ("file"));
                if (domain.equals(domain_js) && path.startsWith(path_js)) {
                    try {

//                        提取urlpath
                        newRequest = newRequest.withPath(matchFunc(newRequest.path(),file_js));
//                        提取header，匹配并替换加密结果
                        List<HttpHeader> headerList = newRequest.headers();
                        for (HttpHeader header : headerList) {
                            newRequest = newRequest.withHeader(header.name(),
                                    matchFunc(header.name()+": "+header.value(),file_js).replace(header.name()+": ",""));
                        }
//                    提取body,匹配并替换加密结果
//                        int bodyOffset = newRequest.bodyOffset();
//                        int bodySize = newRequest.body().length();
//                        ByteArray bodyArray = newRequest.toByteArray().subArray(bodyOffset, bodyOffset + bodySize);
                        String bodyString = newRequest.bodyToString();
                        newRequest = newRequest.withBody(matchFunc(bodyString,file_js));
                    } catch (Exception e) {
                        montoyaApi.logging().logToError(e.getMessage());
                    }
                }
            }
            return RequestToBeSentAction.continueWith(newRequest);
        }
        return null;
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived httpResponseReceived) {
        if (httpResponseReceived.toolSource().toolType().toolName().equals("Repeater") || httpResponseReceived.toolSource().toolType().toolName().equals("Intruder")) {
            String domain = httpResponseReceived.initiatingRequest().httpService().host();
            String path = httpResponseReceived.initiatingRequest().path();

            Utilities utilities = montoyaApi.utilities();
            for (int i = 0; i < rootPanel.getModel().getSize(); i++) {
                String domain_js = utilities.jsonUtils().readString(rootPanel.getModel().getElementAt(i).toString(), ("domain"));
                String path_js = utilities.jsonUtils().readString(rootPanel.getModel().getElementAt(i).toString(), ("path"));
                String file_js = utilities.jsonUtils().readString(rootPanel.getModel().getElementAt(i).toString(), ("file"));
                if (domain.equals(domain_js) && path.startsWith(path_js)) {
                    try {
//                    提取body,匹配并替换加密结果
//                        int bodyOffset = httpResponseReceived.bodyOffset();
//                        int bodySize = httpResponseReceived.body().length();
//                        ByteArray bodyArray = httpResponseReceived.toByteArray().subArray(bodyOffset, bodyOffset + bodySize);
                        String bodyString = httpResponseReceived.bodyToString();
                        String decrypt = jsHandler.call_js_endecryption_function(file_js, bodyString, false);
                        HttpResponse newResponse = httpResponseReceived.withBody(ByteArray.byteArray(decrypt.getBytes(StandardCharsets.UTF_8)));

                        montoyaApi.logging().logToOutput(httpResponseReceived.initiatingRequest().toByteArray().toString());
                        montoyaApi.logging().logToOutput(bodyString);
                        return ResponseReceivedAction.continueWith(newResponse);
                    } catch (Exception e) {
                        montoyaApi.logging().logToOutput(httpResponseReceived.initiatingRequest().toByteArray().toString());
                        montoyaApi.logging().logToOutput(httpResponseReceived.bodyToString());
                        montoyaApi.logging().logToError(e.getMessage());
                    }
                    return ResponseReceivedAction.continueWith(httpResponseReceived);
                }
            }
        }
        return null;
    }
}
