package it.pintux.life.homesteadaddon.model;

import java.util.UUID;

public record InviteView(
        long inviteId,
        UUID playerId,
        String playerName,
        long invitedAt
) {
}
