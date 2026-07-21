package it.pintux.life.homesteadaddon.model;

public record RewardsView(
        int chunksPerMember,
        int subAreasPerMember,
        int chunksByPlaytime,
        int subAreasByPlaytime
) {
}
