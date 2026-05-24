package it.pintux.life.essentialsaddon.backend;

import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Minimal interface for routing Bedrock shop interactions.
 * Add a new shop plugin by implementing this and registering it in the router.
 */
public interface ShopBackend {
    /**
     * Higher priority backends get first chance to handle shared commands like /shop.
     */
    int priority();

    /**
     * Called once on plugin enable (should be fast and safe).
     */
    void bootstrap();

    /**
     * Return true if the backend consumed the command (and probably cancelled the event).
     */
    boolean handleCommand(PlayerCommandPreprocessEvent event);

    /**
     * Return true if the backend consumed the inventory open (and cancelled the event).
     */
    boolean handleInventoryOpen(InventoryOpenEvent event);
}

