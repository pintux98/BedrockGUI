package it.pintux.life.paper.utils;

import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.LegacyColors;
import it.pintux.life.common.utils.MessageConfig;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

import static org.bukkit.Bukkit.getServer;

public class PaperMessageConfig implements MessageConfig {

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
        return LegacyColors.translate(message);
    }
}
