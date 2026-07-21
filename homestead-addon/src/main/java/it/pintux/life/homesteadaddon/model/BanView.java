package it.pintux.life.homesteadaddon.model;

import java.util.UUID;

public record BanView(
        long banId,
        UUID playerId,
        String playerName,
        String reason,
        long bannedAt
) {
}
