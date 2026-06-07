package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.SpectatorProvider;
import it.pintux.life.bedwarsaddon.model.SpectateTarget;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Logger;

public final class SpectatorCatalogService {
    private final Logger logger;
    private SpectatorProvider provider;

    public SpectatorCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(SpectatorProvider provider) {
        this.provider = provider;
    }

    public SpectatorProvider getProvider() {
        return provider;
    }

    public boolean isReady() {
        return provider != null && provider.isReady();
    }

    public List<SpectateTarget> getTargets(Player spectator) {
        return isReady() ? provider.getTargets(spectator) : List.of();
    }

    public boolean teleport(Player spectator, String targetUuid) {
        return isReady() && provider.teleport(spectator, targetUuid);
    }
}
