package it.pintux.life.duelsaddon.model;

import java.util.Locale;

public enum StatsKind {
    UNRANKED,
    RANKED,
    CHALLENGE;

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

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
