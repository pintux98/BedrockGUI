package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.ModeView;
import it.pintux.life.duelsaddon.model.TeamSize;
import it.pintux.life.duelsaddon.util.AddonText;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bedrock forms for the queue flow: ladder, then team size, then mode.
 *
 * <p>Team sizes with no eligible mode are omitted rather than shown and then rejected, and the
 * root form collapses to a leave-queue view while the player is already queued, since PhoenixDuels
 * only allows one queue at a time.</p>
 */
public final class BedrockQueueService extends BedrockServiceSupport {
    private final BedrockPartyService partyService;
    private final BedrockStatsService statsService;

    public BedrockQueueService(DuelsAddonConfiguration config, DuelsGateway gateway,
                               BedrockPlayerDetector detector,
                               BedrockPartyService partyService,
                               BedrockStatsService statsService) {
        super(config, gateway, detector);
        this.partyService = partyService;
        this.statsService = statsService;
    }

    public void openMain(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("queue.title"));

        Optional<String> queued = gateway.queuedModeId(player);
        if (gateway.isInQueue(player)) {
            String modeName = queued.flatMap(gateway::mode).map(ModeView::displayName).orElse("?");
            form.content(render("queue.in-queue-content", Map.of(
                    "mode", modeName,
                    "waiting", String.valueOf(waitingFor(queued.orElse(null))))));
            form.button(render("queue.button-leave", Map.of("mode", modeName)), fp -> {
                if (gateway.leaveQueue(player)) {
                    AddonText.send(player, text("messages.queue-left"));
                } else {
                    fail(player, "messages.not-in-queue");
                }
            });
        } else {
            form.content(text("queue.content"));
            form.button(text("queue.button-unranked"), fp -> openPlayerModes(player, false));
            form.button(text("queue.button-ranked"), fp -> openPlayerModes(player, true));
        }

        form.button(text("queue.button-party"), fp -> partyService.openMain(player));
        form.button(text("queue.button-stats"), fp -> statsService.openMain(player));
        form.button(text("common.close-button"), fp -> {
        });
        form.send(wrap(player));
    }

    public void openPlayerModes(Player player, boolean ranked) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        String type = text(ranked ? "queue.label-ranked" : "queue.label-unranked");
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                render("queue.player-mode-title", Map.of("type", type)));
        form.content(text("queue.player-mode-content"));

        addSizeButton(form, player, ranked, TeamSize.SOLO, "queue.button-solo");
        addSizeButton(form, player, ranked, TeamSize.DUO, "queue.button-duo");
        addSizeButton(form, player, ranked, TeamSize.TRIO, "queue.button-trio");
        addSizeButton(form, player, ranked, TeamSize.QUAD, "queue.button-quad");

        form.button(text("common.back-button"), fp -> openMain(player));
        form.send(wrap(player));
    }

    public void openModes(Player player, boolean ranked, TeamSize size, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        String type = text(ranked ? "queue.label-ranked" : "queue.label-unranked");
        List<ModeView> modes = gateway.modesFor(ranked, size);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(render("queue.mode-title", Map.of(
                "type", type,
                "size", size.key())));
        if (modes.isEmpty()) {
            form.content(text("queue.no-modes"));
        } else {
            form.content(text("queue.mode-content"));
        }

        Pagination pagination = new Pagination(modes.size(), page);
        for (int i = pagination.start; i < pagination.end; i++) {
            ModeView mode = modes.get(i);
            String summary = mode.summary();
            Map<String, String> ph = Map.of(
                    "mode", mode.displayName(),
                    "summary", summary,
                    "players", String.valueOf(gateway.queuedPlayers(ranked, size, mode.id())));
            if (!mode.allowedFor(player)) {
                form.button(render("queue.mode-button-locked", ph), fp -> fail(player, "messages.no-permission"));
            } else if (summary.isEmpty()) {
                form.button(render("queue.mode-button", ph), fp -> join(player, ranked, size, mode));
            } else {
                form.button(render("queue.mode-button-described", ph), fp -> join(player, ranked, size, mode));
            }
        }
        pagination.addNav(form, p -> openModes(player, ranked, size, p));
        form.button(text("common.back-button"), fp -> openPlayerModes(player, ranked));
        form.send(wrap(player));
    }

    public void joinById(Player player, boolean ranked, TeamSize size, String modeId) {
        if (!ensureAvailable(player)) {
            return;
        }
        Optional<ModeView> mode = gateway.mode(modeId);
        if (mode.isEmpty()) {
            fail(player, "messages.mode-not-found");
            return;
        }
        join(player, ranked, size, mode.get());
    }

    private void join(Player player, boolean ranked, TeamSize size, ModeView mode) {
        if (gateway.isInMatch(player)) {
            fail(player, "messages.in-match");
            return;
        }
        if (gateway.joinQueue(player, ranked, size, mode.id())) {
            AddonText.send(player, config.apply(text("messages.queue-joined"), Map.of("mode", mode.displayName())));
        } else {
            fail(player, "messages.queue-failed");
        }
    }

    private void addSizeButton(BedrockGUIApi.SimpleFormBuilder form, Player player, boolean ranked,
                               TeamSize size, String path) {
        if (gateway.modesFor(ranked, size).isEmpty()) {
            return;
        }
        form.button(text(path), fp -> openModes(player, ranked, size, 1));
    }

    private int waitingFor(String modeId) {
        if (modeId == null) {
            return 0;
        }
        int total = 0;
        for (TeamSize size : TeamSize.values()) {
            total += gateway.queuedPlayers(false, size, modeId);
            total += gateway.queuedPlayers(true, size, modeId);
        }
        return total;
    }
}
