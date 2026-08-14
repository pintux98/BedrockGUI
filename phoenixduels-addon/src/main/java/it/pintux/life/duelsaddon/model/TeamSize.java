package it.pintux.life.duelsaddon.model;

import java.util.Locale;

public enum TeamSize {
    SOLO,
    DUO,
    TRIO,
    QUAD;

    public static TeamSize parse(String raw) {
        if (raw == null) {
            return SOLO;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return SOLO;
        }
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
