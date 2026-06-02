package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.provider.CrateProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Logger;

public final class CrateCatalogService {
    private final Logger logger;
    private volatile List<String> crateNames = List.of();
    private volatile boolean ready = false;
    private CrateProvider provider;

    public CrateCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(CrateProvider provider) {
        this.provider = provider;
        refresh();
    }

    public synchronized void refresh() {
        if (provider == null || !provider.isReady()) {
            ready = false;
            crateNames = List.of();
            return;
        }
        try {
            List<String> crates = new ArrayList<>(provider.getCrateNames());
            crates.sort(String.CASE_INSENSITIVE_ORDER);
            this.crateNames = List.copyOf(crates);
            this.ready = true;
        } catch (Exception e) {
            logger.warning("Unable to refresh crate catalog: " + e.getMessage());
            ready = false;
        }
    }

    public boolean isReady() {
        return ready && !crateNames.isEmpty();
    }

    public List<String> getAccessibleCrates(Player player) {
        if (!isReady()) return List.of();
        List<String> result = new ArrayList<>();
        for (String crateId : crateNames) {
            if (provider.hasAccess(player, crateId)) {
                result.add(crateId);
            }
        }
        return result;
    }

    public String getDisplayName(String crateId) {
        return provider != null ? provider.getDisplayName(crateId) : crateId;
    }

    public String getDescription(String crateId) {
        return provider != null ? provider.getDescription(crateId) : "";
    }

    public ItemStack getCrateIcon(String crateId) {
        return provider != null ? provider.getCrateIcon(crateId) : null;
    }

    public List<ItemStack> getPreviewContents(String crateId) {
        return provider != null ? provider.getPreviewContents(crateId) : List.of();
    }

    public CrateProvider getProvider() {
        return provider;
    }
}
