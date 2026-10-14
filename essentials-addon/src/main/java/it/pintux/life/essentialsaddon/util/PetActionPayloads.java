package it.pintux.life.essentialsaddon.util;

public final class PetActionPayloads {

    private PetActionPayloads() {
    }

    public static String encodePet(String petId) {
        return petId;
    }

    public static String decodePet(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Invalid pet payload: " + payload);
        }
        return payload.trim();
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
