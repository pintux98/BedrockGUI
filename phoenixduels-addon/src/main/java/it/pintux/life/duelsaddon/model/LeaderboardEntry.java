package it.pintux.life.duelsaddon.model;

/**
 * One ranked row of a leaderboard, already sorted by the requested metric.
 *
 * @param rank  1-based position
 * @param value the metric's value for this player
 */
public record LeaderboardEntry(int rank, String playerName, int value) {
}
