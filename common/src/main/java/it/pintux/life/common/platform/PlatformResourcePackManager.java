package it.pintux.life.common.platform;

import java.util.Set;
import java.util.UUID;

/**
 * Platform-specific resource pack manager interface
 */
public interface PlatformResourcePackManager {
    
    /**
     * Sends a resource pack to a specific player
     * @param playerId the player's UUID
     * @param packIdentifier the resource pack identifier
     * @return true if the pack was sent successfully
     */
    boolean sendResourcePack(UUID playerId, String packIdentifier);
    
    /**
     * Checks if a player has a specific resource pack loaded
     * @param playerId the player's UUID
     * @param packIdentifier the resource pack identifier
     * @return true if the player has the pack
     */
    boolean hasResourcePack(UUID playerId, String packIdentifier);
    
    /**
     * Gets the resource pack configured for a specific menu
     * @param menuName the menu name
     * @return the pack identifier or null if none configured
     */
    String getMenuResourcePack(String menuName);
    
    /**
     * Sends the appropriate resource pack for a menu to a player
     * @param playerUuid the player's UUID
     * @param menuName the menu name
     * @return true if a pack was sent successfully
     */
    boolean sendMenuResourcePack(UUID playerUuid, String menuName);
    
    /**
     * Gets all loaded resource packs
     * @return set of pack identifiers
     */
    Set<String> getLoadedPacks();
    
    /**
     * Checks if resource pack support is enabled
     * @return true if enabled
     */
    boolean isEnabled();
    
    /**
     * Checks if the platform supports resource packs
     * @return true if supported
     */
    boolean isResourcePackSupported();
    
    /**
     * Gets all resource packs loaded by a specific player
     * @param playerId the player's UUID
     * @return set of pack identifiers loaded by the player
     */
    Set<String> getPlayerResourcePacks(UUID playerId);
    
    /**
     * Gets all available resource packs that can be sent
     * @return set of available pack identifiers
     */
    Set<String> getAvailableResourcePacks();
    
    /**
     * Handles player disconnect cleanup
     * @param playerId the player's UUID
     */
    void onPlayerDisconnect(UUID playerId);
    
    /**
     * Removes a specific resource pack from a player
     * @param playerId the player's UUID
     * @param packIdentifier the resource pack identifier to remove
     */
    void removeResourcePack(UUID playerId, String packIdentifier);
    
    /**
     * Clears all resource packs for a player
     * @param playerId the player's UUID
     */
    void clearPlayerResourcePacks(UUID playerId);
}