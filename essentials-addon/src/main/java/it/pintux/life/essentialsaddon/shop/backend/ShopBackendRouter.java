package it.pintux.life.essentialsaddon.shop.backend;

import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ShopBackendRouter {
    private final List<ShopBackend> backends;

    public ShopBackendRouter(List<ShopBackend> backends) {
        List<ShopBackend> copy = new ArrayList<>(backends);
        copy.sort(Comparator.comparingInt(ShopBackend::priority).reversed());
        this.backends = List.copyOf(copy);
    }

    public void bootstrapAll() {
        for (ShopBackend backend : backends) {
            backend.bootstrap();
        }
    }

    public void onCommand(PlayerCommandPreprocessEvent event) {
        for (ShopBackend backend : backends) {
            if (backend.handleCommand(event)) {
                return;
            }
        }
    }

    public void onInventoryOpen(InventoryOpenEvent event) {
        for (ShopBackend backend : backends) {
            if (backend.handleInventoryOpen(event)) {
                return;
            }
        }
    }
}

