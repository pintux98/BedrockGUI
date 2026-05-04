package it.pintux.life.shopguiaddon.listener;

import it.pintux.life.shopguiaddon.service.ShopGuiPlusHook;
import net.brcdev.shopgui.event.ShopGUIPlusPostEnableEvent;
import net.brcdev.shopgui.event.ShopsPostLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ShopGuiLifecycleListener implements Listener {
    private final ShopGuiPlusHook hook;

    public ShopGuiLifecycleListener(ShopGuiPlusHook hook) {
        this.hook = hook;
    }

    @EventHandler
    public void onShopGuiPlusPostEnable(ShopGUIPlusPostEnableEvent event) {
        hook.onPostEnable();
    }

    @EventHandler
    public void onShopsPostLoad(ShopsPostLoadEvent event) {
        hook.onShopsPostLoad();
    }
}
