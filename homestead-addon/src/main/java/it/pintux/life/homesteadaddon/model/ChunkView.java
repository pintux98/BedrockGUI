package it.pintux.life.homesteadaddon.model;

import java.util.UUID;

public record ChunkView(
        String worldName,
        UUID worldId,
        int x,
        int z
) {
}
