package it.pintux.life.homesteadaddon.model;

public record LevelView(
        int level,
        long xpProgress,
        long xpForNextLevel,
        double progressPercent,
        long totalXp,
        int rank
) {
}
