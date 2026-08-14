package it.pintux.life.duelsaddon.model;

import java.util.UUID;

/**
 * One party slot.
 *
 * <p>PhoenixDuels models an invited-but-unanswered player as a participant that is not yet
 * accepted, so {@link #pending()} distinguishes an outstanding invitation from a real member.</p>
 *
 * @param leader  whether this member currently leads the party
 * @param pending whether this is an unanswered invitation rather than a member
 * @param online  whether the player is connected; offline members stay in the party
 */
public record MemberView(UUID playerId,
                         String playerName,
                         boolean leader,
                         boolean pending,
                         boolean online) {
}
