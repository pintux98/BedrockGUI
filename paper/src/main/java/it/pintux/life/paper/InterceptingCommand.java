package it.pintux.life.paper;

import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.paper.platform.PaperPlayerChecker;
import it.pintux.life.paper.utils.PaperPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps a Command to intercept execution for Bedrock players based on command_intercept patterns.
 */
public class InterceptingCommand extends Command implements PluginIdentifiableCommand {
    private final Command original;
    private final BedrockGUI plugin;
    private static final ThreadLocal<Boolean> inIntercept = ThreadLocal.withInitial(() -> false);
    private static final Map<String, Long> recentIntercepts = new ConcurrentHashMap<>();
    private static final long INTERCEPT_COOLDOWN_MS = 1000L; // prevent repeated intercept loops across ticks
    private static final ThreadLocal<java.util.HashMap<String, Integer>> execDepth = ThreadLocal.withInitial(java.util.HashMap::new);
    private static final class ExecState { int count; long windowStart; long disabledUntil; }
    private static final ThreadLocal<ExecState> execState = ThreadLocal.withInitial(ExecState::new);

    public InterceptingCommand(Command original, BedrockGUI plugin) {
        super(original.getName());
        this.original = original;
        this.plugin = plugin;
        setDescription(original.getDescription());
        setUsage(original.getUsage());
        setAliases(original.getAliases());
        setPermission(original.getPermission());
        setPermissionMessage(original.getPermissionMessage());
        permissionMessage(original.permissionMessage());
    }

    public Command getOriginal() {
        return original;
    }

