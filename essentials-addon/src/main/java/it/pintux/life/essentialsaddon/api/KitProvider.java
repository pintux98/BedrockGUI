package it.pintux.life.essentialsaddon.api;

import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Contract for kit providers (EssentialsX, CMI, etc.).
 * Implement to add support for new kit plugins.
 */
public interface KitProvider {
    String getProviderId();
    boolean isReady();
    Collection<String> getKitNames();
    boolean hasAccess(Player player, String kitName);
    boolean claimKit(Player player, String kitName);
    default String getDisplayName(String kitName) { return kitName; }
    default int getItemCount(String kitName) { return -1; }
    default long getCooldownSeconds(Player player, String kitName) { return 0; }
    default boolean isAvailable(Player player, String kitName) { return true; }
}
