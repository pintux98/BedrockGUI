package it.pintux.life.homesteadaddon.model;

import java.util.UUID;

public record MemberView(
        long memberRowId,
        UUID playerId,
        String playerName,
        long playerFlags,
        long controlFlags,
        long joinedAt
) {
}
