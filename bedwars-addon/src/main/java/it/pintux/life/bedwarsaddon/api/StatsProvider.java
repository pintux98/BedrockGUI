package it.pintux.life.bedwarsaddon.api;

import it.pintux.life.bedwarsaddon.model.PlayerStatsInfo;
import org.bukkit.entity.Player;

/**
 * Contract for a Bedwars stats backend (BedWars2023, etc.).
 * Implementations are the ONLY classes allowed to touch the underlying plugin's types.
 */
public interface StatsProvider {
    String getProviderId();

    boolean isReady();

    /** The player's stats, or null if unavailable. */
    PlayerStatsInfo getStats(Player player);
}
