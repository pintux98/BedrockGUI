package it.pintux.life.paper.data;

import it.pintux.life.common.data.DataProvider;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.paper.utils.PaperPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.logging.Logger;

/**
 * Paper/Bukkit implementation of DataProvider
 * Provides real server data for the List action handler
 */
public class PaperDataProvider implements DataProvider {
    
    private static final Logger logger = Logger.getLogger(PaperDataProvider.class.getName());
    
    @Override
    public List<Map<String, String>> getOnlinePlayersData() {
        List<Map<String, String>> players = new ArrayList<>();
        
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Map<String, String> playerData = new HashMap<>();
                playerData.put("name", player.getName());
                playerData.put("uuid", player.getUniqueId().toString());
                playerData.put("displayname", player.getDisplayName());
                playerData.put("world", player.getWorld().getName());
                playerData.put("gamemode", player.getGameMode().name());
                playerData.put("health", String.format("%.1f/%.1f", player.getHealth(), player.getMaxHealth()));
                playerData.put("food", String.valueOf(player.getFoodLevel()));
                playerData.put("level", String.valueOf(player.getLevel()));
                playerData.put("exp", String.format("%.2f", player.getExp()));
                playerData.put("ping", String.valueOf(player.getPing()));
                playerData.put("ip", player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "Unknown");
                playerData.put("description", "Online Player");
                players.add(playerData);
            }
        } catch (Exception e) {
            logger.warning("Error getting online players data: " + e.getMessage());
        }
        
        return players;
    }
    
    @Override
    public List<Map<String, String>> getServerInfoData() {
        List<Map<String, String>> serverInfo = new ArrayList<>();
        
        try {
            // Server basic info
            Map<String, String> basicInfo = new HashMap<>();
            basicInfo.put("name", "Server Version");
            basicInfo.put("value", Bukkit.getVersion());
            basicInfo.put("description", "Server Software Version");
            serverInfo.add(basicInfo);
            
            // Bukkit version
            Map<String, String> bukkitInfo = new HashMap<>();
            bukkitInfo.put("name", "Bukkit Version");
            bukkitInfo.put("value", Bukkit.getBukkitVersion());
            bukkitInfo.put("description", "Bukkit API Version");
            serverInfo.add(bukkitInfo);
            
            // Online players
            Map<String, String> playersInfo = new HashMap<>();
            playersInfo.put("name", "Players Online");
            playersInfo.put("value", Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
            playersInfo.put("description", "Current/Maximum Players");
            serverInfo.add(playersInfo);
            
            // Worlds count
            Map<String, String> worldsInfo = new HashMap<>();
            worldsInfo.put("name", "Worlds Loaded");
            worldsInfo.put("value", String.valueOf(Bukkit.getWorlds().size()));
            worldsInfo.put("description", "Number of Loaded Worlds");
            serverInfo.add(worldsInfo);
            
            // Plugins count
            Map<String, String> pluginsInfo = new HashMap<>();
            pluginsInfo.put("name", "Plugins Loaded");
            pluginsInfo.put("value", String.valueOf(Bukkit.getPluginManager().getPlugins().length));
            pluginsInfo.put("description", "Number of Loaded Plugins");
            serverInfo.add(pluginsInfo);
            
            // Memory usage
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            Map<String, String> memoryInfo = new HashMap<>();
            memoryInfo.put("name", "Memory Usage");
            memoryInfo.put("value", formatBytes(usedMemory) + "/" + formatBytes(maxMemory));
            memoryInfo.put("description", "Used/Maximum Memory");
            serverInfo.add(memoryInfo);
            
            // Server uptime
            long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
            Map<String, String> uptimeInfo = new HashMap<>();
            uptimeInfo.put("name", "Server Uptime");
            uptimeInfo.put("value", formatUptime(uptimeMillis));
            uptimeInfo.put("description", "Time Since Server Start");
            serverInfo.add(uptimeInfo);
            
            // TPS (if available)
            try {
                double[] tps = Bukkit.getTPS();
                if (tps.length > 0) {
                    Map<String, String> tpsInfo = new HashMap<>();
                    tpsInfo.put("name", "TPS (1m/5m/15m)");
                    tpsInfo.put("value", String.format("%.2f/%.2f/%.2f", 
                        Math.min(tps[0], 20.0), 
                        Math.min(tps[1], 20.0), 
                        Math.min(tps[2], 20.0)));
                    tpsInfo.put("description", "Server Ticks Per Second");
                    serverInfo.add(tpsInfo);
                }
            } catch (Exception e) {
                // TPS not available on this server version
            }
            
        } catch (Exception e) {
            logger.warning("Error getting server info data: " + e.getMessage());
        }
        
        return serverInfo;
    }
    
    @Override
    public List<Map<String, String>> getPlayerPermissionsData(FormPlayer formPlayer) {
        List<Map<String, String>> permissions = new ArrayList<>();
        
        try {
            if (formPlayer instanceof PaperPlayer) {
                Player player = ((PaperPlayer) formPlayer).getBukkitPlayer();
                
                // Get effective permissions
                Set<PermissionAttachmentInfo> effectivePermissions = player.getEffectivePermissions();
                
                for (PermissionAttachmentInfo permInfo : effectivePermissions) {
                    Map<String, String> permData = new HashMap<>();
                    permData.put("name", permInfo.getPermission());
                    permData.put("value", String.valueOf(permInfo.getValue()));
                    permData.put("description", "Permission: " + (permInfo.getValue() ? "Granted" : "Denied"));
                    permissions.add(permData);
                }
                
                // Sort by permission name
                permissions.sort((a, b) -> a.get("name").compareToIgnoreCase(b.get("name")));
            }
        } catch (Exception e) {
            logger.warning("Error getting player permissions data: " + e.getMessage());
        }
        
        return permissions;
    }
    
    @Override
    public List<Map<String, String>> getWorldsData() {
        List<Map<String, String>> worlds = new ArrayList<>();
        
        try {
            for (World world : Bukkit.getWorlds()) {
                Map<String, String> worldData = new HashMap<>();
                worldData.put("name", world.getName());
                worldData.put("environment", world.getEnvironment().name());
                worldData.put("players", String.valueOf(world.getPlayers().size()));
                worldData.put("difficulty", world.getDifficulty().name());
                worldData.put("time", String.valueOf(world.getTime()));
                worldData.put("weather", world.hasStorm() ? "Storm" : "Clear");
                worldData.put("pvp", String.valueOf(world.getPVP()));
                worldData.put("spawn", world.getSpawnLocation().getBlockX() + "," + 
                                     world.getSpawnLocation().getBlockY() + "," + 
                                     world.getSpawnLocation().getBlockZ());
                worldData.put("description", "World Environment: " + world.getEnvironment().name());
                worlds.add(worldData);
            }
        } catch (Exception e) {
            logger.warning("Error getting worlds data: " + e.getMessage());
        }
        
        return worlds;
    }
    
    @Override
    public List<Map<String, String>> getPluginsData() {
        List<Map<String, String>> plugins = new ArrayList<>();
        
        try {
            for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                Map<String, String> pluginData = new HashMap<>();
                pluginData.put("name", plugin.getName());
                pluginData.put("version", plugin.getDescription().getVersion());
                pluginData.put("enabled", String.valueOf(plugin.isEnabled()));
                pluginData.put("author", String.join(", ", plugin.getDescription().getAuthors()));
                pluginData.put("description", plugin.getDescription().getDescription() != null ? 
                              plugin.getDescription().getDescription() : "No description");
                pluginData.put("website", plugin.getDescription().getWebsite() != null ? 
                              plugin.getDescription().getWebsite() : "No website");
                plugins.add(pluginData);
            }
            
            // Sort by plugin name
            plugins.sort((a, b) -> a.get("name").compareToIgnoreCase(b.get("name")));
        } catch (Exception e) {
            logger.warning("Error getting plugins data: " + e.getMessage());
        }
        
        return plugins;
    }
    
    /**
     * Formats bytes into human readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Formats uptime milliseconds into human readable format
     */
    private String formatUptime(long uptimeMillis) {
        long seconds = uptimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}