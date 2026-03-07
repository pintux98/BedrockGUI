package it.pintux.life.paper;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.AssetServer;
import it.pintux.life.common.utils.FormSender;
import it.pintux.life.paper.placeholders.BedrockGUIExpansion;
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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public final class BedrockGUI extends JavaPlugin implements Listener {

    private FormMenuUtil formMenuUtil;
    private MessageData messageData;
    private BedrockGUIApi api;
    private java.util.Set<String> interceptBaseLabels = new java.util.HashSet<>();
    private AssetServer assetServer;


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
        if (assetServer != null) {
            assetServer.shutdown();
            assetServer = null;
        }
        getLogger().info("BedrockGUI disabled");
    }

    public void reloadData() {
        reloadConfig();
        this.saveResource("messages.yml", false);

        File dataFolder = getDataFolder();
        MessageConfig configHandler = new PaperMessageConfig(dataFolder, "messages.yml");
        messageData = new MessageData(configHandler);

        if (api != null) {
            getLogger().info("Reloading BedrockGUI with fresh instances to avoid stale cache...");
            try {
                api.shutdown();
            } catch (Exception ex) {
                getLogger().warning("Error shutting down API during reload: " + ex.getMessage());
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

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "bedrockgui:cmd", new it.pintux.life.paper.platform.CommandBridgeListener());

        api = new BedrockGUIApi(new PaperConfig(getDataFolder(), getConfig()), messageData, commandExecutor, soundManager, economyManager, formSender, titleManager, pluginManager, playerManager, new it.pintux.life.paper.platform.PaperScheduler(this));

        formMenuUtil = api.getFormMenuUtil();
        formMenuUtil.setAssetServer(assetServer);
        it.pintux.life.paper.platform.PaperJavaMenuManager javaMenuManager = new it.pintux.life.paper.platform.PaperJavaMenuManager(this, messageData);
        getServer().getPluginManager().registerEvents(javaMenuManager, this);
        formMenuUtil.setJavaMenuManager(javaMenuManager);
        getLogger().info("Using FormMenuUtil from BedrockGUIApi");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BedrockGUIExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholder features disabled.");
        }
        getLogger().info("BedrockGUI loaded and enabled");

        wrapKnownCommands();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCmd(ServerCommandEvent event) {
        String command = event.getCommand();
        PaperPlayerChecker playerChecker = new PaperPlayerChecker();

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

    private void wrapKnownCommands() {
        try {
            interceptBaseLabels = computeInterceptBaseLabels();
            Object craftServer = Bukkit.getServer();
            java.lang.reflect.Field mapField = craftServer.getClass().getDeclaredField("commandMap");
            mapField.setAccessible(true);
            org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) mapField.get(craftServer);

            java.lang.reflect.Field knownField = org.bukkit.command.SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, org.bukkit.command.Command> known = (java.util.Map<String, org.bukkit.command.Command>) knownField.get(commandMap);

            java.util.Set<String> excluded = new java.util.HashSet<>(java.util.Arrays.asList(
                    "plugman", "help", "?", "ver", "version", "plugins",
                    "bukkit:help", "bukkit:?", "bukkit:ver", "bukkit:version", "bukkit:plugins",
                    "bedrockgui", "bgui"
            ));

            int wrapped = 0;
            int unwrapped = 0;
            java.util.Map<org.bukkit.command.Command, InterceptingCommand> created = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, org.bukkit.command.Command> e : new java.util.ArrayList<>(known.entrySet())) {
                org.bukkit.command.Command cmd = e.getValue();
                if (cmd == null) continue;
                if (cmd instanceof InterceptingCommand) {
                    InterceptingCommand ic = (InterceptingCommand) cmd;
                    org.bukkit.command.Command orig = ic.getOriginal();
                    String key = e.getKey();
                    String name = orig.getName();
                    String className = orig.getClass().getName();
                    if (key != null && (key.toLowerCase().startsWith("minecraft:") || key.toLowerCase().startsWith("bukkit:")))
                        continue;
                    if (className.startsWith("org.bukkit.command.defaults")) continue;
                    if (name != null && excluded.contains(name.toLowerCase())) continue;
                    if (key != null && excluded.contains(key.toLowerCase())) continue;
                    boolean match = false;
                    if (name != null && interceptBaseLabels.contains(name.toLowerCase())) match = true;
                    if (!match && key != null) {
                        String k = key.toLowerCase();
                        int idx = k.lastIndexOf(":");
                        String tail = idx >= 0 ? k.substring(idx + 1) : k;
                        if (interceptBaseLabels.contains(tail)) match = true;
                    }
                    if (!match) {
                        known.put(e.getKey(), orig);
                        unwrapped++;
                    }
                    continue;
                }
                String key = e.getKey();
                String name = cmd.getName();
                String className = cmd.getClass().getName();
                if (key != null && (key.toLowerCase().startsWith("minecraft:") || key.toLowerCase().startsWith("bukkit:")))
                    continue;
                if (className.startsWith("org.bukkit.command.defaults")) continue;
                if (name != null && excluded.contains(name.toLowerCase())) continue;
                if (key != null && excluded.contains(key.toLowerCase())) continue;
                if (cmd instanceof org.bukkit.command.PluginIdentifiableCommand) {
                    org.bukkit.plugin.Plugin owner = ((org.bukkit.command.PluginIdentifiableCommand) cmd).getPlugin();
                    if (owner == this) continue;
                    if (owner != null && !owner.isEnabled()) continue;
                }
                boolean match = false;
                if (name != null && interceptBaseLabels.contains(name.toLowerCase())) match = true;
                if (!match && key != null) {
                    String k = key.toLowerCase();
                    int idx = k.lastIndexOf(":");
                    String tail = idx >= 0 ? k.substring(idx + 1) : k;
                    if (interceptBaseLabels.contains(tail)) match = true;
                }
                if (!match) continue;
                InterceptingCommand wrapper = created.computeIfAbsent(cmd, o -> new InterceptingCommand(o, this));
                known.put(e.getKey(), wrapper);
                wrapped++;
            }
            if (wrapped > 0 || unwrapped > 0) {
                getLogger().info("Command wrap refresh: wrapped=" + wrapped + ", unwrapped=" + unwrapped + " (labels=" + interceptBaseLabels.size() + ")");
            } else {
                if (interceptBaseLabels.isEmpty()) {
                    getLogger().warning("No base labels derived from config; wildcard-only patterns won't be programmatically intercepted.");
                } else {
                    getLogger().fine("No new commands matched cached base labels at this stage");
                }
            }
        } catch (Throwable t) {
            getLogger().warning("Failed to wrap known commands for interception: " + t.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        wrapKnownCommands();
    }

    private java.util.Set<String> computeInterceptBaseLabels() {
        java.util.Set<String> labels = new java.util.HashSet<>();
        try {
            FormMenuUtil fmu = getFormMenuUtil();
            if (fmu != null) {
                fmu.getFormMenus().forEach((key, formMenu) -> {
                    String formCommand = formMenu.getFormCommand();
                    if (formCommand != null && !formCommand.isEmpty()) {
                        String[] parts = formCommand.trim().toLowerCase().split(" ");
                        if (parts.length > 0) labels.add(parts[0]);
                    }
                    String intercept = formMenu.getCommandIntercept();
                    if (intercept != null && !intercept.isEmpty()) {
                        String p = intercept.trim().toLowerCase();
                        if (!p.startsWith("%")) {
                            String[] tokens = p.split(" ");
                            if (tokens.length > 0) labels.add(tokens[0]);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            getLogger().warning("Failed to compute intercept base labels: " + t.getMessage());
        }
        return labels;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        wrapKnownCommands();
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

