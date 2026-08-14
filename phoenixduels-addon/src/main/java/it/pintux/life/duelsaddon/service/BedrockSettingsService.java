package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import org.bukkit.entity.Player;

/**
 * The duel-settings toggle form.
 *
 * <p>PhoenixDuels stores these as "rejecting" flags; the form presents them positively as
 * "accept requests", so both toggles are inverted on read and on write.</p>
 */
public final class BedrockSettingsService extends BedrockServiceSupport {

    public BedrockSettingsService(DuelsAddonConfiguration config, DuelsGateway gateway,
                                  BedrockPlayerDetector detector) {
        super(config, gateway, detector);
    }

    public void openSettings(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        String duelLabel = text("settings.toggle-duel-requests");
        String partyLabel = text("settings.toggle-party-requests");

        api.createCustomForm(text("settings.title"))
                .toggle(duelLabel, !gateway.isRejectingDuelRequests(player))
                .toggle(partyLabel, !gateway.isRejectingPartyRequests(player))
                .onSubmit(results -> {
                    gateway.setRejectingDuelRequests(player, !formToggle(results, duelLabel));
                    gateway.setRejectingPartyRequests(player, !formToggle(results, partyLabel));
                    player.sendMessage(text("settings.saved"));
                })
                .send(wrap(player));
    }
}
