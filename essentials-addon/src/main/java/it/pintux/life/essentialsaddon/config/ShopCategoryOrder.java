package it.pintux.life.essentialsaddon.config;

import java.util.Locale;

public enum ShopCategoryOrder {
    /**
     * The order the shop plugin lays its categories out in on Java: the configured slot for EconomyShopGUI,
     * and the order ShopGUI+ hands its shops back in.
     */
    MENU,
    /**
     * Alphabetical, ignoring colour codes.
     */
    NAME;

    public static ShopCategoryOrder parse(String raw) {
        if (raw != null) {
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fall through to the default
            }
        }
        return MENU;
    }
}
