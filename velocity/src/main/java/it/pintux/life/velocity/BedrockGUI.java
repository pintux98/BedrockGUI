package it.pintux.life.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.velocity.platform.*;
import it.pintux.life.velocity.utils.VelocityConfig;
import it.pintux.life.velocity.utils.VelocityMessageConfig;
import it.pintux.life.velocity.utils.VelocityPlayer;
import it.pintux.life.velocity.utils.DependencyValidator;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

@Plugin(
    id = "bedrockgui-velocity",
    name = "BedrockGUI-Velocity",
    version = "2.0.3-BETA",
    description = "BedrockGUI plugin for Velocity proxy",
    authors = {"pintux"},
    dependencies = {
        @Dependency(id = "floodgate")
    }
)
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
        
        // Create data directory if it doesn't exist
        if (!dataDirectory.toFile().exists()) {
            dataDirectory.toFile().mkdirs();
        }
        
        reloadData();
        
        // Register event listeners
        server.getEventManager().register(this, this);
        
        // Register command
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

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        // Handle player login events if needed
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        // Handle player disconnect events if needed
    }

    public void reloadData() {
        if (!DependencyValidator.validateDependencies()) {
            logger.warn("Some dependencies have compatibility issues. Plugin will continue but some features may not work properly.");
        }

        // Load configuration
        File dataFolder = dataDirectory.toFile();
        config = new VelocityConfig(dataFolder);
        
        // Load messages
        MessageConfig configHandler = new VelocityMessageConfig(dataFolder, "messages.yml");
        messageData = new MessageData(configHandler);

        // Shutdown existing API if reloading
        if (api != null) {
            logger.info("Reloading BedrockGUI with fresh instances to avoid stale cache...");
            try {
                api.shutdown();
            } catch (Exception ex) {
                logger.warn("Error shutting down API during reload: " + ex.getMessage());
            }
        }

        // Initialize platform-specific managers
        it.pintux.life.velocity.platform.VelocityCommandExecutor commandExecutor = new it.pintux.life.velocity.platform.VelocityCommandExecutor();
        VelocitySoundManager soundManager = new VelocitySoundManager();
        VelocityEconomyManager economyManager = null; // Economy not typically available on proxy
        VelocityFormSender formSender = new VelocityFormSender(server);
        VelocityTitleManager titleManager = new VelocityTitleManager();
        VelocityPluginManager pluginManager = new VelocityPluginManager(server);
        VelocityPlayerManager playerManager = new VelocityPlayerManager(server);

        // Create API instance
        api = new BedrockGUIApi(config, messageData, commandExecutor, soundManager, economyManager, 
                               formSender, titleManager, pluginManager, playerManager);

        formMenuUtil = api.getFormMenuUtil();
        logger.info("Using FormMenuUtil from BedrockGUIApi");
        
        logger.info("BedrockGUI for Velocity loaded and enabled");
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

    public VelocityConfig getConfig() {
        return config;
    }
}