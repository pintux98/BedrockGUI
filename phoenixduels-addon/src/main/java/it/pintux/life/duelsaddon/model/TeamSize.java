package it.pintux.life.duelsaddon.model;

import java.util.Locale;

/**
 * Queue team size, the half of PhoenixDuels' {@code PlayerMode} that is not the ladder.
 */
public enum TeamSize {
    SOLO,
    DUO,
    TRIO,
    QUAD;

    /**
     * Lenient parse for values coming out of form action payloads, which players hand-write in
     * their menu YAML.
     *
     * @return the matching size, or {@link #SOLO} for anything unrecognised
     */
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

    /**
     * @return the lowercase form used in action payloads and form titles
     */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
