package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.StatsProvider;
import it.pintux.life.bedwarsaddon.model.PlayerStatsInfo;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public final class StatsCatalogService {
    private final Logger logger;
    private StatsProvider provider;

    public StatsCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(StatsProvider provider) {
        this.provider = provider;
    }

    public StatsProvider getProvider() {
        return provider;
    }

    public boolean isReady() {
        return provider != null && provider.isReady();
    }

    public PlayerStatsInfo getStats(Player player) {
        return isReady() ? provider.getStats(player) : null;
    }
}
