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
import org.bukkit.event.server.PluginDisableEvent;
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
        boolean freshInstall = !new File(getDataFolder(), "config.yml").exists();
        migrateConfigurations();
        if (freshInstall) {
            int extracted = it.pintux.life.common.utils.DefaultFormsExtractor.extract(getDataFolder(), getLogger()::warning);
            getLogger().info("First run: extracted " + extracted + " default form file(s) to forms/");
        }
        reloadData();
        new Metrics(this, 23364);
    }

    private void migrateConfigurations() {
        it.pintux.life.common.config.ConfigMigrator
                .of(getDataFolder(), "config.yml", () -> getResource("config.yml"),
                        getLogger()::info, getLogger()::warning)
                .preserve("forms")
                .migrate();
        it.pintux.life.common.config.ConfigMigrator
                .of(getDataFolder(), "messages.yml", () -> getResource("messages.yml"),
                        getLogger()::info, getLogger()::warning)
                .migrate();
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
        PaperConfig paperConfig = new PaperConfig(getDataFolder(), getConfig());
        if (assetServer != null) {
            assetServer.shutdown();
        }
        assetServer = AssetServer.fromConfig(paperConfig, org.bukkit.Bukkit.getIp(), 8191, getDataFolder());
        if (assetServer != null) {
            assetServer.start();
        }

        api = new BedrockGUIApi(paperConfig, messageData, commandExecutor, soundManager, economyManager, formSender, titleManager, pluginManager, playerManager, new it.pintux.life.paper.platform.PaperScheduler(this), new it.pintux.life.paper.platform.PaperInventoryManager());

        formMenuUtil = api.getFormMenuUtil();
        formMenuUtil.setAssetServer(assetServer);
        playerChecker = new PaperPlayerChecker();

        it.pintux.life.paper.placeholders.CorePlaceholders.register(api.getPlaceholderRegistry());

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BedrockGUIExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered");
        } else {
            getLogger().info("PlaceholderAPI not found. Built-in placeholders are still served by BedrockGUI.");
        }
        getLogger().info("Loaded and enabled");

        try {
            org.bukkit.command.CommandMap commandMap = Bukkit.getCommandMap();

            formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
                String formCmd = formMenu.getFormCommand();
                if (formCmd != null && !formCmd.isEmpty()) {
                    String base = formCmd.trim().split("\\s+")[0].toLowerCase();
                    org.bukkit.command.Command formCommand = new org.bukkit.command.Command(base) {
                        @Override
                        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                            if (sender instanceof Player player) {
                                api.openMenu(new PaperPlayer(player), key, args);
                            }
                            return true;
                        }
                    };
                    formCommand.setPermission(formMenu.getPermission());
                    commandMap.register("bedrockgui-form", formCommand);
                }
            });
        } catch (Exception e) {
            getLogger().warning("Failed to auto-register form commands: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (api != null && event.getPlugin() != this) {
            api.getActionRegistry().unregisterHandlersFrom(event.getPlugin().getClass().getClassLoader());
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
                String[] args = consoleArgs(command.trim().split("\\s+"),senderPlayer == null ? targetPlayer.getName() : null);
                api.openMenu(paperPlayer, key, args);
                return;
            }

            String[] parts = command.trim().split("\\s+");
            if (formMenu.getFormCommand() != null
                    && parts[0].equalsIgnoreCase(formMenu.getFormCommand().trim().split("\\s+")[0])) {
                event.setCancelled(true);
                String[] args = consoleArgs(parts, senderPlayer == null ? targetPlayer.getName() : null);
                api.openMenu(paperPlayer, key, args);
            }
        });
    }

    private static String[] consoleArgs(String[] parts, String targetName) {
        java.util.List<String> args = new java.util.ArrayList<>(Arrays.asList(parts).subList(1, parts.length));
        if (targetName != null) {
            for (int i = 0; i < args.size(); i++) {
                if (args.get(i).equalsIgnoreCase(targetName)) {
                    args.remove(i);
                    break;
                }
            }
        }
        return args.toArray(new String[0]);
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

        String[] parts = message.substring(1).trim().split("\\s+");
        String commandName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        formMenuUtil.getFormMenus().forEach((key, formMenu) -> {
            if (event.isCancelled()) return;

            String intercept = formMenu.getCommandIntercept();
            if (intercept != null && matchesInterceptPattern(commandWithoutSlash, intercept.toLowerCase())
                    && (formMenu.getPermission() == null || player.hasPermission(formMenu.getPermission()))) {
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
                        event.setCancelled(true);
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
            return c.equals(core) || c.startsWith(core + " ");
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

