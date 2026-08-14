package it.pintux.life.duelsaddon.model;

import java.util.List;
import java.util.UUID;

/**
 * A PhoenixDuels party, including its unanswered invitations.
 *
 * @param maximumSlots     the party's slot cap, which permissions can raise per player
 * @param members          members and pending invitations, leader first
 * @param pendingChallenge whether the party is already committed to a challenge, in which case it
 *                         cannot be challenged again
 */
public record PartyView(UUID leaderId,
                        String leaderName,
                        int maximumSlots,
                        List<MemberView> members,
                        boolean pendingChallenge) {

    /**
     * @return members who have actually joined, excluding unanswered invitations
     */
    public int memberCount() {
        return (int) members.stream().filter(member -> !member.pending()).count();
    }

    public boolean isLeader(UUID uuid) {
        return leaderId != null && leaderId.equals(uuid);
    }
}
