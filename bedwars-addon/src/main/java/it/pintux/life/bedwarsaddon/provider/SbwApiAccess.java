package it.pintux.life.bedwarsaddon.provider;

import org.screamingsandals.bedwars.api.BedwarsAPI;

/** Resolves the ScreamingBedWars API singleton. Null-safe. */
public final class SbwApiAccess {

    public BedwarsAPI get() {
        try {
            return BedwarsAPI.getInstance();
        } catch (Throwable t) {
            return null;
        }
    }

    public boolean isAvailable() {
        return get() != null;
    }
}
