package it.pintux.life.paper;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.AssetServer;
import it.pintux.life.common.utils.FormSender;
import it.pintux.life.paper.placeholders.BedrockGUIExpansion;
import it.pintux.life.paper.platform.*;

import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.paper.utils.PaperConfig;
import it.pintux.life.paper.utils.PaperPlayer;
import it.pintux.life.paper.utils.PaperMessageConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public final class BedrockGUI extends JavaPlugin implements Listener {

    private FormMenuUtil formMenuUtil;
    private MessageData messageData;
    private BedrockGUIApi api;
    private AssetServer assetServer;
    private PaperPlayerChecker playerChecker;

    @Override
    public void onEnable() {
        PluginCommand cmd = getCommand("bedrockgui");
        BedrockCommand executor = new BedrockCommand(this);
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        this.saveResource("messages.yml", false);
        reloadData();
        new Metrics(this, 23364);
    }

    @Override
    public void onDisable() {
        if (api != null) {
            try {
                api.shutdown();
                getLogger().info("Shutdown completed successfully");
            } catch (Exception e) {
                getLogger().severe("Error during shutdown: " + e.getMessage());
            }
        }
        if (assetServer != null) {
            assetServer.shutdown();
            assetServer = null;
        }
        getLogger().info("Disabled");
    }

    public void reloadData() {
        reloadConfig();

        File dataFolder = getDataFolder();
        MessageConfig configHandler = new PaperMessageConfig(dataFolder, "messages.yml");
        messageData = new MessageData(configHandler);

        if (api != null) {
            try {
                api.shutdown();
            } catch (Exception ignored) {
            }
        }

        PaperCommandExecutor commandExecutor = new PaperCommandExecutor();
        PaperSoundManager soundManager = new PaperSoundManager();
        PaperEconomyManager economyManager = null;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            economyManager = new PaperEconomyManager(this);
            getLogger().info("Vault integration enabled");
        } else {
            getLogger().warning("Vault not found. Economy features disabled.");
        }
        FormSender formSender = new FormSender();
        PaperTitleManager titleManager = new PaperTitleManager();
        PaperPluginManager pluginManager = new PaperPluginManager();
        PaperPlayerManager playerManager = new PaperPlayerManager(this);
        assetServer = new AssetServer(org.bukkit.Bukkit.getIp(),8191, getDataFolder() );
        assetServer.start();

        api = new BedrockGUIApi(new PaperConfig(getDataFolder(), getConfig()), messageData, commandExecutor, soundManager, economyManager, formSender, titleManager, pluginManager, playerManager, new it.pintux.life.paper.platform.PaperScheduler(this));

        formMenuUtil = api.getFormMenuUtil();
        formMenuUtil.setAssetServer(assetServer);
        PaperJavaMenuManager javaMenuManager = new PaperJavaMenuManager(this, messageData);
        getServer().getPluginManager().registerEvents(javaMenuManager, this);
        formMenuUtil.setJavaMenuManager(javaMenuManager);
        playerChecker = new PaperPlayerChecker();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BedrockGUIExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholder features disabled.");
        }
        getLogger().info("Loaded and enabled");

        try {
            org.bukkit.command.SimpleCommandMap commandMap = (org.bukkit.command.SimpleCommandMap)
                    org.bukkit.Bukkit.getServer().getClass().getMethod("getCommandMap").invoke(org.bukkit.Bukkit.getServer());
            if (commandMap == null) return;

            formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
                String formCmd = formMenu.getFormCommand();
                if (formCmd != null && !formCmd.isEmpty()) {
                    String base = formCmd.trim().split("\\s+")[0].toLowerCase();
                    org.bukkit.command.Command formCommand = new org.bukkit.command.Command(base) {
                        @Override
                        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                            return true;
                        }
                    };
                    formCommand.setPermission("bedrockgui.form." + key);
                    commandMap.register("bedrockgui-form", formCommand);
                }
            });
        } catch (Exception e) {
            getLogger().warning("Failed to auto-register form commands: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCmd(ServerCommandEvent event) {
        String command = event.getCommand();

        Player senderPlayer = event.getSender() instanceof Player ? (Player) event.getSender() : null;
        Player targetPlayer = senderPlayer != null ? senderPlayer : findTargetPlayerFromCommand(command);
        if (targetPlayer == null) {
            return;
        }

        if (!playerChecker.isBedrockPlayer(targetPlayer.getUniqueId())) {
            return;
        }

        PaperPlayer paperPlayer = new PaperPlayer(targetPlayer);

        formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
            if (event.isCancelled()) return;

            String intercept = formMenu.getCommandIntercept();
            if (intercept != null && matchesInterceptPattern(command.toLowerCase(), intercept.toLowerCase())) {
                event.setCancelled(true);
                String[] parts = command.split(" ");
                String[] args = Arrays.copyOfRange(parts, 1, parts.length);
                api.openMenu(paperPlayer, key, args);
                return;
            }

            if (formMenu.getFormCommand() != null && command.startsWith(formMenu.getFormCommand())) {
                event.setCancelled(true);
                String[] parts = command.split(" ");
                String[] args = Arrays.copyOfRange(parts, 1, parts.length);
                api.openMenu(paperPlayer, key, args);
            }
        });
    }

    private Player findTargetPlayerFromCommand(String commandLine) {
        if (commandLine == null || commandLine.isEmpty()) return null;
        String[] tokens = commandLine.split(" ");
        for (String token : tokens) {
            Player p = Bukkit.getPlayerExact(token);
            if (p != null) return p;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreprocessCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!playerChecker.isBedrockPlayer(player.getUniqueId())) {
            return;
        }
        String message = event.getMessage();

        String commandWithoutSlash = message.substring(1).toLowerCase();

        String[] parts = commandWithoutSlash.split(" ");
        String commandName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
            if (event.isCancelled()) return;

            String intercept = formMenu.getCommandIntercept();
            if (intercept != null && matchesInterceptPattern(commandWithoutSlash, intercept.toLowerCase())) {
                event.setCancelled(true);
                PaperPlayer player1 = new PaperPlayer(event.getPlayer());
                api.openMenu(player1, key, args);
                return;
            }

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

    private boolean matchesInterceptPattern(String command, String pattern) {
        if (pattern == null || pattern.isEmpty()) return false;
        String p = pattern.trim().toLowerCase();
        boolean leadingWildcard = p.startsWith("%");
        boolean trailingWildcard = p.endsWith("%");

        String core = p;
        if (leadingWildcard) core = core.substring(1).trim();
        if (trailingWildcard) core = core.substring(0, core.length() - 1).trim();

        if (core.isEmpty()) return false;

        String c = command.trim().toLowerCase();
        if (leadingWildcard && trailingWildcard) {
            return c.contains(core);
        } else if (trailingWildcard) {
            return c.startsWith(core);
        } else if (leadingWildcard) {
            return c.endsWith(core);
        } else {
            return c.equals(core);
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

