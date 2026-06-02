package it.pintux.life.bedwarsaddon.api;

import it.pintux.life.bedwarsaddon.model.SpectateTarget;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Contract for a Bedwars spectator backend (BedWars2023, etc.).
 * Implementations are the ONLY classes allowed to touch the underlying plugin's types.
 */
public interface SpectatorProvider {
    String getProviderId();

    boolean isReady();

    /** Players in the spectator's arena that can be teleported to. */
    List<SpectateTarget> getTargets(Player spectator);

    /** Teleport the spectator to the target. Returns false if the target is gone. */
    boolean teleport(Player spectator, String targetUuid);
}
