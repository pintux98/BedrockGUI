package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.api.TpaProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

public final class TpaCatalogService {
    private final Logger logger;
    private volatile boolean ready = false;
    private TpaProvider provider;

    public TpaCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(TpaProvider provider) {
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

    public boolean sendTpa(Player sender, String targetName) {
        if (!isReady()) return false;
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) return false;
        return provider.sendTpaRequest(sender, target);
    }

    public boolean sendTpahere(Player sender, String targetName) {
        if (!isReady()) return false;
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) return false;
        return provider.sendTpahereRequest(sender, target);
    }

    public boolean acceptTpa(Player player) {
        if (!isReady()) return false;
        return provider.acceptTpa(player);
    }

    public boolean denyTpa(Player player) {
        if (!isReady()) return false;
        return provider.denyTpa(player);
    }

    public boolean cancelTpa(Player player) {
        if (!isReady()) return false;
        return provider.cancelTpa(player);
    }

    public List<String> getPendingRequests(Player player) {
        if (!isReady()) return List.of();
        return provider.getPendingRequests(player);
    }

    public boolean hasPendingRequest(Player player) {
        if (!isReady()) return false;
        return provider.hasPendingRequest(player);
    }

    public String getPendingRequestSender(Player player) {
        if (!isReady()) return null;
        return provider.getPendingRequestSender(player);
    }

    public TpaProvider getProvider() {
        return provider;
    }
}
