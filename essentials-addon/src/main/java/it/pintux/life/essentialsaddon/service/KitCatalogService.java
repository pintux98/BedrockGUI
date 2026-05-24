package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.api.KitProvider;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

public final class KitCatalogService {
    private final Logger logger;
    private volatile List<String> kitNames = List.of();
    private volatile boolean ready = false;
    private KitProvider provider;

    public KitCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(KitProvider provider) {
        this.provider = provider;
        refresh();
    }

    public synchronized void refresh() {
        if (provider == null || !provider.isReady()) {
            ready = false;
            kitNames = List.of();
            return;
        }
        try {
            List<String> kits = new ArrayList<>(provider.getKitNames());
            kits.sort(String.CASE_INSENSITIVE_ORDER);
            this.kitNames = List.copyOf(kits);
            this.ready = true;
        } catch (Exception e) {
            logger.warning("Unable to refresh kit catalog: " + e.getMessage());
            ready = false;
        }
    }

    public boolean isReady() {
        return ready && !kitNames.isEmpty();
    }

    public List<String> getAccessibleKits(Player player) {
        if (!isReady()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String kitName : kitNames) {
            if (provider.hasAccess(player, kitName)) {
                result.add(kitName);
            }
        }
        return result;
    }

    public String getDisplayName(String kitName) {
        if (provider == null) {
            return kitName;
        }
        return provider.getDisplayName(kitName);
    }

    public int getItemCount(String kitName) {
        if (provider == null) {
            return -1;
        }
        return provider.getItemCount(kitName);
    }

    public long getCooldownSeconds(Player player, String kitName) {
        if (provider == null) {
            return 0;
        }
        return provider.getCooldownSeconds(player, kitName);
    }

    public boolean isAvailable(Player player, String kitName) {
        if (provider == null) {
            return false;
        }
        return provider.isAvailable(player, kitName);
    }

    public KitProvider getProvider() {
        return provider;
    }
}
