package it.pintux.life.essentialsaddon.provider;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Abstract interface for warp providers (EssentialsX, CMI, etc.).
 * Implement this to add support for new warp plugins.
 */
public interface WarpProvider {

    /**
     * Unique identifier for this provider (e.g., "essentialsx", "cmi").
     */
    String getProviderId();

    /**
     * Returns true if the provider is available and ready.
     */
    boolean isReady();

    /**
     * Returns all warp names available on the server.
     */
    Collection<String> getWarpNames();

    /**
     * Returns the location of a warp by name.
     * Returns null if the warp does not exist.
     */
    Location getWarpLocation(String warpName);

    /**
     * Returns true if the player has permission to use this warp.
     */
    boolean hasAccess(Player player, String warpName);

    /**
     * Teleports the player to the named warp.
     * Returns true on success, false on failure.
     */
    boolean teleport(Player player, String warpName);

    /**
     * Returns the display name for a warp, or the warp name itself if no display name is configured.
     */
    default String getDisplayName(String warpName) {
        return warpName;
    }
}
