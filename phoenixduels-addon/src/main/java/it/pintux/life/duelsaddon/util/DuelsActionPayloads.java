package it.pintux.life.duelsaddon.util;

import java.util.Locale;
import java.util.UUID;

public final class DuelsActionPayloads {
    private static final String SEP = "|";
    private static final String SPLIT = "\\|";

    private DuelsActionPayloads() {}

    public static String mode(String modeId) {
        return modeId == null ? "" : modeId;
    }

    public static String modePage(String modeId, int page) {
        return mode(modeId) + SEP + page;
    }

    public static String queue(boolean ranked, String size, String modeId) {
        return (ranked ? "ranked" : "unranked") + SEP + size.toLowerCase(Locale.ROOT) + SEP + mode(modeId);
    }

    public static String player(UUID target) {
        return target == null ? "" : target.toString();
    }

    public static String playerName(String name) {
        return name == null ? "" : name;
    }

    public static String[] parts(String payload) {
        if (payload == null || payload.isBlank()) {
            return new String[0];
        }
        return payload.split(SPLIT);
    }

    public static String first(String payload) {
        String[] parts = parts(payload);
        return parts.length == 0 ? "" : parts[0].trim();
    }

    public static String modeId(String payload) {
        String value = first(payload);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing mode id in payload: " + payload);
        }
        return value;
    }

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

    public static boolean ranked(String payload) {
        return "ranked".equalsIgnoreCase(first(payload));
    }

    public static String size(String payload) {
        String[] parts = parts(payload);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Missing team size in payload: " + payload);
        }
        return parts[1].trim().toUpperCase(Locale.ROOT);
    }

    public static String queueMode(String payload) {
        String[] parts = parts(payload);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Missing mode id in payload: " + payload);
        }
        return parts[2].trim();
    }

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

    public static int rounds(String payload, int def) {
        String[] parts = parts(payload);
        if (parts.length < 2) {
            return def;
        }
        try {
            return Math.max(1, Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
