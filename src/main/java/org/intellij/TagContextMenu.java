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
    RootPanel panel;

    public TagContextMenu(MontoyaApi montoyaApi, RootPanel rootPanel) {
        api = montoyaApi;
        panel = rootPanel;
    }
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        // 创建右键菜单列表
        if (event.isFromTool(ToolType.REPEATER))
        {
            List<Component> menuItemList = new ArrayList<>();

            JMenuItem addTagItem = new JMenuItem("AddTag");
            JMenuItem sendTargetItem = new JMenuItem("SendTarget");
            MessageEditorHttpRequestResponse messageEditorHttpRequestResponse = event.messageEditorRequestResponse().get();

            sendTargetItem.addActionListener(l -> {
//                自动添加目标
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

                    messageEditorHttpRequestResponse.setRequest(HttpRequest.httpRequest(newByteArray));
                });
                menuItemList.add(addTagItem);
            }

            return menuItemList;
        }

        return null;
    }
}
