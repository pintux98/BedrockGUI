package it.pintux.life.essentialsaddon.service;

import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.api.TpaProvider;
import it.pintux.life.essentialsaddon.util.MainThread;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Pushes the Accept/Deny form at a Bedrock player as soon as a teleport request arrives.
 *
 * <p>Hooks the backing plugin's own event — {@code TPARequestEvent} on EssentialsX,
 * {@code CMIPlayerTeleportRequestEvent} on CMI — rather than polling on a timer. A provider
 * without such an event simply gets no popup.</p>
 */
public final class TpaRequestNotifier {
    private final Logger logger;
    private final TpaCatalogService tpaCatalog;
    private final BedrockTpaService tpaService;
    private final BedrockPlayerDetector bedrockPlayerDetector;

    public TpaRequestNotifier(
            Logger logger,
            TpaCatalogService tpaCatalog,
            BedrockTpaService tpaService,
            BedrockPlayerDetector bedrockPlayerDetector
    ) {
        this.logger = logger;
        this.tpaCatalog = tpaCatalog;
        this.tpaService = tpaService;
        this.bedrockPlayerDetector = bedrockPlayerDetector;
    }

    /** @return true when the provider's request event was hooked. */
    public boolean register(Plugin plugin) {
        TpaProvider provider = tpaCatalog.getProvider();
        if (provider == null) {
            return false;
        }
        boolean hooked = provider.registerRequestListener(plugin, this::onRequest);
        if (!hooked) {
            logger.info("TPA request popup unavailable: " + provider.getProviderId()
                    + " exposes no teleport-request event.");
        }
        return hooked;
    }

    private void onRequest(org.bukkit.entity.Player target, String senderName) {
        if (target == null || !bedrockPlayerDetector.isBedrockPlayer(target)) {
            return;
        }
        // EssentialsX fires the event before it queues the request, so let the tick finish
        // and the request exist before offering Accept/Deny.
        MainThread.runLater(() -> {
            if (target.isOnline()) {
                tpaService.showIncomingRequestForm(target, senderName);
            }
        }, 1L);
    }
}
