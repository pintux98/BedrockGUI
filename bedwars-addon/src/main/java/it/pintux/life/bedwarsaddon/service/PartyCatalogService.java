package it.pintux.life.bedwarsaddon.service;

import it.pintux.life.bedwarsaddon.api.PartyProvider;
import it.pintux.life.bedwarsaddon.model.PartyInfo;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public final class PartyCatalogService {
    private final Logger logger;
    private PartyProvider provider;

    public PartyCatalogService(Logger logger) {
        this.logger = logger;
    }

    public void setProvider(PartyProvider provider) {
        this.provider = provider;
    }

    public PartyProvider getProvider() {
        return provider;
    }

    public boolean isReady() {
        return provider != null && provider.isReady();
    }

    public PartyInfo getParty(Player player) {
        return isReady() ? provider.getParty(player) : PartyInfo.none();
    }

    public boolean add(Player requester, String targetName) {
        return isReady() && provider.add(requester, targetName);
    }

    public void leave(Player player) {
        if (isReady()) provider.leave(player);
    }

    public void disband(Player player) {
        if (isReady()) provider.disband(player);
    }

    public boolean kick(Player owner, String targetName) {
        return isReady() && provider.kick(owner, targetName);
    }
}
