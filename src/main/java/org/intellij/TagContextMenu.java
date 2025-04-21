package org.intellij;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Range;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagContextMenu implements ContextMenuItemsProvider {
    MontoyaApi api;
    RootPanel panel;
    AutoUtil autoUtil;

    public TagContextMenu(MontoyaApi montoyaApi, RootPanel rootPanel) {
        api = montoyaApi;
        panel = rootPanel;
        autoUtil = new AutoUtil(montoyaApi);
    }

    public List<Component> provideMenuItems(ContextMenuEvent event) {
        // 创建右键菜单列表
        if (event.isFromTool(ToolType.REPEATER) || event.isFromTool(ToolType.PROXY))
        {
            List<Component> menuItemList = new ArrayList<>();
            JMenuItem addTagItem = new JMenuItem("AddTag");
            JMenuItem sendTargetItem = new JMenuItem("SendTarget");
            JMenuItem encodeTtem = new JMenuItem("Encode");
            JMenuItem decodeTtem = new JMenuItem("Decode");
            MessageEditorHttpRequestResponse messageEditorHttpRequestResponse = event.messageEditorRequestResponse().get();

            sendTargetItem.addActionListener(l -> {
//                添加目标
                String host = messageEditorHttpRequestResponse.requestResponse().request().headerValue("Host");
                String path = messageEditorHttpRequestResponse.requestResponse().request().pathWithoutQuery();
                panel.addList(host, path);
            });
            menuItemList.add(sendTargetItem);

            if (!messageEditorHttpRequestResponse.selectionOffsets().isEmpty()) {
                addTagItem.addActionListener(l -> {

                    Range range = messageEditorHttpRequestResponse.selectionOffsets().get();
                    HttpRequest httpRequest = messageEditorHttpRequestResponse.requestResponse().request();
                    ByteArray selectByteArray = httpRequest.toByteArray().subArray(range);
                    ByteArray startByteArray = httpRequest.toByteArray().subArray(0, range.startIndexInclusive());

                    byte[] bytes = Arrays.copyOfRange(httpRequest.toByteArray().getBytes(), range.endIndexExclusive(), httpRequest.toByteArray().length());
                    ByteArray endByteArray = ByteArray.byteArray(bytes);
                    ByteArray newByteArray = startByteArray;
                    String newSelcet = "(-" + selectByteArray.toString() + "-)";

                    newByteArray = newByteArray.withAppended(newSelcet);
                    newByteArray = newByteArray.withAppended(endByteArray);

                    try {
                        messageEditorHttpRequestResponse.setRequest(HttpRequest.httpRequest(newByteArray));
                    }catch (Exception e) {
                        if (!e.getMessage().contains("Item is read-only")){
                            api.logging().logToError("Error in TagContextMenu",e);
                        }
                    }
                });
                menuItemList.add(addTagItem);
            }

            if (!messageEditorHttpRequestResponse.selectionOffsets().isEmpty()) {
                encodeTtem.addActionListener(l -> {


                    Range range = messageEditorHttpRequestResponse.selectionOffsets().get();
                    if (messageEditorHttpRequestResponse.selectionContext().name()=="REQUEST") {
                        HttpRequest httpRequest = messageEditorHttpRequestResponse.requestResponse().request();
                        ByteArray selectByteArray = httpRequest.toByteArray().subArray(range);
                        ByteArray startByteArray = httpRequest.toByteArray().subArray(0, range.startIndexInclusive());

                        byte[] bytes = Arrays.copyOfRange(httpRequest.toByteArray().getBytes(), range.endIndexExclusive(), httpRequest.toByteArray().length());
                        ByteArray endByteArray = ByteArray.byteArray(bytes);
                        ByteArray newByteArray = startByteArray;
                        String newSelect = selectByteArray.toString();
                        api.logging().logToOutput("original:\n" + newSelect);
                        String host = event.messageEditorRequestResponse().get().requestResponse().request().headerValue("Host");
                        String path = event.messageEditorRequestResponse().get().requestResponse().request().path();

                        String[] config = panel.findConf(host, path);

                        if (config != null) {
                            String mode_config = config[5];
                            String ende_config = config[6];
                            try {
                                if (ende_config == "") {
                                    throw new Exception();
                                }
                                newSelect = autoUtil.matchFunc(newSelect, ende_config, mode_config, true);
                                api.logging().logToOutput("encrypt:\n" + newSelect+"\n");
                            } catch (Exception e) {
                                newSelect = selectByteArray.toString();
                            }
                        }

                        newByteArray = newByteArray.withAppended(newSelect);
                        newByteArray = newByteArray.withAppended(endByteArray);

                        try {
                            messageEditorHttpRequestResponse.setRequest(HttpRequest.httpRequest(newByteArray));
                        } catch (Exception e) {
                            if (!e.getMessage().contains("Item is read-only")) {
                                api.logging().logToError("Error in TagContextMenu", e);
                            }
                        }
                    }
                    else {
                        HttpResponse httpResponse = messageEditorHttpRequestResponse.requestResponse().response();
                        ByteArray selectByteArray = httpResponse.toByteArray().subArray(range);
                        ByteArray startByteArray = httpResponse.toByteArray().subArray(0, range.startIndexInclusive());

                        byte[] bytes = Arrays.copyOfRange(httpResponse.toByteArray().getBytes(), range.endIndexExclusive(), httpResponse.toByteArray().length());
                        ByteArray endByteArray = ByteArray.byteArray(bytes);
                        ByteArray newByteArray = startByteArray;
                        String newSelect = selectByteArray.toString();
                        api.logging().logToOutput("original:\n" + newSelect);
                        String host = event.messageEditorRequestResponse().get().requestResponse().request().headerValue("Host");
                        String path = event.messageEditorRequestResponse().get().requestResponse().request().path();

                        String[] config = panel.findConf(host, path);

                        if (config != null) {
                            String mode_config = config[5];
                            String ende_config = config[6];
                            try {
                                if (ende_config == "") {
                                    throw new Exception();
                                }
                                newSelect = autoUtil.matchFunc(newSelect, ende_config, mode_config, true);
                                api.logging().logToOutput("encrypt:\n" + newSelect+"\n");
                            } catch (Exception e) {
                                newSelect = selectByteArray.toString();
                            }
                        }

                        newByteArray = newByteArray.withAppended(newSelect);
                        newByteArray = newByteArray.withAppended(endByteArray);

                        try {
                            messageEditorHttpRequestResponse.setResponse(HttpResponse.httpResponse(newByteArray));
                        } catch (Exception e) {
                            if (!e.getMessage().contains("Item is read-only")) {
                                api.logging().logToError("Error in TagContextMenu", e);
                            }
                        }
                    }
                });
                menuItemList.add(encodeTtem);
            }
            if (!messageEditorHttpRequestResponse.selectionOffsets().isEmpty()) {
                decodeTtem.addActionListener(l -> {

                    Range range = messageEditorHttpRequestResponse.selectionOffsets().get();
                    if (messageEditorHttpRequestResponse.selectionContext().name()=="REQUEST") {
                        HttpRequest httpRequest = messageEditorHttpRequestResponse.requestResponse().request();
                        ByteArray selectByteArray = httpRequest.toByteArray().subArray(range);
                        ByteArray startByteArray = httpRequest.toByteArray().subArray(0, range.startIndexInclusive());
                        byte[] bytes = Arrays.copyOfRange(httpRequest.toByteArray().getBytes(), range.endIndexExclusive(), httpRequest.toByteArray().length());
                        ByteArray endByteArray = ByteArray.byteArray(bytes);
                        ByteArray newByteArray = startByteArray;
                        String newSelect = selectByteArray.toString();
                        api.logging().logToOutput("original:\n" +newSelect);
                        String host = event.messageEditorRequestResponse().get().requestResponse().request().headerValue("Host");
                        String path = event.messageEditorRequestResponse().get().requestResponse().request().path();

                        String[] config = panel.findConf(host, path);

                        if (config != null) {
                            String mode_config = config[5];
                            String ende_config = config[6];
                            try {
                                if (ende_config == "") {
                                    throw new Exception();
                                }
                                newSelect = autoUtil.matchFunc(newSelect, ende_config, mode_config, false, true);
                                api.logging().logToOutput("decrypt:\n" + newSelect+"\n");
                            } catch (Exception e) {
                                newSelect = selectByteArray.toString();
                            }
                        }

                        newByteArray = newByteArray.withAppended(newSelect);
                        newByteArray = newByteArray.withAppended(endByteArray);

                        try {
                            messageEditorHttpRequestResponse.setRequest(HttpRequest.httpRequest(newByteArray));
                        } catch (Exception e) {
                            if (!e.getMessage().contains("Item is read-only")) {
                                api.logging().logToError("Error in TagContextMenu", e);
                            }
                        }
                    }
                    else {
                        HttpResponse httpResponse = messageEditorHttpRequestResponse.requestResponse().response();
                        ByteArray selectByteArray = httpResponse.toByteArray().subArray(range);
                        ByteArray startByteArray = httpResponse.toByteArray().subArray(0, range.startIndexInclusive());

                        byte[] bytes = Arrays.copyOfRange(httpResponse.toByteArray().getBytes(), range.endIndexExclusive(), httpResponse.toByteArray().length());
                        ByteArray endByteArray = ByteArray.byteArray(bytes);
                        ByteArray newByteArray = startByteArray;
                        String newSelect = selectByteArray.toString();
                        api.logging().logToOutput("original:\n" +newSelect);
                        String host = event.messageEditorRequestResponse().get().requestResponse().request().headerValue("Host");
                        String path = event.messageEditorRequestResponse().get().requestResponse().request().path();

                        String[] config = panel.findConf(host, path);

                        if (config != null) {
                            String mode_config = config[5];
                            String ende_config = config[6];
                            try {
                                if (ende_config == "") {
                                    throw new Exception();
                                }
                                newSelect = autoUtil.matchFunc(newSelect, ende_config, mode_config, false, true);
                                api.logging().logToOutput("decrypt:\n" + newSelect+"\n");
                            } catch (Exception e) {
                                newSelect = selectByteArray.toString();
                            }
                        }
                        newByteArray = newByteArray.withAppended(newSelect);
                        newByteArray = newByteArray.withAppended(endByteArray);

                        try {
                            messageEditorHttpRequestResponse.setResponse(HttpResponse.httpResponse(newByteArray));
                        } catch (Exception e) {
                            if (!e.getMessage().contains("Item is read-only")) {
                                api.logging().logToError("Error in TagContextMenu", e);
                            }
                        }
                    }
                });
                menuItemList.add(decodeTtem);
            }
            return menuItemList;
        }
        return null;
    }
}