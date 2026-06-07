package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.UpgradeProvider;
import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.UpgradeContent;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Logger;

public final class UpgradeCatalogService {
    private final Logger logger;
    private UpgradeProvider provider;

    public UpgradeCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(UpgradeProvider provider) {
        this.provider = provider;
    }

    public UpgradeProvider getProvider() {
        return provider;
    }

    public boolean isReady() {
        return provider != null && provider.isReady();
    }

    public boolean isWatching(Player player) {
        return isReady() && provider.isWatchingUpgradeGui(player);
    }

    public void stopWatching(Player player) {
        if (isReady()) provider.stopWatching(player);
    }

    public List<UpgradeContent> getUpgrades(Player player) {
        return isReady() ? provider.getUpgrades(player) : List.of();
    }

    public PurchaseResult purchase(Player player, String upgradeId) {
        if (!isReady()) return PurchaseResult.fail("provider unavailable");
        return provider.purchase(player, upgradeId);
    }
}
