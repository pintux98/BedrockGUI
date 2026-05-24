package it.pintux.life.essentialsaddon.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Contract for warp providers (EssentialsX, CMI, etc.).
 * Implement to add support for new warp plugins.
 */
public interface WarpProvider {
    String getProviderId();
    boolean isReady();
    Collection<String> getWarpNames();
    Location getWarpLocation(String warpName);
    boolean hasAccess(Player player, String warpName);
    boolean teleport(Player player, String warpName);
    default String getDisplayName(String warpName) { return warpName; }
}
