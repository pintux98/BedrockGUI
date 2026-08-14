package it.pintux.life.duelsaddon.util;

import java.util.UUID;

/**
 * Parses the argument half of a {@code pd_*} form action, the part after the colon in a menu YAML
 * entry such as {@code pd_queue_join:ranked|duo|crystal}.
 *
 * <p>Only parsers live here. Builders were removed: nothing in this addon constructs payloads,
 * because the forms call the services directly. Payloads are hand-written by whoever authors the
 * BedrockGUI menu, so every parser is lenient where a bad value has a sane default and throws
 * {@link IllegalArgumentException} where it does not — {@code DuelsFormAction} turns that into a
 * failed action with the message attached.</p>
 */
public final class DuelsActionPayloads {
    private static final String SPLIT = "\\|";

    private DuelsActionPayloads() {}

    /**
     * @return the payload as a plain player name, empty string when absent
     */
    public static String playerName(String payload) {
        return payload == null ? "" : payload.trim();
    }

    /**
     * @return the first {@code |}-separated segment, or an empty string
     */
    public static String first(String payload) {
        String[] parts = parts(payload);
        return parts.length == 0 ? "" : parts[0].trim();
    }

    /**
     * @param def returned when the payload has no second segment or it is not a number
     * @return the page number from {@code <something>|<page>}
     */
    public static int page(String payload, int def) {
        String[] parts = parts(payload);
        if (parts.length < 2) {
            return def;
        }
        try {
            return Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * @return whether the first segment selects the ranked ladder; anything else means unranked
     */
    public static boolean ranked(String payload) {
        return "ranked".equalsIgnoreCase(first(payload));
    }

    /**
     * @return the team size segment of {@code <ladder>|<size>|<mode>}, uppercased
     * @throws IllegalArgumentException when the payload has no size segment
     */
    public static String size(String payload) {
        String[] parts = parts(payload);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Missing team size in payload: " + payload);
        }
        return parts[1].trim().toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * @return the mode segment of {@code <ladder>|<size>|<mode>}
     * @throws IllegalArgumentException when the payload has no mode segment
     */
    public static String queueMode(String payload) {
        String[] parts = parts(payload);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Missing mode id in payload: " + payload);
        }
        return parts[2].trim();
    }

    /**
     * @return the payload's first segment parsed as a player uuid
     * @throws IllegalArgumentException when it is absent or malformed
     */
    public static UUID uuid(String payload) {
        String value = first(payload);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing player uuid in payload: " + payload);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid player uuid in payload: " + payload);
        }
    }

    private static String[] parts(String payload) {
        if (payload == null || payload.isBlank()) {
            return new String[0];
        }
        return payload.split(SPLIT);
    }
}
