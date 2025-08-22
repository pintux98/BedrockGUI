package it.pintux.life.paper;

import it.pintux.life.common.actions.handlers.ListActionHandler;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.paper.data.PaperDataProvider;
import it.pintux.life.paper.placeholders.BedrockGUIExpansion;
import it.pintux.life.paper.platform.PaperFormSender;
import it.pintux.life.paper.platform.PaperPlayerChecker;
import it.pintux.life.paper.platform.PaperCommandExecutor;
import it.pintux.life.paper.platform.PaperEconomyManager;
import it.pintux.life.paper.platform.PaperSoundManager;
import it.pintux.life.paper.platform.PaperTitleManager;

// import it.pintux.life.common.form.EnhancedFormMenuUtil;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.paper.utils.PaperConfig;
import it.pintux.life.paper.utils.PaperPlayer;
import it.pintux.life.paper.utils.PaperMessageConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public final class BedrockGUI extends JavaPlugin implements Listener {

    private FormMenuUtil formMenuUtil;
    //private EnhancedFormMenuUtil enhancedFormMenuUtil;
    private MessageData messageData;
    private BedrockGUIApi api;


    @Override
    public void onEnable() {
        getCommand("bedrockgui").setExecutor(new BedrockCommand(this));
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        reloadData();
        //setupGeyserIntegration();
        new Metrics(this, 23364);
    }

    @Override
    public void onDisable() {
        if (api != null) {
            // Cleanup any resources
            getLogger().info("BedrockGUI with Resource Pack API disabled");
        }
    }

    public void reloadData() {
        reloadConfig();
        this.saveResource("messages.yml", false);

        File dataFolder = getDataFolder();
        MessageConfig configHandler = new PaperMessageConfig(dataFolder, "messages.yml");
        messageData = new MessageData(configHandler);

        PaperCommandExecutor commandExecutor = new PaperCommandExecutor();
        PaperSoundManager soundManager = new PaperSoundManager();
        PaperEconomyManager economyManager = new PaperEconomyManager(this);
        PaperFormSender formSender = new PaperFormSender();
        PaperDataProvider dataProvider = new PaperDataProvider();
        PaperTitleManager titleManager = new PaperTitleManager();

        api = new BedrockGUIApi(new PaperConfig(getConfig()), messageData, commandExecutor, soundManager, economyManager, formSender, titleManager, dataProvider);

        // EnhancedFormMenuUtil enhancedFormMenuUtil = new EnhancedFormMenuUtil(
        //     new PaperConfig(getConfig()), 
        //     messageData, 
        //     null,
        //     new PaperPlayerChecker(), 
        //     formSender
        // );
        
        // Use the FormMenuUtil from the API to avoid duplicate registration
        formMenuUtil = api.getFormMenuUtil();
        getLogger().info("Using FormMenuUtil from BedrockGUIApi");

        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BedrockGUIExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered");
        }

        // Register List action handler
        ListActionHandler.register();
        getLogger().info("List action handler registered");

        getLogger().info("BedrockGUI loaded and enabled");
        //getLogger().info("BedrockGUI loaded with Resource Pack API support");
        //if (api.isResourcePacksEnabled()) {
        //    getLogger().info("Resource packs are enabled!");
        //} else {
        //    getLogger().info("Resource packs are disabled. Enable in config.yml to use enhanced features.");
        //}
    }

    @EventHandler
    public void onCmd(ServerCommandEvent event) {
        if (!(event.getSender() instanceof Player)) return;
        PaperPlayer player = new PaperPlayer((Player) event.getSender());
        String command = event.getCommand();

        formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
            if (formMenu.getFormCommand() != null && command.startsWith(formMenu.getFormCommand())) {
                event.setCancelled(true);

                String[] parts = command.split(" ");
                String[] args = Arrays.copyOfRange(parts, 1, parts.length);

                api.openMenu(player, key, args);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerPreprocessCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        PaperPlayerChecker playerChecker = new PaperPlayerChecker();
        if (!playerChecker.isBedrockPlayer(player.getUniqueId())) {
            return;
        }
        String message = event.getMessage();

        String commandWithoutSlash = message.substring(1).toLowerCase();

        String[] parts = commandWithoutSlash.split(" ");
        String commandName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
            String formCommand = formMenu.getFormCommand();

            if (formCommand != null) {
                String[] formCommandParts = formCommand.split(" ");
                String baseCommand = formCommandParts[0];

                if (commandName.equalsIgnoreCase(baseCommand)) {
                    int requiredArgs = formCommandParts.length - 1;
                    if (args.length >= requiredArgs) {
                        event.setCancelled(true);
                        PaperPlayer player1 = new PaperPlayer(event.getPlayer());
                        api.openMenu(player1, key, args);
                    } else {
                        player.sendMessage(messageData.getValue(MessageData.MENU_ARGS, Map.of("args", requiredArgs), null));
                    }
                }
            }
        });
    }


    public FormMenuUtil getFormMenuUtil() {
        return formMenuUtil;
    }

    public MessageData getMessageData() {
        return messageData;
    }

    /**
     * Sets up Geyser integration for resource pack management
     */
    private void setupGeyserIntegration() {
        try {
            if (getServer().getPluginManager().getPlugin("Geyser-Spigot") != null) {
                // Platform-specific resource pack listener initialization removed
                // GeyserApi.api().eventBus().register(this, geyserListener);
                getLogger().info("Geyser integration enabled for resource pack management");
            } else {
                getLogger().warning("Geyser not found. Resource pack features will be limited.");
            }
        } catch (Exception e) {
            getLogger().warning("Failed to setup Geyser integration: " + e.getMessage());
        }
    }

    /**
     * Gets the enhanced form menu utility
     */
    // public EnhancedFormMenuUtil getEnhancedFormMenuUtil() {
    //     return null;
    // }

    /**
     * Gets the BedrockGUI API instance
     */
    public BedrockGUIApi getApi() {
        return api;
    }

    /**
     * Reloads resource packs
     */
    // public void reloadResourcePacks() {
    //     if (api != null) {
    //         api.reload();
    //         getLogger().info("Resource packs reloaded");
    //     }
    // }
}
