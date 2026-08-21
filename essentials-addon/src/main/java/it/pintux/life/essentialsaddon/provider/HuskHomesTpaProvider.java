package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.TpaProvider;
import net.william278.huskhomes.HuskHomes;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.event.ReceiveTeleportRequestEvent;
import net.william278.huskhomes.manager.RequestsManager;
import net.william278.huskhomes.teleport.TeleportRequest;
import net.william278.huskhomes.user.OnlineUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Teleport requests served by HuskHomes through its published API.
 *
 * <p>Pending requests are read from HuskHomes' own {@link RequestsManager} rather than tracked
 * here, so the form always shows what HuskHomes actually holds.</p>
 */
public final class HuskHomesTpaProvider implements TpaProvider {
    private final Logger logger;
    private volatile String lastFailure;

    public HuskHomesTpaProvider(Logger logger) {
        this.logger = logger;
        if (Bukkit.getPluginManager().getPlugin("HuskHomes") == null) {
            throw new IllegalStateException("HuskHomes not found");
        }
        HuskHomesAPI.getInstance();
    }

    @Override
    public String getProviderId() {
        return "HuskHomes";
    }

    @Override
    public boolean isReady() {
        return requests() != null;
    }

    @Override
    public boolean registerRequestListener(Plugin plugin, RequestListener listener) {
        try {
            return hookRequestEvent(plugin, listener);
        } catch (Exception | LinkageError failure) {
            // HuskHomes older than 4.1 has no request event: no popup, everything else still works.
            report("This HuskHomes build exposes no teleport-request event", failure);
            return false;
        }
    }

    private boolean hookRequestEvent(Plugin plugin, RequestListener listener) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onReceive(ReceiveTeleportRequestEvent event) {
                OnlineUser recipient = event.getRecipient();
                TeleportRequest request = event.getRequest();
                if (recipient == null || request == null) {
                    return;
                }
                Player target = Bukkit.getPlayerExact(recipient.getUsername());
                if (target != null) {
                    listener.onRequest(target, request.getRequesterName());
                }
            }
        }, plugin);
        return true;
    }

    @Override
    public boolean sendTpaRequest(Player sender, Player target) {
        return send(sender, target.getName(), TeleportRequest.Type.TPA);
    }

    @Override
    public boolean sendTpahereRequest(Player sender, Player target) {
        return send(sender, target.getName(), TeleportRequest.Type.TPA_HERE);
    }

    @Override
    public boolean acceptTpa(Player target) {
        return respond(target, true);
    }

    @Override
    public boolean denyTpa(Player target) {
        return respond(target, false);
    }

    @Override
    public boolean cancelTpa(Player sender) {
        // HuskHomes has no sender-side cancel: a request it holds only expires.
        return false;
    }

    @Override
    public List<String> getPendingRequests(Player player) {
        String sender = getPendingRequestSender(player);
        return sender == null ? List.of() : List.of(sender);
    }

    @Override
    public boolean hasPendingRequest(Player player) {
        return getPendingRequestSender(player) != null;
    }

    @Override
    public String getPendingRequestSender(Player player) {
        RequestsManager requests = requests();
        if (requests == null) {
            return null;
        }
        try {
            OnlineUser user = HuskHomesAPI.getInstance().adaptUser(player);
            Optional<TeleportRequest> request = requests.getLastTeleportRequest(user);
            if (request.isEmpty() || request.get().hasExpired()) {
                return null;
            }
            return request.get().getRequesterName();
        } catch (Throwable failure) {
            report("Could not read the pending request for " + player.getName(), failure);
            return null;
        }
    }

    private boolean send(Player sender, String targetName, TeleportRequest.Type type) {
        RequestsManager requests = requests();
        if (requests == null) {
            return false;
        }
        try {
            OnlineUser user = HuskHomesAPI.getInstance().adaptUser(sender);
            requests.sendTeleportRequest(user, targetName, type, null);
            return true;
        } catch (IllegalArgumentException unknownTarget) {
            return false;
        } catch (Throwable failure) {
            report("Could not send a " + type + " request from " + sender.getName(), failure);
            return false;
        }
    }

    private boolean respond(Player target, boolean accept) {
        RequestsManager requests = requests();
        if (requests == null) {
            return false;
        }
        try {
            OnlineUser user = HuskHomesAPI.getInstance().adaptUser(target);
            if (requests.getLastTeleportRequest(user).isEmpty()) {
                return false;
            }
            requests.respondToTeleportRequest(user, accept);
            return true;
        } catch (Throwable failure) {
            report("Could not answer the pending request for " + target.getName(), failure);
            return false;
        }
    }

    private RequestsManager requests() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
        if (!(plugin instanceof HuskHomes huskHomes) || !plugin.isEnabled()) {
            return null;
        }
        try {
            return huskHomes.getManager().requests();
        } catch (Throwable failure) {
            report("HuskHomes' request manager is unavailable", failure);
            return null;
        }
    }

    /** Logs each distinct failure once, so a broken call is visible without spamming console. */
    private void report(String message, Throwable failure) {
        String detail = failure == null ? message
                : message + ": " + failure.getClass().getSimpleName()
                        + (failure.getMessage() == null ? "" : " - " + failure.getMessage());
        String key = detail.toLowerCase(Locale.ROOT);
        if (key.equals(lastFailure)) {
            return;
        }
        lastFailure = key;
        if (failure == null) {
            logger.warning("HuskHomes TPA provider: " + detail);
        } else {
            logger.log(Level.WARNING, "HuskHomes TPA provider: " + detail, failure);
        }
    }
}
