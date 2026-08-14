package it.pintux.life.essentialsaddon.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Contract for TPA providers (EssentialsX, CMI, etc.).
 * Implement to add support for new teleport-request plugins.
 */
public interface TpaProvider {

    /** Notified when a teleport request lands on {@code target}. */
    @FunctionalInterface
    interface RequestListener {
        void onRequest(Player target, String senderName);
    }

    /**
     * Hooks the backing plugin's own "teleport request received" event so the Bedrock popup can
     * be pushed the moment a request arrives.
     *
     * @return false when the plugin exposes no such event, in which case the popup is skipped —
     * we deliberately do not fall back to a polling task.
     */
    default boolean registerRequestListener(Plugin plugin, RequestListener listener) {
        return false;
    }

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
