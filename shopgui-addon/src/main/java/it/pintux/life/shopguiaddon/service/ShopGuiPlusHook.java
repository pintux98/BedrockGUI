package it.pintux.life.shopguiaddon.service;

import net.brcdev.shopgui.ShopGuiPlusApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShopGuiPlusHook {
    private final JavaPlugin plugin;
    private final ShopGuiCatalogService catalogService;
    private boolean hooked;

    public ShopGuiPlusHook(JavaPlugin plugin, ShopGuiCatalogService catalogService) {
        this.plugin = plugin;
        this.catalogService = catalogService;
    }

    public void bootstrapIfReady() {
        try {
            if (ShopGuiPlusApi.getPlugin() == null) {
                return;
            }
            if (!hooked) {
                hooked = true;
                plugin.getLogger().info("Detected ShopGUI+ and preparing Bedrock shop bridge.");
            }
            if (ShopGuiPlusApi.getPlugin().getShopManager().areShopsLoaded()) {
                catalogService.refreshCatalog();
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to bootstrap ShopGUI+ hook: " + exception.getMessage());
        }
    }

    public void onPostEnable() {
        hooked = true;
        bootstrapIfReady();
    }

    public void onShopsPostLoad() {
        catalogService.refreshCatalog();
    }
}
