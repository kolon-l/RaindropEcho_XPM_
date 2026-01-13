package org.intellij;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;

/**
 * RainDropEcho_XPM - Burp Suite插件主类
 * 
 * <p>实现Burp Suite的BurpExtension接口，提供自动加解密功能。
 * 该插件支持多种加解密模式：
 * <ul>
 *   <li>JSFile模式：通过Node.js执行JavaScript文件进行加解密</li>
 *   <li>API模式：通过HTTP API调用进行加解密</li>
 *   <li>WebSocket模式：通过WebSocket连接进行加解密</li>
 * </ul>
 * 
 * <p>主要功能：
 * <ul>
 *   <li>自动拦截和处理HTTP请求/响应</li>
 *   <li>支持正则表达式匹配需要加解密的数据</li>
 *   <li>支持多字段聚合加解密</li>
 *   <li>提供右键菜单快速加解密功能</li>
 *   <li>支持配置管理界面</li>
 * </ul>
 * 
 * @author TingYuSYS
 * @author kolon (modify)
 * @version 2.3.0
 */
public class RainDropEcho_XPM implements BurpExtension, ExtensionUnloadingHandler {
    
    /**
     * 插件Logo和版本信息
     */
    private String LOGO = """
    __________        .__       ________                     ___________      .__               ____  ___
    \\______   \\_____  |__| ____ \\______ \\_______  ____ ______\\_   _____/ ____ |  |__   ____     \\   \\/  /
     |       _/\\__  \\ |  |/    \\ |    |  \\_  __ \\/  _ \\\\____ \\|    __)__/ ___\\|  |  \\ /  _ \\     \\     /
     |    |   \\ / __ \\|  |   |  \\|    `   \\  | \\(  <_> )  |_> >        \\  \\___|   Y  (  <_> )    /     \\
     |____|_  /(____  /__|___|  /_______  /__|   \\____/|   __/_______  /\\___  >___|  /\\____/____/___/\\  \\
            \\/      \\/        \\/        \\/             |__|          \\/     \\/     \\/     /_____/     \\_/
                                                               |_|      v3.0.0
                                                                        by: TingYuSYS
                                                                        modify by : kolon
    
    [-] Loading Plugin Success!

    聚合处理说明：
    1、用于将多个标记点的数据聚合后进行处理，聚合字段如“数据1§数据2§数据3”，即向js/API/WebSocket的传入数据。
    2、使用右键聚合时，需先标记所有数据（非聚合模式不用标记，选中即可）。
    """;
    
    /**
     * 插件名称
     */
    private String PluginName = "Raindrop Echo_XPM";
    
    /**
     * Burp Suite Montoya API实例
     */
    private MontoyaApi api;
    
    /**
     * 主面板实例，用于显示配置管理界面
     */
    private RootPanel rootPanel;
    
    /**
     * 自动加解密工具类实例
     */
    private AutoUtil autoUtil;
    
    /**
     * HTTP请求/响应处理器实例
     */
    private RequestResponseHandler reqResHandler;
    
    /**
     * 右键菜单提供者实例
     */
    private TagContextMenu menu;
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        api = montoyaApi;
        api.extension().setName(PluginName);
        api.extension().registerUnloadingHandler(this);

        autoUtil = new AutoUtil(api);
        rootPanel = new RootPanel(api, autoUtil);
        api.userInterface().registerSuiteTab(PluginName, rootPanel.$$$getRootComponent$$$());

        reqResHandler = new RequestResponseHandler(api, rootPanel, autoUtil);
        api.http().registerHttpHandler(reqResHandler);

        api.userInterface().registerContextMenuItemsProvider(new TagContextMenu(api, rootPanel, autoUtil));

        api.logging().logToOutput(LOGO);
    }

    /**
     * 插件卸载时的清理工作
     * 
     * <p>当插件被卸载时，此方法会被调用，执行以下清理操作：
     * <ul>
     *   <li>关闭所有WebSocket服务器连接</li>
     *   <li>释放相关资源</li>
     *   <li>输出卸载日志信息</li>
     * </ul>
     * 
     * @throws RuntimeException 如果清理过程中发生中断异常
     */
    @Override
    public void extensionUnloaded() {
        try {
            autoUtil.deleteWSServers();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        api.logging().logToOutput("[*] Extension has been unloaded.");
    }
}