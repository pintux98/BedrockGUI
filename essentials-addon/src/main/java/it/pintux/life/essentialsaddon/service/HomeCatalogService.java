package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.api.HomeProvider;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Routes home reads and writes to the active provider. Results arrive through callbacks, which
 * may run on any thread: see {@link HomeProvider}.
 */
public final class HomeCatalogService {
    private final Logger logger;
    private volatile boolean ready = false;
    private HomeProvider provider;

    public HomeCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(HomeProvider provider) {
        this.provider = provider;
        refresh();
    }

    public synchronized void refresh() {
        ready = provider != null && provider.isReady();
    }

    public boolean isReady() {
        return ready && provider != null;
    }

    public void homeNames(Player player, Consumer<List<String>> callback) {
        if (!isReady()) {
            callback.accept(List.of());
            return;
        }
        provider.homeNames(player, callback);
    }

    public void homeLimit(Player player, IntConsumer callback) {
        if (!isReady()) {
            callback.accept(0);
            return;
        }
        provider.homeLimit(player, callback);
    }

    public void teleportHome(Player player, String homeName, Consumer<Boolean> callback) {
        if (!isReady()) {
            callback.accept(false);
            return;
        }
        provider.teleportHome(player, homeName, callback);
    }

    public void setHome(Player player, String homeName, Consumer<Boolean> callback) {
        if (!isReady()) {
            callback.accept(false);
            return;
        }
        provider.setHome(player, homeName, callback);
    }

    public void deleteHome(Player player, String homeName, Consumer<Boolean> callback) {
        if (!isReady()) {
            callback.accept(false);
            return;
        }
        provider.deleteHome(player, homeName, callback);
    }

    public boolean supportsPublicHomes() {
        return isReady() && provider.supportsPublicHomes();
    }

    public void publicHomes(Player player, Consumer<List<String>> callback) {
        if (!supportsPublicHomes()) {
            callback.accept(List.of());
            return;
        }
        provider.publicHomes(player, callback);
    }

    public void teleportPublicHome(Player player, String identifier, Consumer<Boolean> callback) {
        if (!supportsPublicHomes()) {
            callback.accept(false);
            return;
        }
        provider.teleportPublicHome(player, identifier, callback);
    }

    public HomeProvider getProvider() {
        return provider;
    }
}
