package it.pintux.life.shopguiaddon.listener;

import it.pintux.life.shopguiaddon.backend.ShopBackendRouter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

public final class ShopGuiInventoryListener implements Listener {
    private final ShopBackendRouter router;

    public ShopGuiInventoryListener(ShopBackendRouter router) {
        this.router = router;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        router.onInventoryOpen(event);
    }
}
