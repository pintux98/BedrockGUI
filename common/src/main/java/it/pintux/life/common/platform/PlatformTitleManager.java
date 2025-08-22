package it.pintux.life.common.platform;

import it.pintux.life.common.utils.FormPlayer;

/**
 * Platform abstraction for sending titles and action bars to players.
 */
public interface PlatformTitleManager {
    /**
     * Send a title and optional subtitle to the player with custom timings.
     *
     * @param player the target player
     * @param title the title text (may be null or empty)
     * @param subtitle the subtitle text (may be null or empty)
     * @param fadeIn ticks to fade in
     * @param stay ticks to stay
     * @param fadeOut ticks to fade out
     * @return true if sent successfully
     */
    boolean sendTitle(FormPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    /**
     * Send an action bar message to the player.
     *
     * @param player the target player
     * @param message the action bar message
     * @return true if sent successfully
     */
    boolean sendActionBar(FormPlayer player, String message);

    /**
     * Clears any currently displayed title from the player.
     * @param player the target player
     */
    void clearTitle(FormPlayer player);

    /**
     * Resets the player's title settings and clears current title.
     * @param player the target player
     */
    void resetTitle(FormPlayer player);

    /**
     * @return true if titles/action bars are supported on this platform
     */
    boolean isSupported();
}