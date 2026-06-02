package it.pintux.life.bedwarsaddon.api;

import it.pintux.life.bedwarsaddon.model.PartyInfo;
import org.bukkit.entity.Player;

/**
 * Contract for a Bedwars party backend (BedWars2023, etc.).
 * Implementations are the ONLY classes allowed to touch the underlying plugin's types.
 */
public interface PartyProvider {
    String getProviderId();

    boolean isReady();

    PartyInfo getParty(Player player);

    /** Add (or create-with) a member by name. Returns false if the target is not online. */
    boolean add(Player requester, String targetName);

    void leave(Player player);

    /** Disband the party (only effective if the player is the owner). */
    void disband(Player player);

    /** Kick a member by name. Returns false if the target is not online. */
    boolean kick(Player owner, String targetName);
}
