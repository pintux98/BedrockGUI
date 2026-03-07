package it.pintux.life.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.AssetServer;
import it.pintux.life.common.utils.FormSender;
import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.velocity.platform.*;
import it.pintux.life.velocity.utils.VelocityConfig;
import it.pintux.life.velocity.utils.VelocityMessageConfig;
import it.pintux.life.velocity.utils.DependencyValidator;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class BedrockGUI {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private FormMenuUtil formMenuUtil;
    private MessageData messageData;
    private BedrockGUIApi api;
    private VelocityConfig config;

    @Inject
    public BedrockGUI(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Enabling BedrockGUI for Velocity...");

        if (!dataDirectory.toFile().exists()) {
            dataDirectory.toFile().mkdirs();
        }

        reloadData();

        server.getCommandManager().register("bedrockgui", new VelocityCommandExecutor(this));

        logger.info("BedrockGUI for Velocity enabled successfully!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Disabling BedrockGUI for Velocity...");

        if (api != null) {
            try {
                api.shutdown();
                logger.info("BedrockGUI shutdown completed successfully");
            } catch (Exception e) {
                logger.error("Error during BedrockGUI shutdown: " + e.getMessage(), e);
            }
        }

        logger.info("BedrockGUI for Velocity disabled");
    }

    public void reloadData() {
        if (!DependencyValidator.validateDependencies()) {
            logger.warn("Some dependencies have compatibility issues. Plugin will continue but some features may not work properly.");
        }

        File dataFolder = dataDirectory.toFile();
        config = new VelocityConfig(dataFolder);

        MessageConfig configHandler = new VelocityMessageConfig(dataFolder, "messages.yml");
        messageData = new MessageData(configHandler);

        if (api != null) {
            logger.info("Reloading BedrockGUI with fresh instances to avoid stale cache...");
            try {
                api.shutdown();
            } catch (Exception ex) {
                logger.warn("Error shutting down API during reload: " + ex.getMessage());
            }
        }

        it.pintux.life.velocity.platform.VelocityCommandExecutor commandExecutor = new it.pintux.life.velocity.platform.VelocityCommandExecutor(getServer());
        FormSender formSender = new FormSender();
        VelocityTitleManager titleManager = new VelocityTitleManager();
        VelocityPluginManager pluginManager = new VelocityPluginManager(server);
        VelocityPlayerManager playerManager = new VelocityPlayerManager(server);
        AssetServer assetServer = new AssetServer(server.getBoundAddress().getHostString(), 8192, dataFolder);
        assetServer.start();

        api = new BedrockGUIApi(config, messageData, commandExecutor, null, null,
                formSender, titleManager, pluginManager, playerManager, new it.pintux.life.velocity.platform.VelocityScheduler(getServer()));

        formMenuUtil = api.getFormMenuUtil();
        formMenuUtil.setAssetServer(assetServer);
        logger.info("Using FormMenuUtil from BedrockGUIApi");

        logger.info("BedrockGUI for Velocity loaded and enabled");

        try {
            formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
                String formCmd = formMenu.getFormCommand();
                if (formCmd != null && !formCmd.isEmpty()) {
                    String base = formCmd.trim().split("\\s+")[0].toLowerCase();
                    server.getCommandManager().register(base, new FormCommand(this, key));
                }
            });
        } catch (Exception ignored) {
        }
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
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
