package it.pintux.life.paper;

import it.pintux.life.common.platform.PlatformResourcePackManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Paper implementation of the resource pack manager
 */
public class PaperResourcePackManager implements PlatformResourcePackManager {
    
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<UUID, Set<String>> playerResourcePacks;
    private final Map<String, String> availableResourcePacks;
    private boolean enabled;
    
    public PaperResourcePackManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.playerResourcePacks = new ConcurrentHashMap<>();
        this.availableResourcePacks = new ConcurrentHashMap<>();
        this.enabled = false;
        
        // Initialize with default resource packs from config
        loadAvailableResourcePacks();
    }
    
    private void loadAvailableResourcePacks() {
        // Load resource packs from config
        if (plugin.getConfig().getConfigurationSection("resource_packs") != null) {
            enabled = plugin.getConfig().getBoolean("resource_packs.enabled", false);
            
            if (enabled) {
                // Add default packs
                availableResourcePacks.put("ui_enhanced", "https://example.com/ui_enhanced.zip");
                availableResourcePacks.put("custom_icons", "https://example.com/custom_icons.zip");
                availableResourcePacks.put("admin_tools", "https://example.com/admin_tools.zip");
                
                logger.info("Loaded " + availableResourcePacks.size() + " resource packs");
            }
        }
    }
    
    @Override
    public boolean sendResourcePack(UUID playerId, String resourcePackId) {
        if (!enabled) {
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return false;
        }
        
        String packUrl = availableResourcePacks.get(resourcePackId);
        if (packUrl == null) {
            logger.warning("Resource pack not found: " + resourcePackId);
            return false;
        }
        
        try {
            // Send resource pack to player
            player.setResourcePack(packUrl);
            
            // Track the resource pack for this player
            playerResourcePacks.computeIfAbsent(playerId, k -> new HashSet<>()).add(resourcePackId);
            
            logger.info("Sent resource pack '" + resourcePackId + "' to player: " + player.getName());
            return true;
        } catch (Exception e) {
            logger.warning("Failed to send resource pack to player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean hasResourcePack(UUID playerId, String resourcePackId) {
        Set<String> packs = playerResourcePacks.get(playerId);
        return packs != null && packs.contains(resourcePackId);
    }
    
    @Override
    public Set<String> getPlayerResourcePacks(UUID playerId) {
        return playerResourcePacks.getOrDefault(playerId, new HashSet<>());
    }
    
    @Override
    public Set<String> getAvailableResourcePacks() {
        return new HashSet<>(availableResourcePacks.keySet());
    }
    
    @Override
    public boolean isResourcePackSupported() {
        // Paper/Bukkit supports resource packs
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public Set<String> getLoadedPacks() {
        return new HashSet<>(availableResourcePacks.keySet());
    }
    
    @Override
    public boolean sendMenuResourcePack(UUID playerId, String menuName) {
        // For Paper, we can use the same logic as sendResourcePack
        // In a real implementation, you might have menu-specific resource packs
        return sendResourcePack(playerId, "ui_enhanced"); // Default to UI enhanced pack for menus
    }
    
    @Override
    public String getMenuResourcePack(String menuName) {
        // Return default resource pack for menus
        return "ui_enhanced"; // Default to UI enhanced pack for menus
    }
    

    
    /**
     * Handles player disconnect cleanup
     */
    @Override
    public void onPlayerDisconnect(UUID playerId) {
        playerResourcePacks.remove(playerId);
        logger.info("Cleaned up resource pack data for disconnected player: " + playerId);
    }
    
    @Override
    public void removeResourcePack(UUID playerId, String packIdentifier) {
        Set<String> playerPacks = playerResourcePacks.get(playerId);
        if (playerPacks != null) {
            playerPacks.remove(packIdentifier);
            logger.info("Removed resource pack '" + packIdentifier + "' from player: " + playerId);
        }
    }
    
    @Override
    public void clearPlayerResourcePacks(UUID playerId) {
        playerResourcePacks.remove(playerId);
        
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            // Reset to default resource pack (empty)
            player.setResourcePack("");
        }
        
        logger.info("Cleared all resource packs for player: " + playerId);
    }
    
    /**
     * Reloads resource pack configuration
     */
    public void reload() {
        availableResourcePacks.clear();
        loadAvailableResourcePacks();
        logger.info("Resource pack configuration reloaded");
    }
}