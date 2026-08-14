package it.pintux.life.duelsaddon.model;

import java.util.UUID;

public record InviteView(UUID inviterId,
                         String inviterName,
                         String modeId,
                         String modeName,
                         int rounds,
                         int expiresInSeconds,
                         boolean party) {
}
