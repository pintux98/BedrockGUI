package it.pintux.life.essentialsaddon.util;

import java.util.UUID;

public final class PetActionPayloads {

    private PetActionPayloads() {
    }

    public static String encodePet(UUID petUuid) {
        return petUuid.toString();
    }

    public static UUID decodePet(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Invalid pet payload: " + payload);
        }
        return UUID.fromString(payload.trim());
    }

    public static String encodeSkilltree(String petUuid, String skilltreeName) {
        return petUuid + "|" + skilltreeName;
    }

    public static String[] decodeSkilltree(String payload) {
        if (payload == null || !payload.contains("|")) {
            throw new IllegalArgumentException("Invalid skilltree payload: " + payload);
        }
        return payload.split("\\|", 2);
    }

    public static String encodeShop(String shopId, String petId) {
        return (shopId == null ? "" : shopId) + "|" + petId;
    }

    public static String[] decodeShop(String payload) {
        if (payload == null || !payload.contains("|")) {
            throw new IllegalArgumentException("Invalid shop payload: " + payload);
        }
        return payload.split("\\|", 2);
    }
}
