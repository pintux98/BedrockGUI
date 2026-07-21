package it.pintux.life.homesteadaddon.command;

import it.pintux.life.homesteadaddon.HomesteadAddonPlugin;
import it.pintux.life.homesteadaddon.service.BedrockRegionService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HomesteadAddonCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("reload", "open", "openfor");
    private static final List<String> MENUS = List.of(
            "regions", "region", "info", "players", "flags", "subareas",
            "levels", "rewards", "logs", "misc", "rating",
            "chunks", "mapcolor", "mapicon", "weather", "top", "welcome");

    private final HomesteadAddonPlugin plugin;

    public HomesteadAddonCommand(HomesteadAddonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            send(sender, "&eUsage: /homesteadaddon <reload|open|openfor>");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "open" -> handleOpen(sender, args);
            case "openfor" -> handleOpenFor(sender, args);
            default -> send(sender, "&cUnknown subcommand: " + args[0]);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("homesteadaddon.reload") && !sender.hasPermission("homesteadaddon.admin")) {
            send(sender, "&cYou don't have permission to reload.");
            return;
        }
        plugin.reloadConfiguration();
        send(sender, "&aHomesteadAddon configuration reloaded!");
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!sender.hasPermission("homesteadaddon.admin")) {
            send(sender, "&cYou don't have permission.");
            return;
        }
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly players can open forms. Use openfor <player> <menu>.");
            return;
        }
        if (args.length < 2) {
            send(sender, "&eUsage: /homesteadaddon open <regions|region|info> [regionId]");
            return;
        }
        openMenu(sender, player, args[1], args.length > 2 ? args[2] : null);
    }

    private void handleOpenFor(CommandSender sender, String[] args) {
        if (!sender.hasPermission("homesteadaddon.admin")) {
            send(sender, "&cYou don't have permission.");
            return;
        }
        if (args.length < 3) {
            send(sender, "&eUsage: /homesteadaddon openfor <player> <regions|region|info> [regionId]");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            send(sender, "&cPlayer not found or offline: " + args[1]);
            return;
        }
        openMenu(sender, target, args[2], args.length > 3 ? args[3] : null);
    }

    private void openMenu(CommandSender sender, Player target, String menu, String regionIdArg) {
        BedrockRegionService service = plugin.getRegionService();
        if (service == null) {
            send(sender, "&cHomestead addon is not active.");
            return;
        }
        switch (menu.toLowerCase(Locale.ROOT)) {
            case "regions" -> service.openRegionList(target, false, 1);
            case "region" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null) service.openRegionMenu(target, id);
            }
            case "info" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null) service.openRegionInfo(target, id);
            }
            case "players" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getMemberService() != null) {
                    plugin.getMemberService().openPlayersManagement(target, id);
                }
            }
            case "flags" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getFlagService() != null) {
                    plugin.getFlagService().openFlagsChooser(target, id);
                }
            }
            case "subareas" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getSubAreaService() != null) {
                    plugin.getSubAreaService().openSubAreasList(target, id, 1);
                }
            }
            case "levels" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getLevelService() != null) {
                    plugin.getLevelService().openLevels(target, id);
                }
            }
            case "rewards" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getLevelService() != null) {
                    plugin.getLevelService().openRewards(target, id);
                }
            }
            case "logs" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getLogService() != null) {
                    plugin.getLogService().openLogs(target, id, 1);
                }
            }
            case "misc" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getMiscService() != null) {
                    plugin.getMiscService().openMiscSettings(target, id);
                }
            }
            case "rating" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getMiscService() != null) {
                    plugin.getMiscService().openRating(target, id);
                }
            }
            case "chunks" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getChunkService() != null) {
                    plugin.getChunkService().openClaimedChunks(target, id, 1);
                }
            }
            case "mapcolor" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getChunkService() != null) {
                    plugin.getChunkService().openMapColor(target, id);
                }
            }
            case "mapicon" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getChunkService() != null) {
                    plugin.getChunkService().openMapIcon(target, id);
                }
            }
            case "weather" -> {
                Long id = parseRegionId(sender, regionIdArg);
                if (id != null && plugin.getRegionService() != null) {
                    plugin.getRegionService().openWeatherTime(target, id);
                }
            }
            case "top" -> {
                if (plugin.getRegionService() != null) {
                    plugin.getRegionService().openTopRegions(target, regionIdArg == null ? "BANK" : regionIdArg);
                }
            }
            case "welcome" -> {
                if (plugin.getRegionService() != null) {
                    plugin.getRegionService().openWelcomeSigns(target, 1);
                }
            }
            default -> send(sender, "&cUnknown menu: " + menu);
        }
    }

    private Long parseRegionId(CommandSender sender, String arg) {
        if (arg == null) {
            send(sender, "&cThat menu needs a region id.");
            return null;
        }
        try {
            return Long.parseLong(arg.trim());
        } catch (NumberFormatException e) {
            send(sender, "&cInvalid region id: " + arg);
            return null;
        }
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return prefix(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return prefix(MENUS, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("openfor")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return prefix(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("openfor")) {
            return prefix(MENUS, args[2]);
        }
        return List.of();
    }

    private static List<String> prefix(List<String> options, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
