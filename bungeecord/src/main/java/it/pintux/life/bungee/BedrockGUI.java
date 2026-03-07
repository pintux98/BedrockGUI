package it.pintux.life.bungee;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.AssetServer;
import it.pintux.life.common.utils.FormSender;
import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.bungee.platform.*;
import it.pintux.life.bungee.utils.*;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;

public class BedrockGUI extends Plugin {

    private FormMenuUtil formMenuUtil;
    private MessageData messageData;
    private BedrockGUIApi api;
    private BungeeConfig config;
    private AssetServer assetServer;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        reloadData();
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new BungeeCommand(this));
    }

    @Override
    public void onDisable() {
        if (api != null) {
            try {
                api.shutdown();
            } catch (Exception ignored) {
            }
        }
        if (assetServer != null) {
            assetServer.shutdown();
            assetServer = null;
        }
    }

    public void reloadData() {
        File dataFolder = getDataFolder();
        config = new BungeeConfig(dataFolder);
        MessageConfig configHandler = new BungeeMessageConfig(dataFolder, "messages.yml");
        messageData = new MessageData(configHandler);

        if (api != null) {
            try {
                api.shutdown();
            } catch (Exception ignored) {
            }
        }

        BungeeCommandExecutor commandExecutor = new BungeeCommandExecutor();
        FormSender formSender = new FormSender();
        BungeeTitleManager titleManager = new BungeeTitleManager();
        BungeePluginManager pluginManager = new BungeePluginManager(getProxy());
        BungeePlayerManager playerManager = new BungeePlayerManager(getProxy());
        assetServer = new AssetServer(getProxy().getConfig().getListeners().stream().findFirst().map(s -> s.getHost().getHostString()).orElse("127.0.0.1"), 8193, dataFolder);
        assetServer.start();

        api = new BedrockGUIApi(config, messageData, commandExecutor, null, null,
                formSender, titleManager, pluginManager, playerManager, new it.pintux.life.bungee.platform.BungeeScheduler(this));

        formMenuUtil = api.getFormMenuUtil();
        formMenuUtil.setAssetServer(assetServer);
    }

    public FormMenuUtil getFormMenuUtil() {
        return formMenuUtil;
    }

    public MessageData getMessageData() {
        return messageData;
    }

    public BedrockGUIApi getApi() {
        return api;
    }
}
