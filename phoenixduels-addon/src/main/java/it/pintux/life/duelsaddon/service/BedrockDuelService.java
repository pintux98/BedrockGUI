package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.MapView;
import it.pintux.life.duelsaddon.model.ModeView;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class BedrockDuelService extends BedrockServiceSupport {
    private final Map<UUID, Draft> drafts = new HashMap<>();
    private BedrockInvitationService invitationService;

    public BedrockDuelService(DuelsAddonConfiguration config, DuelsGateway gateway,
                             BedrockPlayerDetector detector) {
        super(config, gateway, detector);
    }

    public void setInvitationService(BedrockInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    public void openDuelPlayer(Player player, String targetName) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        String resolved = targetName;
        if (resolved == null || resolved.isBlank()) {
            openTargetPicker(player);
            return;
        }
        Player target = Bukkit.getPlayerExact(resolved);
        if (target == null) {
            fail(player, "messages.player-not-found");
            return;
        }
        Draft draft = drafts.computeIfAbsent(player.getUniqueId(), id -> new Draft());
        draft.targetName = target.getName();
        if (draft.modeId == null) {
            draft.modeId = firstChallengeMode();
        }
        if (draft.rounds <= 0) {
            draft.rounds = config.defaultRounds();
        }

        String modeName = gateway.mode(draft.modeId).map(ModeView::displayName).orElse("?");
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                render("duel.title", Map.of("player", target.getName())));
        form.content(render("duel.content", Map.of("player", target.getName())));
        form.button(render("duel.button-mode", Map.of("mode", modeName)),
                fp -> openModePicker(player, modeId -> {
                    draft.modeId = modeId;
                    openDuelPlayer(player, draft.targetName);
                }));
        form.button(render("duel.button-rounds", Map.of("rounds", String.valueOf(draft.rounds))),
                fp -> openRoundsInput(player));
        form.button(text("duel.button-send"), fp -> send(player, draft));
        form.button(text("common.close-button"), fp -> {
        });
        form.send(wrap(player));
    }

    public void openTargetPicker(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("duel.create-match-title"));
        form.content(text("duel.create-match-content"));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId()) || gateway.isInMatch(online)) {
                continue;
            }
            form.button(online.getName(), fp -> openDuelPlayer(player, online.getName()));
        }
        form.button(text("common.close-button"), fp -> {
        });
        form.send(wrap(player));
    }

    public void openModePicker(Player player, Consumer<String> onPick) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        List<ModeView> modes = new ArrayList<>();
        for (ModeView mode : gateway.modes()) {
            if (mode.enabled() && mode.challengeAllowed()) {
                modes.add(mode);
            }
        }
        if (modes.isEmpty()) {
            modes.addAll(gateway.modes().stream().filter(ModeView::enabled).toList());
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("duel.select-mode-title"));
        if (modes.isEmpty()) {
            form.content(text("queue.no-modes"));
        } else {
            form.content(text("duel.select-mode-content"));
        }
        for (ModeView mode : modes) {
            boolean allowed = !mode.permissionRequired() || mode.permission() == null
                    || mode.permission().isBlank() || player.hasPermission(mode.permission());
            if (allowed) {
                form.button(mode.displayName(), fp -> onPick.accept(mode.id()));
            } else {
                form.button(render("queue.mode-button-locked", Map.of("mode", mode.displayName())),
                        fp -> fail(player, "messages.no-permission"));
            }
        }
        form.send(wrap(player));
    }

    public void openMapPicker(Player player, String modeId, Consumer<String> onPick) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        List<MapView> maps = gateway.maps(modeId);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("duel.select-map-title"));
        if (maps.isEmpty()) {
            form.content(text("duel.no-maps"));
        } else {
            form.content(text("duel.select-map-content"));
        }
        form.button(text("duel.any-map-button"), fp -> onPick.accept(""));
        for (MapView map : maps) {
            form.button(render("duel.map-button", Map.of("map", map.displayName())), fp -> onPick.accept(map.id()));
        }
        form.send(wrap(player));
    }

    public void openRoundsInput(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        Draft draft = drafts.computeIfAbsent(player.getUniqueId(), id -> new Draft());
        String label = text("duel.rounds-label");
        api.createCustomForm(text("duel.rounds-title"))
                .input(label, "1", String.valueOf(Math.max(1, draft.rounds)))
                .onSubmit(results -> {
                    String raw = formValue(results, label);
                    try {
                        draft.rounds = Math.max(1, Integer.parseInt(raw));
                    } catch (NumberFormatException e) {
                        fail(player, "messages.invalid-input");
                    }
                    openDuelPlayer(player, draft.targetName);
                })
                .send(wrap(player));
    }

    public void openLostItems(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        boolean has = gateway.hasLostItems(player);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("duel.lost-items-title"));
        form.content(has ? text("duel.lost-items-content") : text("duel.lost-items-empty"));
        if (has) {
            form.button(text("duel.lost-items-button"), fp -> {
                if (!gateway.claimLostItems(player)) {
                    fail(player, "messages.action-failed");
                }
            });
        }
        form.button(text("common.close-button"), fp -> {
        });
        form.send(wrap(player));
    }

    private void send(Player player, Draft draft) {
        if (draft.targetName == null || draft.modeId == null) {
            fail(player, "messages.challenge-failed");
            return;
        }
        if (gateway.isInMatch(player)) {
            fail(player, "messages.in-match");
            return;
        }
        if (gateway.challengePlayer(player, draft.targetName, draft.modeId, draft.rounds)) {
            player.sendMessage(config.apply(text("messages.challenge-sent"),
                    Map.of("player", draft.targetName)));
            Player target = Bukkit.getPlayerExact(draft.targetName);
            if (invitationService != null && target != null) {
                invitationService.sendDuelChallenge(target, player.getUniqueId());
            }
        } else {
            fail(player, "messages.challenge-failed");
        }
    }

    private String firstChallengeMode() {
        Optional<ModeView> challenge = gateway.modes().stream()
                .filter(m -> m.enabled() && m.challengeAllowed())
                .findFirst();
        if (challenge.isPresent()) {
            return challenge.get().id();
        }
        return gateway.modes().stream().filter(ModeView::enabled).map(ModeView::id).findFirst().orElse(null);
    }

    public void forget(UUID playerId) {
        drafts.remove(playerId);
    }

    private static final class Draft {
        private String targetName;
        private String modeId;
        private int rounds;
    }
}
