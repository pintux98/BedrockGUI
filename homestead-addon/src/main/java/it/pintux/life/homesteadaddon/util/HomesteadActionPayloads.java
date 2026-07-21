package it.pintux.life.homesteadaddon.util;

import java.util.UUID;

public final class HomesteadActionPayloads {
    private static final String SEP = "|";
    private static final String SPLIT = "\\|";

    private HomesteadActionPayloads() {}


    public static String region(long regionId) {
        return Long.toString(regionId);
    }

    public static String regionPage(long regionId, int page) {
        return regionId + SEP + page;
    }

    public static String regionMember(long regionId, UUID member) {
        return regionId + SEP + member;
    }

    public static String regionSub(long regionId, long subAreaId) {
        return regionId + SEP + subAreaId;
    }

    public static String regionSubMember(long regionId, long subAreaId, UUID member) {
        return regionId + SEP + subAreaId + SEP + member;
    }


    public static String[] parts(String payload) {
        if (payload == null || payload.isBlank()) {
            return new String[0];
        }
        return payload.split(SPLIT);
    }

    public static long regionId(String payload) {
        String[] parts = parts(payload);
        if (parts.length == 0) {
            throw new IllegalArgumentException("Missing region id in payload: " + payload);
        }
        return parseLong(parts[0], "region id", payload);
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

    public static long subAreaId(String payload) {
        String[] parts = parts(payload);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Missing sub-area id in payload: " + payload);
        }
        return parseLong(parts[1], "sub-area id", payload);
    }

    public static UUID member(String payload) {
        String[] parts = parts(payload);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Missing member uuid in payload: " + payload);
        }
        return parseUuid(parts[1], payload);
    }

    public static UUID subAreaMember(String payload) {
        String[] parts = parts(payload);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Missing sub-area member uuid in payload: " + payload);
        }
        return parseUuid(parts[2], payload);
    }

    private static long parseLong(String raw, String field, String payload) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + field + " in payload: " + payload);
        }
    }

    private static UUID parseUuid(String raw, String payload) {
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid member uuid in payload: " + payload);
        }
    }
}
