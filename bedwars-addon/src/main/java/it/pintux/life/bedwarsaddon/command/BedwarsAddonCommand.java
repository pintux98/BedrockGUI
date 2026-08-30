package it.pintux.life.bedwarsaddon.command;

import it.pintux.life.bedwarsaddon.BedrockBedwarsAddonPlugin;
import it.pintux.life.bedwarsaddon.util.AddonText;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class BedwarsAddonCommand implements CommandExecutor, TabCompleter {
    private final BedrockBedwarsAddonPlugin plugin;

    public BedwarsAddonCommand(BedrockBedwarsAddonPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bedwarsaddon.admin")) {
                AddonText.send(sender, plugin.getConfiguration().commandNoPermission());
                return true;
            }
            plugin.reloadConfiguration();
            AddonText.send(sender, plugin.getConfiguration().commandReloaded());
            return true;
        }
        if (args.length == 1 && isPlayerForm(args[0])) {
            if (!(sender instanceof Player player)) {
                AddonText.send(sender, plugin.getConfiguration().commandPlayersOnly());
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "party" -> plugin.openParty(player);
                case "arena" -> plugin.openArena(player);
                case "stats" -> plugin.openStats(player);
                case "spectator" -> plugin.openSpectator(player);
            }
            return true;
        }
        AddonText.send(sender, plugin.getConfiguration().commandUsage());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("reload", "party", "arena", "stats", "spectator");
        return List.of();
    }

    private static boolean isPlayerForm(String arg) {
        return arg.equalsIgnoreCase("party") || arg.equalsIgnoreCase("arena")
                || arg.equalsIgnoreCase("stats") || arg.equalsIgnoreCase("spectator");
    }
}
