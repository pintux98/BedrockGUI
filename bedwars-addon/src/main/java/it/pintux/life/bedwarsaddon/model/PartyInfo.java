package it.pintux.life.bedwarsaddon.model;

import java.util.List;

/**
 * A snapshot of a player's party.
 *
 * @param hasParty   whether the player is in a party
 * @param owner      whether the player owns the party
 * @param ownerName  the owner's name ("" if none)
 * @param members    member names (includes the owner)
 */
public record PartyInfo(boolean hasParty, boolean owner, String ownerName, List<String> members) {
    public static PartyInfo none() {
        return new PartyInfo(false, false, "", List.of());
    }
}
