package org.intellij;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.core.Range;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.requests.HttpRequest;
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

    public TagContextMenu(MontoyaApi montoyaApi) {
        api = montoyaApi;
    }
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        // 创建右键菜单列表
        if (event.isFromTool(ToolType.REPEATER))
        {
            List<Component> menuItemList = new ArrayList<>();

            JMenuItem retrieveRequestItem = new JMenuItem("AddTag");

            MessageEditorHttpRequestResponse messageEditorHttpRequestResponse = event.messageEditorRequestResponse().get();
            retrieveRequestItem.addActionListener(l -> {
                 Range range = messageEditorHttpRequestResponse.selectionOffsets().get();
                 HttpRequest httpRequest = messageEditorHttpRequestResponse.requestResponse().request();
                 ByteArray selectByteArray = httpRequest.toByteArray().subArray(range);
//                 api.logging().logToOutput("select:"+selectByteArray.toString());
                 ByteArray startByteArray = httpRequest.toByteArray().subArray(0,range.startIndexInclusive());
//                 api.logging().logToOutput("start:"+startByteArray.toString());

                 byte[] bytes = Arrays.copyOfRange(httpRequest.toByteArray().getBytes(),range.endIndexExclusive(),httpRequest.toByteArray().length());
                 ByteArray endByteArray = ByteArray.byteArray(bytes);
//                 api.logging().logToOutput("end:"+endByteArray.toString());

                 ByteArray newByteArray = startByteArray;
                 String newSelcet = "{-"+selectByteArray.toString()+"-}";
                 newByteArray = newByteArray.withAppended(newSelcet);
                 newByteArray = newByteArray.withAppended(endByteArray);

                 messageEditorHttpRequestResponse.setRequest(HttpRequest.httpRequest(newByteArray));
            });

            menuItemList.add(retrieveRequestItem);
            return menuItemList;
        }

        return null;
    }
}
