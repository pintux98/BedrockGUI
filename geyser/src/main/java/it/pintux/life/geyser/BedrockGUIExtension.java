package it.pintux.life.geyser;

import it.pintux.life.common.BedrockGuiAPI;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.geyser.platform.*;
import it.pintux.life.geyser.utils.GeyserConfig;
import it.pintux.life.geyser.utils.GeyserMessageConfig;
import it.pintux.life.geyser.utils.GeyserPlayer;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.event.connection.ConnectionEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandExecutor;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;

import java.util.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.geysermc.geyser.api.extension.ExtensionLogger;

public class BedrockGUIExtension implements Extension {

    private FormMenuUtil formMenuUtil;
    private MessageData messageData;
    private BedrockGuiAPI api;
    private ExtensionLogger logger;
    private Metrics metrics;


    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        this.logger = this.logger();
        logger.info("BedrockGUI Extension loading...");
        reloadData();
    }

    @Subscribe
    public void onGeyserPostInitialize(GeyserPostInitializeEvent event) {
        logger.info("BedrockGUI Extension initializing...");

        // Initialize metrics
        initializeMetrics();

        logger.info("BedrockGUI Extension enabled successfully!");
    }

    @Subscribe
    public void onGeyserShutdown(GeyserShutdownEvent event) {
        if (api != null) {
            logger.info("BedrockGUI Extension disabled");
        }
    }

    private void initializeMetrics() {
        try {
            // Service ID for BedrockGUI on bStats (you would need to register on bStats to get this)
            int serviceId = 12345; // Replace with actual service ID from bStats
            metrics = new Metrics(this, serviceId);

            // Add custom charts
            metrics.addCustomChart(new Metrics.SimplePie("resource_packs_enabled", () ->
                    api != null && api.isResourcePacksEnabled() ? "Enabled" : "Disabled"
            ));

            logger.info("Metrics initialized successfully!");
        } catch (Exception e) {
            logger.warning("Failed to initialize metrics: " + e.getMessage());
        }
    }

    @Subscribe
    public void onPlayerConnect(ConnectionEvent event) {
        if (api != null && event.connection() != null) {
            // Handle player connection for resource pack management
            api.onPlayerJoin(event.connection().javaUuid());
        }
    }

    private void reloadData() {
        try {
            Path dataFolder = this.dataFolder();
            File configFile = dataFolder.resolve("config.yml").toFile();
            File messagesFile = dataFolder.resolve("messages.yml").toFile();

            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
            }

            if (!messagesFile.exists()) {
                messagesFile.getParentFile().mkdirs();
            }

            MessageConfig configHandler = new GeyserMessageConfig(dataFolder.toFile(), "messages.yml");
            messageData = new MessageData(configHandler);

            api = BedrockGuiAPI.getInstance();
            GeyserConfig geyserConfig = new GeyserConfig(dataFolder.resolve("config.yml").toFile());
            api.initialize(geyserConfig, messageData);

            GeyserFormSender formSender = new GeyserFormSender();

            formMenuUtil = new FormMenuUtil(
                    geyserConfig,
                    messageData,
                    null,
                    null,
                    null,
                    formSender
            );

            logger.info("BedrockGUI loaded with Resource Pack API support");
            if (api.isResourcePacksEnabled()) {
                logger.info("Resource packs are enabled!");
            } else {
                logger.info("Resource packs are disabled. Enable in config.yml to use enhanced features.");
            }
        } catch (Exception e) {
            logger.severe("Failed to reload data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onDefineCommands(GeyserDefineCommandsEvent event) {
        // Register the main bedrockgui command
        event.register(
                Command.builder(this)
                        .name("bedrockgui")
                        .description("BedrockGUI main command")
                        .aliases(Arrays.asList("bgui"))
                        .source(CommandSource.class)
                        .executor((source, command, args) -> {
                            if (args.length == 0) {
                                source.sendMessage("BedrockGUI Extension v" + this.description().version());
                                source.sendMessage("Available commands:");
                                source.sendMessage("  /bedrockgui reload - Reload configuration");
                                source.sendMessage("  /bedrockgui info - Show extension information");
                                return;
                            }

                            String subCommand = args[0].toLowerCase();
                            switch (subCommand) {
                                case "reload":
                                    if (source.hasPermission("bedrockgui.admin")) {
                                        reload();
                                        source.sendMessage("BedrockGUI Extension reloaded successfully!");
                                    } else {
                                        source.sendMessage("You don't have permission to use this command.");
                                    }
                                    break;

                                case "info":
                                    source.sendMessage("BedrockGUI Extension Information:");
                                    source.sendMessage("Version: " + this.description().version());
                                    source.sendMessage("Author: " + this.description().authors());
                                    source.sendMessage("Resource Packs: " + (api != null && api.isResourcePacksEnabled() ? "Enabled" : "Disabled"));
                                    break;

                                default:
                                    source.sendMessage("Unknown subcommand. Use /bedrockgui for help.");
                                    break;
                            }
                        })
                        .build()
        );

        // Register custom commands from config.yml menus
        registerMenuCommands(event);
    }

    private void registerMenuCommands(GeyserDefineCommandsEvent event) {
        try {
            if (api != null && api.getConfig() != null) {
                // Extract commands from menu configuration
                Set<String> menuKeys = api.getConfig().getKeys("menu");
                if (menuKeys != null) {
                    for (String menuKey : menuKeys) {
                        String commandPath = "menu." + menuKey + ".command";
                        String commandName = api.getConfig().getString(commandPath);

                        if (commandName != null && !commandName.trim().isEmpty()) {
                            String menuTitle = api.getConfig().getString("menu." + menuKey + ".title", "Menu");

                            logger.info("Registering menu command: /" + commandName + " for menu: " + menuKey);

                            event.register(
                                    Command.builder(this)
                                            .name(commandName)
                                            .description("Open " + menuTitle + " menu")
                                            .source(CommandSource.class)
                                            .executor((source, command, args) -> {
                                                try {
                                                    Collection<? extends GeyserConnection> connections = GeyserApi.api().onlineConnections();
                                                    GeyserConnection connection = null;

                                                    for (GeyserConnection con : connections) {
                                                        if (con.bedrockUsername().equalsIgnoreCase(source.name()) ||
                                                            con.javaUsername().equalsIgnoreCase(source.name())) {
                                                            connection = con;
                                                            break;
                                                        }
                                                    }
                                                    if (connection != null) {
                                                        GeyserPlayer geyserPlayer = new GeyserPlayer(connection);
                                                        if (formMenuUtil != null) {
                                                            formMenuUtil.openForm(geyserPlayer, menuKey, args);
                                                            logger.info("Opened menu '" + menuKey + "' for player: " + connection.javaUsername());
                                                        } else {
                                                            source.sendMessage("Menu system is not initialized. Please contact an administrator.");
                                                        }
                                                    } else {
                                                        logger.warning("We cannot determine command executor in this scenario for menu: " + menuKey);
                                                    }

                                                } catch (Exception e) {
                                                    logger.warning("Failed to process menu command " + commandName + ": " + e.getMessage());
                                                    source.sendMessage("Failed to open menu. Please try again.");
                                                }
                                            })
                                            .build()
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to register menu commands: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public FormMenuUtil getFormMenuUtil() {
        return formMenuUtil;
    }

    public MessageData getMessageData() {
        return messageData;
    }

    public BedrockGuiAPI getApi() {
        return api;
    }

    /**
     * Reloads the extension configuration and data
     */
    public void reload() {
        reloadData();
        logger.info("BedrockGUI Extension reloaded");
    }

}