package it.pintux.life.bedwarsaddon.api;

import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.UpgradeContent;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Contract for a Bedwars team-upgrades backend (BedWars2023, etc.).
 * Implementations are the ONLY classes allowed to touch the underlying plugin's types.
 */
public interface UpgradeProvider {
    String getProviderId();

    boolean isReady();

    /** True if the player currently has the native upgrades GUI open (interception seam). */
    boolean isWatchingUpgradeGui(Player player);

    /** Clear the native "watching upgrades" state after we intercept the GUI. */
    void stopWatching(Player player);

    /** Buyable upgrades for the player's current team. */
    List<UpgradeContent> getUpgrades(Player player);

    /** Purchase an upgrade by id (delegates to the plugin, which handles money/messages). */
    PurchaseResult purchase(Player player, String upgradeId);
}
