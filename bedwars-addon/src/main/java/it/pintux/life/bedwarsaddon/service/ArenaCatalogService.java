package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.ArenaProvider;
import it.pintux.life.bedwarsaddon.model.ArenaInfo;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.logging.Logger;

public final class ArenaCatalogService {
    private final Logger logger;
    private ArenaProvider provider;

    public ArenaCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(ArenaProvider provider) {
        this.provider = provider;
    }

    public ArenaProvider getProvider() {
        return provider;
    }

    public boolean isReady() {
        return provider != null && provider.isReady();
    }

    public List<ArenaInfo> getArenas() {
        return isReady() ? provider.getArenas() : List.of();
    }

    public boolean join(Player player, String arenaName) {
        return isReady() && provider.join(player, arenaName);
    }

    public boolean ownsInventory(Inventory inventory) {
        return isReady() && provider.ownsInventory(inventory);
    }
}
