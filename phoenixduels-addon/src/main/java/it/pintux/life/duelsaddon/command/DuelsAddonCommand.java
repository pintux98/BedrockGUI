package it.pintux.life.duelsaddon.command;

import it.pintux.life.duelsaddon.DuelsAddonPlugin;
import it.pintux.life.duelsaddon.listener.DuelsMenus;
import it.pintux.life.duelsaddon.listener.MenuInterceptListener;
import it.pintux.life.duelsaddon.listener.PhoenixMenuResolver;
import it.pintux.life.duelsaddon.model.StatsKind;
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
import java.util.TreeSet;

/**
 * {@code /duelsaddon} — reload, inspect the menu routing table, and open any form on demand.
 *
 * <p>{@code openfor} exists because these forms are otherwise only reachable by triggering
 * PhoenixDuels itself, which makes testing a single form awkward. {@code menus} prints which ids
 * are being replaced and which fall through, which is the fastest way to see whether a
 * PhoenixDuels update renamed something.</p>
 */
public final class DuelsAddonCommand implements CommandExecutor, TabCompleter {
    private static final List<String> ROOTS = List.of("reload", "open", "openfor", "menus");
    private static final List<String> FORMS = List.of(
            "queue", "duel", "party", "party_info", "party_invite", "settings",
            "stats", "leaderboard", "matches", "kits", "lost_items");

    private final DuelsAddonPlugin plugin;

    public DuelsAddonCommand(DuelsAddonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            usage(sender, label);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfiguration();
                send(sender, "&aPhoenixDuels addon reloaded.");
            }
            case "menus" -> menus(sender);
            case "open" -> {
                if (!(sender instanceof Player player)) {
                    send(sender, "&cRun this in game, or use openfor <player> <form>.");
                    return true;
                }
                if (args.length < 2) {
                    send(sender, "&cUsage: /" + label + " open <form>");
                    return true;
                }
                open(sender, player, args[1]);
            }
            case "openfor" -> {
                if (args.length < 3) {
                    send(sender, "&cUsage: /" + label + " openfor <player> <form>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    send(sender, "&cThat player is not online.");
                    return true;
                }
                open(sender, target, args[2]);
            }
            default -> usage(sender, label);
        }
        return true;
    }

    private void menus(CommandSender sender) {
        send(sender, "&bPhoenixDuels: &f" + (plugin.getGateway().isAvailable()
                ? "hooked (" + plugin.getGateway().edition() + ")" : "&cunavailable"));
        MenuInterceptListener listener = plugin.getMenuInterceptListener();
        if (listener == null) {
            send(sender, "&cInterception is disabled.");
            return;
        }
        PhoenixMenuResolver resolver = listener.resolver();
        int resolved = resolver.refresh();
        send(sender, (resolved == 0 ? "&cRegistry keys resolved: &f" : "&aRegistry keys resolved: &f")
                + resolved + "&7/&f" + DuelsMenus.ALL.size()
                + (resolved == 0 ? " &8(interception is doing nothing)" : ""));
        send(sender, "&aResolved: &7" + String.join(", ", resolver.resolvedKeys()));
        send(sender, "&cNot in registry (&f" + resolver.unresolvedKeys().size() + "&c): &7"
                + String.join(", ", resolver.unresolvedKeys()));
        if (!resolver.collisions().isEmpty()) {
            send(sender, "&6Keys sharing one layout: &7" + String.join(", ", resolver.collisions()));
        }
        List<String> live = resolver.liveRegistryKeys();
        send(sender, "&bLive registry keys (&f" + live.size() + "&b): &7"
                + (live.isEmpty() ? "unreadable" : String.join(", ", live)));
        send(sender, "&aServed as Bedrock forms (&f" + listener.handlers().size() + "&a): &7"
                + String.join(", ", new TreeSet<>(listener.handlers().keySet())));
        send(sender, "&eJava-only by design: &7" + String.join(", ", new TreeSet<>(DuelsMenus.JAVA_ONLY)));
    }

    private void open(CommandSender sender, Player target, String form) {
        switch (form.toLowerCase(Locale.ROOT)) {
            case "queue" -> plugin.getQueueService().openMain(target);
            case "duel" -> plugin.getDuelService().openTargetPicker(target);
            case "party" -> plugin.getPartyService().openMain(target);
            case "party_info" -> plugin.getPartyService().openInfo(target, 1);
            case "party_invite" -> plugin.getPartyService().openInvitePicker(target);
            case "settings" -> plugin.getSettingsService().openSettings(target);
            case "stats" -> plugin.getStatsService().openMain(target);
            case "leaderboard" -> plugin.getStatsService()
                    .openLeaderboard(target, StatsKind.UNRANKED, "wins", 1);
            case "matches" -> plugin.getSpectatorService().openMatches(target, 1);
            case "kits" -> plugin.getKitService().openKitList(target);
            case "lost_items" -> plugin.getDuelService().openLostItems(target);
            default -> {
                send(sender, "&cUnknown form. Available: &7" + String.join(", ", FORMS));
                return;
            }
        }
        send(sender, "&aOpened &f" + form.toLowerCase(Locale.ROOT) + "&a for &f" + target.getName() + "&a.");
    }

    private void usage(CommandSender sender, String label) {
        send(sender, "&b/" + label + " reload &8- &7reload config.yml");
        send(sender, "&b/" + label + " menus &8- &7list which menus are served as forms");
        send(sender, "&b/" + label + " open <form> &8- &7open a form for yourself");
        send(sender, "&b/" + label + " openfor <player> <form> &8- &7open a form for someone");
    }

    private static void send(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(ROOTS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return partial(FORMS, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("openfor")) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return partial(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("openfor")) {
            return partial(FORMS, args[2]);
        }
        return List.of();
    }

    private static List<String> partial(List<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}
