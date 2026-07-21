package it.pintux.life.homesteadaddon.model;

import java.util.UUID;

public record RegionView(
        long id,
        String name,
        UUID ownerId,
        String ownerName,
        double bank,
        int mapColor,
        String mapIcon,
        int weather,
        int time,
        long createdAt,
        boolean publicRegion,
        long worldFlags,
        long playerFlags,
        int memberCount,
        int chunkCount,
        int subAreaCount,
        int globalRank
) {
    public boolean isOwnedBy(UUID playerId) {
        return ownerId != null && ownerId.equals(playerId);
    }
}
