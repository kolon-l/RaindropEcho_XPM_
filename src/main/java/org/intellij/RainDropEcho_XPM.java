package org.intellij;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;

import java.net.InetSocketAddress;
import java.util.Arrays;

public class RainDropEcho_XPM implements BurpExtension, ExtensionUnloadingHandler {
    private String LOGO = """
    __________        .__       ________                     ___________      .__               ____  ___
    \\______   \\_____  |__| ____ \\______ \\_______  ____ ______\\_   _____/ ____ |  |__   ____     \\   \\/  /
     |       _/\\__  \\ |  |/    \\ |    |  \\_  __ \\/  _ \\\\____ \\|    __)__/ ___\\|  |  \\ /  _ \\     \\     /\s
     |    |   \\ / __ \\|  |   |  \\|    `   \\  | \\(  <_> )  |_> >        \\  \\___|   Y  (  <_> )    /     \\\s
     |____|_  /(____  /__|___|  /_______  /__|   \\____/|   __/_______  /\\___  >___|  /\\____/____/___/\\  \\
            \\/      \\/        \\/        \\/             |__|          \\/     \\/     \\/     /_____/     \\_/
                                                               |_|      v2.0.0
                                                                        by: TingYuSYS
                                                                        modify by : kolon
    
    [-] Loading Plugin Success!
    """;
    private String PluginName = "Raindrop Echo_XPM";
    private MontoyaApi api;
    private RootPanel rootPanel;
    private AutoUtil autoUtil;
    private RequestResponseHandler reqResHandler;
    private TagContextMenu menu;
    @Override
    public void initialize(MontoyaApi montoyaApi) {

        api = montoyaApi;
        api.extension().setName(PluginName);
        api.extension().registerUnloadingHandler( this);

        autoUtil = new AutoUtil(api);
        rootPanel = new RootPanel(api,autoUtil);
        api.userInterface().registerSuiteTab(PluginName,rootPanel.$$$getRootComponent$$$());
        reqResHandler = new RequestResponseHandler(api,rootPanel,autoUtil);
        api.http().registerHttpHandler(reqResHandler);

        api.userInterface().registerContextMenuItemsProvider( new TagContextMenu(api,rootPanel));

        api.logging().logToOutput(LOGO);

    }

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