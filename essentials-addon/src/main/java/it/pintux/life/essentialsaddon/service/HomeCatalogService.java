package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.api.HomeProvider;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

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
        if (provider == null || !provider.isReady()) {
            ready = false;
            return;
        }
        ready = true;
    }

    public boolean isReady() {
        return ready && provider != null;
    }

    public List<String> getAccessibleHomes(Player player) {
        if (!isReady()) return List.of();
        return provider.getHomeNames(player);
    }

    public Location getHomeLocation(Player player, String homeName) {
        if (!isReady()) return null;
        return provider.getHomeLocation(player, homeName);
    }

    public boolean teleportHome(Player player, String homeName) {
        if (!isReady()) return false;
        return provider.teleportHome(player, homeName);
    }

    public boolean setHome(Player player, String homeName) {
        if (!isReady()) return false;
        return provider.setHome(player, homeName);
    }

    public boolean deleteHome(Player player, String homeName) {
        if (!isReady()) return false;
        return provider.deleteHome(player, homeName);
    }

    public int getMaxHomes(Player player) {
        if (!isReady()) return 0;
        return provider.getMaxHomes(player);
    }

    public int getHomeCount(Player player) {
        if (!isReady()) return 0;
        return provider.getHomeCount(player);
    }

    public HomeProvider getProvider() {
        return provider;
    }
}
