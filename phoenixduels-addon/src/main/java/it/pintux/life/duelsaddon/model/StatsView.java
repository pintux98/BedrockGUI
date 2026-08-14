package it.pintux.life.duelsaddon.model;

public record StatsView(String playerName,
                        StatsKind kind,
                        int wins,
                        int losses,
                        int draws,
                        int kills,
                        int deaths) {

    public int playedMatches() {
        return wins + losses + draws;
    }
}
