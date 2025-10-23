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
import it.pintux.life.paper.platform.PaperPluginManager;
import it.pintux.life.paper.platform.PaperPlayerManager;

import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.paper.utils.PaperConfig;
import it.pintux.life.paper.utils.PaperPlayer;
import it.pintux.life.paper.utils.PaperMessageConfig;
import it.pintux.life.paper.utils.DependencyValidator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public final class BedrockGUI extends JavaPlugin implements Listener {

    private FormMenuUtil formMenuUtil;
    private MessageData messageData;
    private BedrockGUIApi api;


    @Override
    public void onEnable() {
        getCommand("bedrockgui").setExecutor(new BedrockCommand(this));
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        reloadData();
        new Metrics(this, 23364);
    }

    @Override
    public void onDisable() {
        if (api != null) {
            try {
                api.shutdown();
                getLogger().info("BedrockGUI shutdown completed successfully");
            } catch (Exception e) {
                getLogger().severe("Error during BedrockGUI shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }
        getLogger().info("BedrockGUI disabled");
    }

    public void reloadData() {
        
        if (!DependencyValidator.validateDependencies()) {
            getLogger().warning("Some dependencies have compatibility issues. Plugin will continue but some features may not work properly.");
        }
        reloadConfig();
        this.saveResource("messages.yml", false);

        File dataFolder = getDataFolder();
        MessageConfig configHandler = new PaperMessageConfig(dataFolder, "messages.yml");
        messageData = new MessageData(configHandler);

        
        if (api != null) {
            getLogger().info("Reloading existing BedrockGUI configuration...");
            api.reloadConfiguration();
            getLogger().info("Configuration reloaded successfully");
            return;
        }

        
        PaperCommandExecutor commandExecutor = new PaperCommandExecutor();
        PaperSoundManager soundManager = new PaperSoundManager();
        PaperEconomyManager economyManager = null;
        if (DependencyValidator.isPluginCompatible("Vault", "1.7.0")) {
            economyManager = new PaperEconomyManager(this);
            getLogger().info("Vault integration enabled");
        } else if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            getLogger().warning("Vault found but version is incompatible. Economy features disabled.");
        }
        PaperFormSender formSender = new PaperFormSender();
        PaperDataProvider dataProvider = new PaperDataProvider();
        PaperTitleManager titleManager = new PaperTitleManager();
        PaperPluginManager pluginManager = new PaperPluginManager();
        PaperPlayerManager playerManager = new PaperPlayerManager();

        api = new BedrockGUIApi(new PaperConfig(getConfig()), messageData, commandExecutor, soundManager, economyManager, formSender, titleManager, dataProvider, pluginManager, playerManager);

        formMenuUtil = api.getFormMenuUtil();
        getLogger().info("Using FormMenuUtil from BedrockGUIApi");

        if (DependencyValidator.isPluginCompatible("PlaceholderAPI", "2.10.0")) {
            new BedrockGUIExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered");
        } else if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().warning("PlaceholderAPI found but version is incompatible. Placeholder features disabled.");
        }

        
        getLogger().info("BedrockGUI loaded and enabled");
    }

    @EventHandler
    public void onCmd(ServerCommandEvent event) {
        if (!(event.getSender() instanceof Player)) return;
        
        PaperPlayerChecker playerChecker = new PaperPlayerChecker();
        Player player = (Player) event.getSender();
        if (!playerChecker.isBedrockPlayer(player.getUniqueId())) {
            return;
        }
        
        PaperPlayer paperPlayer = new PaperPlayer(player);
        String command = event.getCommand();

        formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
            if (formMenu.getFormCommand() != null && command.startsWith(formMenu.getFormCommand())) {
                event.setCancelled(true);

                String[] parts = command.split(" ");
                String[] args = Arrays.copyOfRange(parts, 1, parts.length);

                api.openMenu(paperPlayer, key, args);
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

    
    public BedrockGUIApi getApi() {
        return api;
    }
}

