package it.pintux.life.paper;

import it.pintux.life.common.api.BedrockGUIApi;
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
        if (DependencyValidator.isPluginCompatible("Vault", "1.7.0")) {
            economyManager = new PaperEconomyManager(this);
            getLogger().info("Vault integration enabled");
        } else if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            getLogger().warning("Vault found but version is incompatible. Economy features disabled.");
        }
        PaperFormSender formSender = new PaperFormSender();
        PaperTitleManager titleManager = new PaperTitleManager();
        PaperPluginManager pluginManager = new PaperPluginManager();
        PaperPlayerManager playerManager = new PaperPlayerManager();

        api = new BedrockGUIApi(new PaperConfig(getConfig()), messageData, commandExecutor, soundManager, economyManager, formSender, titleManager, pluginManager, playerManager);

        formMenuUtil = api.getFormMenuUtil();
        getLogger().info("Using FormMenuUtil from BedrockGUIApi");

        if (DependencyValidator.isPluginCompatible("PlaceholderAPI", "2.10.0")) {
            new BedrockGUIExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered");
        } else if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().warning("PlaceholderAPI found but version is incompatible. Placeholder features disabled.");
        }


        getLogger().info("BedrockGUI loaded and enabled");

        // Initial wrap; more wraps are triggered on ServerLoad/PluginEnable to catch late registrations
        wrapKnownCommands();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCmd(ServerCommandEvent event) {
        String command = event.getCommand();
        PaperPlayerChecker playerChecker = new PaperPlayerChecker();

        // Use sender if it is a player; otherwise try to find a target player mentioned in the command line
        Player senderPlayer = event.getSender() instanceof Player ? (Player) event.getSender() : null;
        Player targetPlayer = senderPlayer != null ? senderPlayer : findTargetPlayerFromCommand(command);
        if (targetPlayer == null) {
            return;
        }

        // Only intercept for Bedrock players
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

    // Re-wrap known commands after plugins finish enabling and on demand
    private void wrapKnownCommands() {
        try {
            // Compute base labels from config (formCommand + explicit intercept patterns)
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
                    // common administrative commands that may re-dispatch internally
                    "plugman", "help", "?", "ver", "version", "plugins",
                    // bukkit-namespaced variants
                    "bukkit:help", "bukkit:?", "bukkit:ver", "bukkit:version", "bukkit:plugins",
                    // our plugin command
                    "bedrockgui"
            ));

            int wrapped = 0;
            int unwrapped = 0;
            java.util.Map<org.bukkit.command.Command, InterceptingCommand> created = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, org.bukkit.command.Command> e : new java.util.ArrayList<>(known.entrySet())) {
                org.bukkit.command.Command cmd = e.getValue();
                if (cmd == null) continue;
                // If already wrapped, decide if it still matches; if not, unwrap
                if (cmd instanceof InterceptingCommand) {
                    InterceptingCommand ic = (InterceptingCommand) cmd;
                    org.bukkit.command.Command orig = ic.getOriginal();
                    String key = e.getKey();
                    String name = orig.getName();
                    String className = orig.getClass().getName();
                    if (key != null && (key.toLowerCase().startsWith("minecraft:") || key.toLowerCase().startsWith("bukkit:"))) continue;
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
                // Skip core/default commands and namespaced Minecraft/Bukkit commands
                if (key != null && (key.toLowerCase().startsWith("minecraft:") || key.toLowerCase().startsWith("bukkit:"))) continue;
                if (className.startsWith("org.bukkit.command.defaults")) continue;
                if (name != null && excluded.contains(name.toLowerCase())) continue;
                if (key != null && excluded.contains(key.toLowerCase())) continue;
                // Skip wrapping commands owned by our plugin (redundant) or disabled plugins
                if (cmd instanceof org.bukkit.command.PluginIdentifiableCommand) {
                    org.bukkit.plugin.Plugin owner = ((org.bukkit.command.PluginIdentifiableCommand) cmd).getPlugin();
                    if (owner == this) continue;
                    if (owner != null && !owner.isEnabled()) continue;
                }
                // Wrap only commands whose base label matches our cached set
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
        // Ensure wrapping after the server finishes loading all plugins/commands
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
                        // Only include explicit base labels (no leading wildcard)
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
        // Some plugins register commands during enable; re-wrap to keep interception intact
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

