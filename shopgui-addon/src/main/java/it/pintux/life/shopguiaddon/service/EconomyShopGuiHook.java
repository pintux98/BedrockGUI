package it.pintux.life.shopguiaddon.service;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyShopGuiHook {
    private final JavaPlugin plugin;
    private final EconomyShopCatalogService catalogService;

    public EconomyShopGuiHook(JavaPlugin plugin, EconomyShopCatalogService catalogService) {
        this.plugin = plugin;
        this.catalogService = catalogService;
    }

    public void bootstrapIfReady() {
        if (!isPluginPresent()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, catalogService::refreshCatalog);
    }

    public void onShopItemsLoaded() {
        catalogService.refreshCatalog();
    }

    public boolean isPluginPresent() {
        return Bukkit.getPluginManager().getPlugin("EconomyShopGUI") != null
                || Bukkit.getPluginManager().getPlugin("EconomyShopGUI-Premium") != null;
    }
}
