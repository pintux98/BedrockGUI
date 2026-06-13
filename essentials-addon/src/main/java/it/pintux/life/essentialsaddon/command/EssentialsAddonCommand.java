package it.pintux.life.essentialsaddon.command;

import it.pintux.life.essentialsaddon.BedrockEssentialsAddonPlugin;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.service.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class EssentialsAddonCommand implements CommandExecutor, TabCompleter {
    private final BedrockEssentialsAddonPlugin plugin;

    public EssentialsAddonCommand(BedrockEssentialsAddonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            send(sender, "&cUsage: /essentialsaddon reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("essentialsaddon.reload")) {
                send(sender, "&cYou don't have permission to reload.");
                return true;
            }

            plugin.reloadConfiguration();

            if (plugin.getWarpCatalogService() != null) plugin.getWarpCatalogService().refresh();
            if (plugin.getKitCatalogService() != null) plugin.getKitCatalogService().refresh();
            if (plugin.getHomeCatalogService() != null) plugin.getHomeCatalogService().refresh();
            if (plugin.getTpaCatalogService() != null) plugin.getTpaCatalogService().refresh();

            send(sender, "&aEssentialsAddon configuration reloaded!");
            return true;
        }

        send(sender, "&cUnknown subcommand: " + args[0]);
        send(sender, "&cUsage: /essentialsaddon reload");
        return true;
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            return completions;
        }
        return List.of();
    }
}
