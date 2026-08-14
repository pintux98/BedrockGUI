package it.pintux.life.duelsaddon.model;

/**
 * One player's duel statistics for a single ladder.
 *
 * <p>The ladder is not carried here: callers already hold the {@link StatsKind} they asked for.</p>
 */
public record StatsView(String playerName,
                        int wins,
                        int losses,
                        int draws,
                        int kills,
                        int deaths) {

    /**
     * @return wins plus losses plus draws, the denominator for a win rate
     */
    public int playedMatches() {
        return wins + losses + draws;
    }
}
