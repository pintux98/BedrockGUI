package it.pintux.life.duelsaddon.model;

import java.util.List;
import java.util.UUID;

public record PartyView(UUID leaderId,
                        String leaderName,
                        int maximumSlots,
                        List<MemberView> members,
                        boolean pendingChallenge) {

    public int memberCount() {
        return (int) members.stream().filter(m -> !m.pending()).count();
    }

    public boolean isLeader(UUID uuid) {
        return leaderId != null && leaderId.equals(uuid);
    }
}
