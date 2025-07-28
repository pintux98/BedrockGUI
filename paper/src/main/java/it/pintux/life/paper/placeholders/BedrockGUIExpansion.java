package it.pintux.life.paper.placeholders;

import it.pintux.life.paper.BedrockGUI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

/**
 * PlaceholderAPI expansion for BedrockGUI custom placeholders
 */
public class BedrockGUIExpansion extends PlaceholderExpansion {
    
    private final BedrockGUI plugin;
    
    public BedrockGUIExpansion(BedrockGUI plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "bgui";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "pintux";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        switch (params.toLowerCase()) {
            case "online_players_list":
                return getOnlinePlayersList();
            case "online_players_size":
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            default:
                return null;
        }
    }
    
    /**
     * Returns a formatted list of online players with their names and UUIDs
     * Format: "name1:uuid1,name2:uuid2,name3:uuid3"
     */
    private String getOnlinePlayersList() {
        return Bukkit.getOnlinePlayers().stream()
                .map(p -> p.getName() + ":" + p.getUniqueId().toString())
                .collect(Collectors.joining(","));
    }
}