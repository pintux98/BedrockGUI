package it.pintux.life.duelsaddon.model;

import java.util.UUID;

public record MemberView(UUID playerId,
                         String playerName,
                         boolean leader,
                         boolean pending,
                         boolean online) {
}
