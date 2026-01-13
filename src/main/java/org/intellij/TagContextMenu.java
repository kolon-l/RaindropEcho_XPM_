package org.intellij;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.core.Range;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import burp.api.montoya.core.ByteArray;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagContextMenu implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final RootPanel panel;
    private final AutoUtil autoUtil;

    public TagContextMenu(MontoyaApi montoyaApi, RootPanel rootPanel, AutoUtil util) {
        api = montoyaApi;
        panel = rootPanel;
        autoUtil = util;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        if (event.isFromTool(ToolType.REPEATER) || event.isFromTool(ToolType.PROXY)) {
            return createContextMenuItems(event);
        }
        return null;
    }

    private List<Component> createContextMenuItems(ContextMenuEvent event) {
        List<Component> menuItemList = new ArrayList<>();
        JMenuItem addTagItem = new JMenuItem("添加标记");
        JMenuItem addMutiTagItem = new JMenuItem("标记输出位置");
        JMenuItem sendTargetItem = new JMenuItem("添加配置");
        JMenuItem encodeItem = new JMenuItem("加密");
        JMenuItem decodeItem = new JMenuItem("解密");
        JMenuItem mutiItem = new JMenuItem("聚合加密");

        JMenu encryptItemMenu = new JMenu("加密");
        JMenu decryptItemMenu = new JMenu("解密");

        MessageEditorHttpRequestResponse messageEditorHttpRequestResponse = event.messageEditorRequestResponse().get();
        String host = messageEditorHttpRequestResponse.requestResponse().request().headerValue("Host");
        String pathWithoutQuery = messageEditorHttpRequestResponse.requestResponse().request().pathWithoutQuery();
        ArrayList<String[]> target_config = panel.findConf(host, pathWithoutQuery);

        sendTargetItem.addActionListener(l -> panel.addList(host, pathWithoutQuery));
        menuItemList.add(sendTargetItem);

        if (!messageEditorHttpRequestResponse.selectionOffsets().isEmpty()) {
            addTagItem.addActionListener(l -> handleAddTag(messageEditorHttpRequestResponse));
            menuItemList.add(addTagItem);

            if (!target_config.isEmpty()) {
                if (target_config.size() > 1) {
                    setupMultiConfigMenus(encryptItemMenu, decryptItemMenu, messageEditorHttpRequestResponse, target_config);
                    menuItemList.add(encryptItemMenu);
                    menuItemList.add(decryptItemMenu);
                } else {
                    setupSingleConfigMenus(encodeItem, decodeItem, messageEditorHttpRequestResponse, target_config);
                    menuItemList.add(encodeItem);
                    menuItemList.add(decodeItem);
                }
            }
        } else {
            if (!target_config.isEmpty()) {
                addMutiTagItem.addActionListener(l -> handleAddAggregateMarker(messageEditorHttpRequestResponse));
                menuItemList.add(addMutiTagItem);

                if ("1".equals(target_config.get(0)[ConfigIndex.JS_LIST_AGGREGATE])) {
                    mutiItem.addActionListener(l -> handleAggregateEncryption(messageEditorHttpRequestResponse, target_config.get(0)));
                    menuItemList.add(mutiItem);
                }
            }
        }

        return menuItemList;
    }

    private void handleAddTag(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse) {
        try {
            Range range = messageEditorHttpRequestResponse.selectionOffsets().get();
            HttpRequest httpRequest = messageEditorHttpRequestResponse.requestResponse().request();
            ByteArray selectByteArray = httpRequest.toByteArray().subArray(range);
            ByteArray startByteArray = httpRequest.toByteArray().subArray(0, range.startIndexInclusive());

            byte[] bytes = Arrays.copyOfRange(httpRequest.toByteArray().getBytes(), range.endIndexExclusive(), httpRequest.toByteArray().length());
            ByteArray endByteArray = ByteArray.byteArray(bytes);
            ByteArray newByteArray = startByteArray;
            String newSelect = "(-" + selectByteArray.toString() + "-)";

            newByteArray = newByteArray.withAppended(newSelect);
            newByteArray = newByteArray.withAppended(endByteArray);

            try {
                messageEditorHttpRequestResponse.setRequest(HttpRequest.httpRequest(newByteArray));
            } catch (Exception e) {
                if (!e.getMessage().contains("Item is read-only")) {
                    api.logging().logToError("添加标记失败", e);
                }
            }
        } catch (Exception e) {
            api.logging().logToError("添加标记失败", e);
        }
    }

    private void setupMultiConfigMenus(JMenu encryptItemMenu, JMenu decryptItemMenu,
                                       MessageEditorHttpRequestResponse messageEditorHttpRequestResponse,
                                       ArrayList<String[]> target_config) {
        setupEncryptMenu(encryptItemMenu, messageEditorHttpRequestResponse, target_config);
        setupDecryptMenu(decryptItemMenu, messageEditorHttpRequestResponse, target_config);
    }

    private void setupEncryptMenu(JMenu encryptItemMenu, MessageEditorHttpRequestResponse messageEditorHttpRequestResponse,
                                   ArrayList<String[]> target_config) {
        encryptItemMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                encryptItemMenu.removeAll();
                for (int i = 0; i < target_config.size(); i++) {
                    JMenuItem item = new JMenuItem(target_config.get(i)[ConfigIndex.JS_LIST_ENDE_CONFIG]);
                    String[] config = target_config.get(i);
                    item.addActionListener(l -> handleEncryption(messageEditorHttpRequestResponse, config));
                    encryptItemMenu.add(item);
                }
            }

            @Override
            public void menuDeselected(MenuEvent e) {}

            @Override
            public void menuCanceled(MenuEvent e) {}
        });
    }

    private void setupDecryptMenu(JMenu decryptItemMenu, MessageEditorHttpRequestResponse messageEditorHttpRequestResponse,
                                   ArrayList<String[]> target_config) {
        decryptItemMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                decryptItemMenu.removeAll();
                for (int i = 0; i < target_config.size(); i++) {
                    JMenuItem item = new JMenuItem(target_config.get(i)[ConfigIndex.JS_LIST_ENDE_CONFIG]);
                    String[] config = target_config.get(i);
                    item.addActionListener(l -> handleDecryption(messageEditorHttpRequestResponse, config));
                    decryptItemMenu.add(item);
                }
            }

            @Override
            public void menuDeselected(MenuEvent e) {}

            @Override
            public void menuCanceled(MenuEvent e) {}
        });
    }

    private void setupSingleConfigMenus(JMenuItem encodeItem, JMenuItem decodeItem,
                                        MessageEditorHttpRequestResponse messageEditorHttpRequestResponse,
                                        ArrayList<String[]> target_config) {
        String[] config = target_config.get(0);
        encodeItem.addActionListener(l -> handleEncryption(messageEditorHttpRequestResponse, config));
        decodeItem.addActionListener(l -> handleDecryption(messageEditorHttpRequestResponse, config));
    }

    private void handleEncryption(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse, String[] config) {
        try {
            Range range = messageEditorHttpRequestResponse.selectionOffsets().get();
            if ("REQUEST".equals(messageEditorHttpRequestResponse.selectionContext().name())) {
                processRequestEncryption(messageEditorHttpRequestResponse, range, config);
            } else {
                processResponseEncryption(messageEditorHttpRequestResponse, range, config);
            }
        } catch (Exception e) {
            api.logging().logToError("加密处理失败", e);
        }
    }

    private void handleDecryption(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse, String[] config) {
        try {
            Range range = messageEditorHttpRequestResponse.selectionOffsets().get();
            if ("REQUEST".equals(messageEditorHttpRequestResponse.selectionContext().name())) {
                processRequestDecryption(messageEditorHttpRequestResponse, range, config);
            } else {
                processResponseDecryption(messageEditorHttpRequestResponse, range, config);
            }
        } catch (Exception e) {
            api.logging().logToError("解密处理失败", e);
        }
    }

    private void processRequestEncryption(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse,
                                         Range range, String[] config) {
        HttpRequest httpRequest = messageEditorHttpRequestResponse.requestResponse().request();
        ByteArray selectByteArray = httpRequest.toByteArray().subArray(range);
        ByteArray startByteArray = httpRequest.toByteArray().subArray(0, range.startIndexInclusive());

        byte[] bytes = Arrays.copyOfRange(httpRequest.toByteArray().getBytes(), range.endIndexExclusive(), httpRequest.toByteArray().length());
        ByteArray endByteArray = ByteArray.byteArray(bytes);
        ByteArray newByteArray = startByteArray;
        String newSelect = selectByteArray.toString();

        String mode_config = config[ConfigIndex.JS_LIST_MODE];
        String ende_config = config[ConfigIndex.JS_LIST_ENDE_CONFIG];
        try {
            if ("".equals(ende_config)) {
                throw new Exception("加密配置为空");
            }
            newSelect = autoUtil.matchFunc(newSelect, ende_config, "", "", mode_config, true, true, false);
        } catch (Exception ee) {
            newSelect = selectByteArray.toString();
        }
        newByteArray = newByteArray.withAppended(newSelect);
        newByteArray = newByteArray.withAppended(endByteArray);

        try {
            messageEditorHttpRequestResponse.setRequest(HttpRequest.httpRequest(newByteArray));
        } catch (Exception ee) {
            if (!ee.getMessage().contains("Item is read-only")) {
                api.logging().logToError("请求加密失败", ee);
            }
        }
    }

    private void processResponseEncryption(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse,
                                          Range range, String[] config) {
        HttpResponse httpResponse = messageEditorHttpRequestResponse.requestResponse().response();
        ByteArray selectByteArray = httpResponse.toByteArray().subArray(range);
        ByteArray startByteArray = httpResponse.toByteArray().subArray(0, range.startIndexInclusive());

        byte[] bytes = Arrays.copyOfRange(httpResponse.toByteArray().getBytes(), range.endIndexExclusive(), httpResponse.toByteArray().length());
        ByteArray endByteArray = ByteArray.byteArray(bytes);
        ByteArray newByteArray = startByteArray;
        String newSelect = selectByteArray.toString();

        if (config != null) {
            String mode_config = config[ConfigIndex.JS_LIST_MODE];
            String ende_config = config[ConfigIndex.JS_LIST_ENDE_CONFIG];
            try {
                if ("".equals(ende_config)) {
                    throw new Exception("加密配置为空");
                }
                newSelect = autoUtil.matchFunc(newSelect, ende_config, "", "", mode_config, true, true, false);
            } catch (Exception ee) {
                newSelect = selectByteArray.toString();
            }
        }

        newByteArray = newByteArray.withAppended(newSelect);
        newByteArray = newByteArray.withAppended(endByteArray);

        try {
            messageEditorHttpRequestResponse.setResponse(HttpResponse.httpResponse(newByteArray));
        } catch (Exception ee) {
            if (!ee.getMessage().contains("Item is read-only")) {
                api.logging().logToError("响应加密失败", ee);
            }
        }
    }

    private void processRequestDecryption(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse,
                                         Range range, String[] config) {
        HttpRequest httpRequest = messageEditorHttpRequestResponse.requestResponse().request();
        ByteArray selectByteArray = httpRequest.toByteArray().subArray(range);
        ByteArray startByteArray = httpRequest.toByteArray().subArray(0, range.startIndexInclusive());

        byte[] bytes = Arrays.copyOfRange(httpRequest.toByteArray().getBytes(), range.endIndexExclusive(), httpRequest.toByteArray().length());
        ByteArray endByteArray = ByteArray.byteArray(bytes);
        ByteArray newByteArray = startByteArray;
        String newSelect = selectByteArray.toString();

        String mode_config = config[ConfigIndex.JS_LIST_MODE];
        String ende_config = config[ConfigIndex.JS_LIST_ENDE_CONFIG];
        try {
            if ("".equals(ende_config)) {
                throw new Exception("解密配置为空");
            }
            newSelect = autoUtil.matchFunc(newSelect, ende_config, "", "", mode_config, false, true, false);
        } catch (Exception ee) {
            newSelect = selectByteArray.toString();
        }
        newByteArray = newByteArray.withAppended(newSelect);
        newByteArray = newByteArray.withAppended(endByteArray);

        try {
            messageEditorHttpRequestResponse.setRequest(HttpRequest.httpRequest(newByteArray));
        } catch (Exception ee) {
            if (!ee.getMessage().contains("Item is read-only")) {
                api.logging().logToError("请求解密失败", ee);
            }
        }
    }

    private void processResponseDecryption(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse,
                                          Range range, String[] config) {
        HttpResponse httpResponse = messageEditorHttpRequestResponse.requestResponse().response();
        ByteArray selectByteArray = httpResponse.toByteArray().subArray(range);
        ByteArray startByteArray = httpResponse.toByteArray().subArray(0, range.startIndexInclusive());

        byte[] bytes = Arrays.copyOfRange(httpResponse.toByteArray().getBytes(), range.endIndexExclusive(), httpResponse.toByteArray().length());
        ByteArray endByteArray = ByteArray.byteArray(bytes);
        ByteArray newByteArray = startByteArray;
        String newSelect = selectByteArray.toString();

        if (config != null) {
            String mode_config = config[ConfigIndex.JS_LIST_MODE];
            String ende_config = config[ConfigIndex.JS_LIST_ENDE_CONFIG];
            try {
                if ("".equals(ende_config)) {
                    throw new Exception("解密配置为空");
                }
                newSelect = autoUtil.matchFunc(newSelect, ende_config, "", "", mode_config, false, true, false);
            } catch (Exception ee) {
                newSelect = selectByteArray.toString();
            }
        }

        newByteArray = newByteArray.withAppended(newSelect);
        newByteArray = newByteArray.withAppended(endByteArray);

        try {
            messageEditorHttpRequestResponse.setResponse(HttpResponse.httpResponse(newByteArray));
        } catch (Exception ee) {
            if (!ee.getMessage().contains("Item is read-only")) {
                api.logging().logToError("响应解密失败", ee);
            }
        }
    }

    private void handleAddAggregateMarker(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse) {
        try {
            int cursorPosition = messageEditorHttpRequestResponse.caretPosition();
            HttpRequest httpRequest = messageEditorHttpRequestResponse.requestResponse().request();
            ByteArray startByteArray = httpRequest.toByteArray().subArray(0, cursorPosition);

            byte[] bytes = Arrays.copyOfRange(httpRequest.toByteArray().getBytes(), cursorPosition, httpRequest.toByteArray().length());
            ByteArray endByteArray = ByteArray.byteArray(bytes);
            ByteArray newByteArray = startByteArray;
            String aggregateMarker = "(-AGGREGATE-)";

            newByteArray = newByteArray.withAppended(aggregateMarker);
            newByteArray = newByteArray.withAppended(endByteArray);

            try {
                messageEditorHttpRequestResponse.setRequest(HttpRequest.httpRequest(newByteArray));
            } catch (Exception e) {
                if (!e.getMessage().contains("Item is read-only")) {
                    api.logging().logToError("添加聚合标记失败", e);
                }
            }
        } catch (Exception e) {
            api.logging().logToError("无法获取光标位置", e);
        }
    }

    private void handleAggregateEncryption(MessageEditorHttpRequestResponse messageEditorHttpRequestResponse, String[] config) {
        HttpRequest httpRequest = messageEditorHttpRequestResponse.requestResponse().request();

        String reg = config[ConfigIndex.JS_LIST_REGEX];
        String ende_config = config[ConfigIndex.JS_LIST_ENDE_CONFIG];
        String mode_config = config[ConfigIndex.JS_LIST_MODE];
        String tag = config[ConfigIndex.JS_LIST_KEEP_PREFIX_SUFFIX];

        try {
            HttpRequest result = autoUtil.processAggregateRequest(httpRequest, ende_config, reg, tag, mode_config);

            if (result != null) {
                try {
                    messageEditorHttpRequestResponse.setRequest(result);
                } catch (Exception e) {
                    if (!e.getMessage().contains("Item is read-only")) {
                        api.logging().logToError("聚合加密失败", e);
                    }
                }
            }
        } catch (Exception e) {
            api.logging().logToError("聚合加密异常", e);
        }
    }
}