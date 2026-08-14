package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.MatchView;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BedrockSpectatorService extends BedrockServiceSupport {

    public BedrockSpectatorService(DuelsAddonConfiguration config, DuelsGateway gateway,
                                   BedrockPlayerDetector detector) {
        super(config, gateway, detector);
    }

    public void openMatches(Player player, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        List<MatchView> matches = gateway.ongoingMatches();

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("spectator.title"));
        if (matches.isEmpty()) {
            form.content(text("spectator.empty"));
        } else {
            form.content(text("spectator.content"));
        }

        Pagination pagination = new Pagination(matches.size(), page);
        for (int i = pagination.start; i < pagination.end; i++) {
            MatchView match = matches.get(i);
            form.button(render("spectator.match-button", Map.of(
                            "mode", match.modeName(),
                            "players", String.join(" vs ", match.playerNames()),
                            "round", String.valueOf(match.currentRound()))),
                    fp -> openMatchPlayers(player, match.anyPlayerId()));
        }
        pagination.addNav(form, p -> openMatches(player, p));
        form.button(text("common.close-button"), fp -> {
        });
        form.send(wrap(player));
    }

    public void openMatchPlayers(Player player, UUID anyPlayerInMatch) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        MatchView match = findMatch(anyPlayerInMatch);
        if (match == null) {
            fail(player, "messages.no-matches");
            return;
        }
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("spectator.players-title"));
        form.content(text("spectator.players-content"));
        for (String name : match.playerNames()) {
            form.button(render("spectator.player-button", Map.of(
                    "player", name,
                    "mode", match.modeName())), fp -> spectate(player, match.anyPlayerId()));
        }
        form.button(text("common.back-button"), fp -> openMatches(player, 1));
        form.send(wrap(player));
    }

    public void spectate(Player player, UUID anyPlayerInMatch) {
        if (!ensureAvailable(player)) {
            return;
        }
        if (gateway.isInMatch(player)) {
            fail(player, "messages.in-match");
            return;
        }
        if (!gateway.spectate(player, anyPlayerInMatch)) {
            fail(player, "messages.spectate-failed");
        }
    }

    private MatchView findMatch(UUID anyPlayerInMatch) {
        for (MatchView match : gateway.ongoingMatches()) {
            if (match.anyPlayerId().equals(anyPlayerInMatch)) {
                return match;
            }
        }
        return null;
    }
}