    @Override
    public Plugin getPlugin() {
        if (original instanceof PluginIdentifiableCommand) {
            return ((PluginIdentifiableCommand) original).getPlugin();
        }
        return plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (original instanceof PluginCommand) {
            PluginCommand pc = (PluginCommand) original;
            if (pc.getPlugin() == plugin) {
                return original.execute(sender, commandLabel, args);
            }
        }

        if (Boolean.TRUE.equals(inIntercept.get())) {
            return original.execute(sender, commandLabel, args);
        }
        inIntercept.set(true);
        try {
            String full = buildCommandLine(commandLabel, args).toLowerCase();
            ExecState state = execState.get();
            long now = System.currentTimeMillis();
            if (state.disabledUntil > now) {
                try {
                    return original.execute(sender, commandLabel, args);
                } catch (Throwable th) {
                    plugin.getLogger().warning("Bypass execution failed for '" + commandLabel + "': " + th.getMessage());
                    return false;
                }
            }
            if (state.windowStart == 0L || (now - state.windowStart) > 1000L) {
                state.windowStart = now;
                state.count = 0;
            }
            state.count++;
            if (state.count > 2048) {
                plugin.getLogger().warning("Temporarily disabling interception due to high dispatch volume in thread (" + state.count + ")");
                state.disabledUntil = now + 2000L;
                try {
                    return original.execute(sender, commandLabel, args);
                } catch (Throwable th) {
                    plugin.getLogger().warning("Throttled execution failed for '" + commandLabel + "': " + th.getMessage());
                    return false;
                }
            }

            java.util.HashMap<String, Integer> depthMap = execDepth.get();
            int depth = depthMap.getOrDefault(full, 0) + 1;
            depthMap.put(full, depth);
            if (depth > 64) {
                plugin.getLogger().warning("Breaking potential command recursion: '" + full + "' sender=" + sender.getClass().getSimpleName());
                return true;
            }

            String senderKey = getSenderKey(sender);
            String loopKey = senderKey + "|" + full;
            Long last = recentIntercepts.get(loopKey);
            if (last != null && (now - last) < INTERCEPT_COOLDOWN_MS) {
                try {
                    return original.execute(sender, commandLabel, args);
                } catch (Throwable th) {
                    plugin.getLogger().warning("Cooldown execution failed for '" + full + "': " + th.getMessage());
                    return false;
                }
            }

            FormMenuUtil formMenuUtil = plugin.getFormMenuUtil();
            if (formMenuUtil != null) {
                String[] cmdArgs = args != null ? args : new String[0];
                for (Map.Entry<String, it.pintux.life.common.form.obj.FormMenu> entry : formMenuUtil.getFormMenus().entrySet()) {
                    String key = entry.getKey();
                    it.pintux.life.common.form.obj.FormMenu formMenu = entry.getValue();

                    String intercept = formMenu.getCommandIntercept();
                    boolean interceptMatch = intercept != null && matchesInterceptPattern(full, intercept.toLowerCase());

                    String formCommand = formMenu.getFormCommand();
                    boolean formMatch = formCommand != null && full.startsWith(formCommand.toLowerCase());

                    if (interceptMatch || formMatch) {
                        Player target = resolveTarget(sender, full);
                        if (target != null) {
                            PaperPlayerChecker checker = new PaperPlayerChecker();
                            if (checker.isBedrockPlayer(target.getUniqueId())) {
                                if (interceptMatch) {
                                    plugin.getLogger().info(
                                            "Intercepted command (programmatic) for Bedrock player: sender="
                                                    + sender.getClass().getSimpleName()
                                                    + ", target=" + target.getName()
                                                    + ", command='" + full + "'"
                                                    + ", menu='" + key + "'"
                                    );
                                } else {
                                    plugin.getLogger().info(
                                            "Intercepted command (programmatic) for Bedrock player via formCommand: sender="
                                                    + sender.getClass().getSimpleName()
                                                    + ", target=" + target.getName()
                                                    + ", command='" + full + "'"
                                                    + ", menu='" + key + "'"
                                    );
                                }
                                try {
                                    plugin.getApi().openMenu(new PaperPlayer(target), key, cmdArgs);
                                } catch (Throwable th) {
                                    plugin.getLogger().warning("openMenu failed for '" + full + "' menu='" + key + "': " + th.getMessage());
                                    recentIntercepts.put(loopKey, now);
                                    return true;
                                }
                                recentIntercepts.put(loopKey, now);
                                return true;
                            }
                        }
                    }
                }
            }

            try {
                if (original instanceof PluginIdentifiableCommand) {
                    Plugin owner = ((PluginIdentifiableCommand) original).getPlugin();
                    if (owner != null && !owner.isEnabled()) {
                        plugin.getLogger().warning("Original plugin disabled for command '" + full + "', skipping execution");
                        return false;
                    }
                }
                return original.execute(sender, commandLabel, args);
            } catch (Throwable th) {
                plugin.getLogger().warning("Original execution failed for '" + full + "': " + th.getMessage());
                return false;
            }
        } finally {
            inIntercept.set(false);
            String full = buildCommandLine(commandLabel, args).toLowerCase();
            java.util.HashMap<String, Integer> depthMap = execDepth.get();
            Integer depth = depthMap.get(full);
            if (depth != null) {
                if (depth <= 1) {
                    depthMap.remove(full);
                } else {
                    depthMap.put(full, depth - 1);
                }
            }
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        return original.tabComplete(sender, alias, args);
    }

    @Override
    public boolean testPermission(@NotNull CommandSender target) {
        return original.testPermission(target);
    }

    @Override
    public boolean testPermissionSilent(@NotNull CommandSender target) {
        return original.testPermissionSilent(target);
    }

    @Override
    public String getPermission() {
        return original.getPermission();
    }

    private String buildCommandLine(String label, String[] args) {
        if (args == null || args.length == 0) return label;
        return label + " " + String.join(" ", args);
    }

    private String getSenderKey(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId().toString();
        }
        return sender.getClass().getName();
    }

    private Player resolveTarget(CommandSender sender, String commandLine) {
        if (sender instanceof Player) return (Player) sender;
        String[] tokens = commandLine.split(" ");
        for (String token : tokens) {
            Player p = Bukkit.getPlayerExact(token);
            if (p != null) return p;
        }
        return null;
    }

    private boolean matchesInterceptPattern(String command, String pattern) {
        String p = pattern.trim();
        boolean startsWithWildcard = p.startsWith("%");
        boolean endsWithWildcard = p.endsWith("%");
        String core = p.replaceAll("^%|%$", "").trim();

        if (startsWithWildcard && endsWithWildcard) {
            return command.contains(core);
        } else if (startsWithWildcard) {
            return command.endsWith(core);
        } else if (endsWithWildcard) {
            return command.startsWith(core);
        } else {
            return command.equals(core);
        }
    }
}