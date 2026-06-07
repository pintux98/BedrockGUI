package it.pintux.life.bedwarsaddon.api;

import it.pintux.life.bedwarsaddon.model.ArenaInfo;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * Contract for a Bedwars arena backend (BedWars2023, etc.).
 * Implementations are the ONLY classes allowed to touch the underlying plugin's types.
 */
public interface ArenaProvider {
    String getProviderId();

    boolean isReady();

    /** All known arenas. */
    List<ArenaInfo> getArenas();

    /** Join an arena by its internal name. Returns false if it could not be joined. */
    boolean join(Player player, String arenaName);

    /** Interception seam: is this inventory the native arena-selector GUI? */
    boolean ownsInventory(Inventory inventory);
}
