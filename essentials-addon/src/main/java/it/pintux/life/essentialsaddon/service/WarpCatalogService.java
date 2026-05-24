package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.api.WarpProvider;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

public final class WarpCatalogService {
    private final Logger logger;
    private volatile List<String> warpNames = List.of();
    private volatile boolean ready = false;
    private WarpProvider provider;

    public WarpCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(WarpProvider provider) {
        this.provider = provider;
        refresh();
    }

    public synchronized void refresh() {
        if (provider == null || !provider.isReady()) {
            ready = false;
            warpNames = List.of();
            return;
        }
        try {
            List<String> warps = new ArrayList<>(provider.getWarpNames());
            warps.sort(String.CASE_INSENSITIVE_ORDER);
            this.warpNames = List.copyOf(warps);
            this.ready = true;
        } catch (Exception e) {
            logger.warning("Unable to refresh warp catalog: " + e.getMessage());
            ready = false;
        }
    }

    public boolean isReady() {
        return ready && !warpNames.isEmpty();
    }

    public List<String> getAccessibleWarps(Player player) {
        if (!isReady()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String warpName : warpNames) {
            if (provider.hasAccess(player, warpName)) {
                result.add(warpName);
            }
        }
        return result;
    }

    public String getDisplayName(String warpName) {
        if (provider == null) {
            return warpName;
        }
        return provider.getDisplayName(warpName);
    }

    public WarpProvider getProvider() {
        return provider;
    }
}
