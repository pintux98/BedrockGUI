package it.pintux.life.essentialsaddon.provider;

import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Abstract interface for kit providers (EssentialsX, CMI, etc.).
 * Implement this to add support for new kit plugins.
 */
public interface KitProvider {

    /**
     * Unique identifier for this provider (e.g., "essentialsx", "cmi").
     */
    String getProviderId();

    /**
     * Returns true if the provider is available and ready.
     */
    boolean isReady();

    /**
     * Returns all kit names available on the server.
     */
    Collection<String> getKitNames();

    /**
     * Returns true if the player has permission to use this kit.
     */
    boolean hasAccess(Player player, String kitName);

    /**
     * Gives the kit to the player.
     * Returns true on success, false on failure.
     */
    boolean claimKit(Player player, String kitName);

    /**
     * Returns the display name for a kit, or the kit name itself if no display name is configured.
     */
    default String getDisplayName(String kitName) {
        return kitName;
    }

    /**
     * Returns the number of items in the kit, or -1 if unknown.
     */
    default int getItemCount(String kitName) {
        return -1;
    }

    /**
     * Returns the cooldown remaining in seconds, or 0 if no cooldown.
     */
    default long getCooldownSeconds(Player player, String kitName) {
        return 0;
    }

    /**
     * Returns true if the kit is currently available for the player (not on cooldown).
     */
    default boolean isAvailable(Player player, String kitName) {
        return true;
    }
}
