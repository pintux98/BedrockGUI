package it.pintux.life.shopguiaddon.listener;

import it.pintux.life.shopguiaddon.backend.ShopBackendRouter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class ShopGuiCommandListener implements Listener {
    private final ShopBackendRouter router;

    public ShopGuiCommandListener(ShopBackendRouter router) {
        this.router = router;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        router.onCommand(event);
    }
}
