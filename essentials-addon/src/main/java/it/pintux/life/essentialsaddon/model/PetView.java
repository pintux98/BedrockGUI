package it.pintux.life.essentialsaddon.model;

import java.util.UUID;

/** Immutable snapshot of one owned pet for Bedrock forms. */
public record PetView(
        UUID uuid,
        String name,
        String petType,
        int level,
        double health,
        double maxHealth,
        double hunger,
        String skilltreeName,
        boolean active
) {
    /** -1 level means "unknown" (stored/inactive pet whose level could not be read). */
    public boolean hasLevel() {
        return level >= 0;
    }
}
