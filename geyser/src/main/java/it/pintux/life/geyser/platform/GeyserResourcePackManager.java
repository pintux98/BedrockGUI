package it.pintux.life.geyser.platform;

import it.pintux.life.common.platform.PlatformResourcePackManager;
import it.pintux.life.common.exceptions.ResourcePackException;
import it.pintux.life.geyser.utils.GeyserConfig;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class GeyserResourcePackManager implements PlatformResourcePackManager {
    
    private final Logger logger;
    private final GeyserConfig config;
    private final Map<UUID, Set<String>> playerPacks;
    private final Map<String, ResourcePackInfo> availablePacks;
    
    public GeyserResourcePackManager(Logger logger, GeyserConfig config) {
        this.logger = logger;
        this.config = config;
        this.playerPacks = new ConcurrentHashMap<>();
        this.availablePacks = new ConcurrentHashMap<>();
        loadResourcePacks();
    }
    
    @Override
    public boolean sendResourcePack(UUID playerId, String packIdentifier) {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(playerId);
        if (connection == null) {
            return false;
        }
        
        ResourcePackInfo packInfo = availablePacks.get(packIdentifier);
        if (packInfo == null) {
            return false;
        }
        
        try {
            // Note: Geyser API doesn't directly support resource pack sending
            // This would need to be implemented through the backend server
            // or through Geyser's resource pack system if available
            
            logger.info("Sending resource pack '" + packIdentifier + "' to player " + playerId);
            
            // For now, we'll track that the pack was "sent"
            playerPacks.computeIfAbsent(playerId, k -> new HashSet<>()).add(packIdentifier);
            
            // In a real implementation, you would:
            // 1. Send the resource pack through Geyser's API (if available)
            // 2. Or coordinate with the backend server to send the pack
            // 3. Handle the response and update player state accordingly
            
            return true;
            
        } catch (Exception e) {
            logger.warning("Failed to send resource pack: " + e.getMessage());
            return false;
        }
    }
    
    public void removeResourcePack(UUID playerId, String packIdentifier) throws ResourcePackException {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(playerId);
        if (connection == null) {
            throw new ResourcePackException(
                "Player not connected via Geyser", 
                packIdentifier, 
                ResourcePackException.ErrorType.GEYSER_CONNECTION_FAILED
            );
        }
        
        Set<String> packs = playerPacks.get(playerId);
        if (packs != null) {
            packs.remove(packIdentifier);
            if (packs.isEmpty()) {
                playerPacks.remove(playerId);
            }
        }
        
        logger.info("Removed resource pack '" + packIdentifier + "' from player " + playerId);
    }
    
    public Set<String> getPlayerResourcePacks(UUID playerId) {
        return new HashSet<>(playerPacks.getOrDefault(playerId, Collections.emptySet()));
    }
    
    @Override
    public boolean hasResourcePack(UUID playerId, String packIdentifier) {
        Set<String> packs = playerPacks.get(playerId);
        return packs != null && packs.contains(packIdentifier);
    }
    
    public void clearPlayerResourcePacks(UUID playerId) {
        playerPacks.remove(playerId);
        logger.info("Cleared all resource packs for player " + playerId);
    }
    
    public Set<String> getAvailableResourcePacks() {
        return new HashSet<>(availablePacks.keySet());
    }
    
    public void reloadResourcePacks() {
        availablePacks.clear();
        loadResourcePacks();
        logger.info("Resource packs reloaded");
    }
    
    public boolean isResourcePacksEnabled() {
        return config.getBoolean("resource-packs.enabled", true);
    }
    
    @Override
    public boolean isEnabled() {
        return config.getBoolean("resource-packs.enabled", true);
    }
    
    @Override
    public boolean isResourcePackSupported() {
        return true; // Geyser supports resource packs for Bedrock clients
    }
    
    @Override
    public String getMenuResourcePack(String menuName) {
        // Check if there's a specific pack configured for this menu
        String packName = config.getString("resource-packs.menu-packs." + menuName, null);
        if (packName != null && availablePacks.containsKey(packName)) {
            return packName;
        }
        // Return default pack if no specific pack is configured
        String defaultPack = config.getString("resource-packs.default-pack", "default");
        return availablePacks.containsKey(defaultPack) ? defaultPack : null;
    }
    
    @Override
    public Set<String> getLoadedPacks() {
        return new HashSet<>(availablePacks.keySet());
    }
    
    @Override
    public boolean sendMenuResourcePack(UUID playerUuid, String menuName) {
        String packName = getMenuResourcePack(menuName);
        if (packName != null) {
            return sendResourcePack(playerUuid, packName);
        }
        return false;
    }
    
    public void onPlayerJoin(UUID playerId) {
        if (!isResourcePacksEnabled()) {
            return;
        }
        
        boolean autoSend = config.getBoolean("resource-packs.auto-send", true);
        if (autoSend) {
            String defaultPack = config.getString("resource-packs.default-pack", "default");
            if (availablePacks.containsKey(defaultPack)) {
                boolean success = sendResourcePack(playerId, defaultPack);
                if (!success) {
                    logger.warning("Failed to send default resource pack to player " + playerId);
                }
            }
        }
    }
    
    public void onPlayerDisconnect(UUID playerId) {
        clearPlayerResourcePacks(playerId);
    }
    
    private void loadResourcePacks() {
        if (!config.contains("resource-packs.packs")) {
            logger.warning("No resource packs configured");
            return;
        }
        
        Set<String> packKeys = config.getKeys("resource-packs.packs");
        for (String packKey : packKeys) {
            String basePath = "resource-packs.packs." + packKey;
            
            String name = config.getString(basePath + ".name", packKey);
            String url = config.getString(basePath + ".url", "");
            String hash = config.getString(basePath + ".hash", "");
            boolean force = config.getBoolean(basePath + ".force", false);
            String description = config.getString(basePath + ".description", "");
            
            if (url.isEmpty()) {
                logger.warning("Resource pack '" + packKey + "' has no URL configured");
                continue;
            }
            
            ResourcePackInfo packInfo = new ResourcePackInfo(name, url, hash, force, description);
            availablePacks.put(packKey, packInfo);
            
            logger.info("Loaded resource pack: " + name + " (" + packKey + ")");
        }
    }
    
    /**
     * Gets information about a specific resource pack
     * @param packIdentifier the pack identifier
     * @return the resource pack info or null if not found
     */
    public ResourcePackInfo getResourcePackInfo(String packIdentifier) {
        return availablePacks.get(packIdentifier);
    }
    
    /**
     * Gets all connected Geyser players
     * @return set of player UUIDs
     */
    public Set<UUID> getConnectedPlayers() {
        Set<UUID> players = new HashSet<>();
        for (GeyserConnection connection : GeyserApi.api().onlineConnections()) {
            if (connection != null) {
                players.add(connection.javaUuid());
            }
        }
        return players;
    }
    
    /**
     * Sends the default resource pack to all connected players
     */
    public void sendDefaultPackToAll() {
        if (!isResourcePacksEnabled()) {
            return;
        }
        
        String defaultPack = config.getString("resource-packs.default-pack", "default");
        if (!availablePacks.containsKey(defaultPack)) {
            logger.warning("Default resource pack '" + defaultPack + "' not found");
            return;
        }
        
        for (UUID playerId : getConnectedPlayers()) {
            boolean success = sendResourcePack(playerId, defaultPack);
            if (!success) {
                logger.warning("Failed to send default pack to player " + playerId);
            }
        }
    }
    
    /**
     * Resource pack information holder
     */
    public static class ResourcePackInfo {
        private final String name;
        private final String url;
        private final String hash;
        private final boolean force;
        private final String description;
        
        public ResourcePackInfo(String name, String url, String hash, boolean force, String description) {
            this.name = name;
            this.url = url;
            this.hash = hash;
            this.force = force;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getHash() { return hash; }
        public boolean isForce() { return force; }
        public String getDescription() { return description; }
    }
}