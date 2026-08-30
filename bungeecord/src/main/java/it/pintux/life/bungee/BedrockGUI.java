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
        if (!new File(getDataFolder(), "config.yml").exists()) {
            int extracted = it.pintux.life.common.utils.DefaultFormsExtractor.extract(getDataFolder(), getLogger()::warning);
            getLogger().info("First run: extracted " + extracted + " default form file(s) to forms/");
        }
        migrateConfigurations();
        reloadData();
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new BungeeCommand(this));
        new Metrics(this, 23364);
    }

    private void migrateConfigurations() {
        it.pintux.life.common.config.ConfigMigrator
                .of(getDataFolder(), "config.yml", () -> getResourceAsStream("config.yml"),
                        getLogger()::info, getLogger()::warning)
                .preserve("forms")
                .migrate();
        it.pintux.life.common.config.ConfigMigrator
                .of(getDataFolder(), "messages.yml", () -> getResourceAsStream("messages.yml"),
                        getLogger()::info, getLogger()::warning)
                .migrate();
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
        if (assetServer != null) {
            assetServer.shutdown();
        }
        assetServer = AssetServer.fromConfig(config, getProxy().getConfig().getListeners().stream().findFirst().map(s -> s.getHost().getHostString()).orElse("127.0.0.1"), 8193, dataFolder);
        if (assetServer != null) {
            assetServer.start();
        }

        api = new BedrockGUIApi(config, messageData, commandExecutor, null, null,
                formSender, titleManager, pluginManager, playerManager, new it.pintux.life.bungee.platform.BungeeScheduler(this));

        it.pintux.life.bungee.placeholders.CorePlaceholders.register(api.getPlaceholderRegistry(), getProxy());

        formMenuUtil = api.getFormMenuUtil();
        formMenuUtil.setAssetServer(assetServer);

        try {
            formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
                String formCmd = formMenu.getFormCommand();
                if (formCmd != null && !formCmd.isEmpty()) {
                    String base = formCmd.trim().split("\\s+")[0];
                    getProxy().getPluginManager().registerCommand(this, new BungeeFormCommand(this, key, "bedrockgui.form." + key));
                }
            });
        } catch (Exception ignored) {
        }
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
