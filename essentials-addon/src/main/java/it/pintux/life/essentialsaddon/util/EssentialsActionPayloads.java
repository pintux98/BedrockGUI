package it.pintux.life.essentialsaddon.util;

public final class EssentialsActionPayloads {
    private EssentialsActionPayloads() {}

    public static String encodeWarp(String warpName) {
        return warpName;
    }

    public static String decodeWarp(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Invalid warp payload: " + payload);
        }
        return payload;
    }

    public static String encodeKit(String kitName) {
        return kitName;
    }

    public static String decodeKit(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Invalid kit payload: " + payload);
        }
        return payload;
    }

    public static String encodeHome(String homeName) {
        return homeName;
    }

    public static String decodeHome(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Invalid home payload: " + payload);
        }
        return payload;
    }
}
