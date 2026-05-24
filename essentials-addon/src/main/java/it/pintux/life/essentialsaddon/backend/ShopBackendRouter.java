package it.pintux.life.essentialsaddon.backend;

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
        this.backends = new ArrayList<>(copy);
    }

    public void addBackends(List<ShopBackend> newBackends) {
        List<ShopBackend> combined = new ArrayList<>(backends);
        combined.addAll(newBackends);
        combined.sort(Comparator.comparingInt(ShopBackend::priority).reversed());
        backends.clear();
        backends.addAll(combined);
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

