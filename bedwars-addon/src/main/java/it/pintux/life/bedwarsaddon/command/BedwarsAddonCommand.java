package it.pintux.life.bedwarsaddon.command;

import it.pintux.life.bedwarsaddon.BedrockBedwarsAddonPlugin;
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
                sender.sendMessage("No permission.");
                return true;
            }
            plugin.reloadConfiguration();
            sender.sendMessage("BedwarsAddon configuration reloaded.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("party")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            plugin.openParty(player);
            return true;
        }
        sender.sendMessage("Usage: /bedwarsaddon <reload|party>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("reload", "party");
        return List.of();
    }
}
