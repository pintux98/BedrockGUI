package it.pintux.life.essentialsaddon.api;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Contract for TPA providers (EssentialsX, CMI, etc.).
 * Implement to add support for new teleport-request plugins.
 */
public interface TpaProvider {
    String getProviderId();
    boolean isReady();
    boolean sendTpaRequest(Player sender, Player target);
    boolean sendTpahereRequest(Player sender, Player target);
    boolean acceptTpa(Player target);
    boolean denyTpa(Player target);
    boolean cancelTpa(Player sender);
    List<String> getPendingRequests(Player player);
    boolean hasPendingRequest(Player player);
    String getPendingRequestSender(Player player);
}
