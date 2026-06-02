package it.pintux.life.essentialsaddon.shop.listener;

import it.pintux.life.shopguiaddon.service.EconomyShopGuiHook;
import me.gypopo.economyshopgui.api.events.ShopItemsLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class EconomyShopLifecycleListener implements Listener {
    private final EconomyShopGuiHook hook;

    public EconomyShopLifecycleListener(EconomyShopGuiHook hook) {
        this.hook = hook;
    }

    @EventHandler
    public void onShopItemsLoad(ShopItemsLoadEvent event) {
        hook.onShopItemsLoaded();
    }
}
