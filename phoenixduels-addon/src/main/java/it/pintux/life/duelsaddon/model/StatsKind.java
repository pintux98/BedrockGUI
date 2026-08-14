package it.pintux.life.duelsaddon.model;

import java.util.Locale;

/**
 * Which ladder a statistic belongs to, mirroring PhoenixDuels' {@code PlayerStats.MatchType}
 * without exposing it.
 */
public enum StatsKind {
    UNRANKED,
    RANKED,
    CHALLENGE;

    /**
     * Lenient parse for values coming out of form action payloads.
     *
     * @return the matching ladder, or {@link #UNRANKED} for anything unrecognised
     */
    public static StatsKind parse(String raw) {
        if (raw == null) {
            return UNRANKED;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNRANKED;
        }
    }
}
