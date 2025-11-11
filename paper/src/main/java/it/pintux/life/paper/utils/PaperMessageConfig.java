package it.pintux.life.paper.utils;

import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageConfig;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bukkit.Bukkit.getServer;

public class PaperMessageConfig implements MessageConfig {

    private final Pattern hexAnglePattern = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private final Pattern hexAmpPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private final FileConfiguration config;

    public PaperMessageConfig(File dataFolder, String filename) {
        File file = new File(dataFolder, filename);
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public String getString(String path) {
        return config.getString(path);
    }

    @Override
    public String setPlaceholders(FormPlayer player, String message) {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Player bukkitPlayer = player == null ? null : Bukkit.getPlayer(player.getUniqueId());
            return PlaceholderAPI.setPlaceholders(bukkitPlayer, message);
        }
        return message;
    }

    @Override
    public String applyColor(String message) {
        if (message == null) return null;

        // Support both "<#RRGGBB>" and "&#RRGGBB" styles by inserting Bungee hex color
        Matcher angleMatcher = hexAnglePattern.matcher(message);
        while (angleMatcher.find()) {
            final ChatColor hexColor = ChatColor.of(angleMatcher.group().substring(1, angleMatcher.group().length() - 1));
            final String before = message.substring(0, angleMatcher.start());
            final String after = message.substring(angleMatcher.end());
            message = before + hexColor + after;
            angleMatcher = hexAnglePattern.matcher(message);
        }

        Matcher ampMatcher = hexAmpPattern.matcher(message);
        while (ampMatcher.find()) {
            final ChatColor hexColor = ChatColor.of("#" + ampMatcher.group(1));
            final String before = message.substring(0, ampMatcher.start());
            final String after = message.substring(ampMatcher.end());
            message = before + hexColor + after;
            ampMatcher = hexAmpPattern.matcher(message);
        }

        // Translate '&' legacy codes and '&x' hex sequences into '§' variants
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
