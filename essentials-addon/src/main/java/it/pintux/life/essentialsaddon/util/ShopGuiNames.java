package it.pintux.life.essentialsaddon.util;

public final class ShopGuiNames {

    private ShopGuiNames() {
    }

    /**
     * Resolves a ShopGUI+ shop name for a page the same way ShopGUI+ does when it builds its own inventory
     * title: {@code Shop#getName(int)} returns the configured name verbatim, so a name like
     * {@code "&9&lBlocks (page %page%)"} still carries the placeholder and would otherwise reach the form.
     */
    public static String resolvePageName(String rawName, int page) {
        return rawName == null ? "" : rawName.replace("%page%", Integer.toString(page));
    }
}
