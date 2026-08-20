package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.DuelDraftView;
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

/**
 * Bedrock forms for sending a duel challenge, and for reclaiming items PhoenixDuels saved.
 *
 * <p>PhoenixDuels' own duel menu keeps the half-built challenge in the open inventory's view
 * metadata. A Bedrock form has no equivalent, since each form is a separate round trip, so the
 * mode and round count in progress are held per player in {@link Draft} until the challenge is
 * sent or the player disconnects.</p>
 */
public final class BedrockDuelService extends BedrockServiceSupport {
    private final Map<UUID, Draft> drafts = new HashMap<>();
    private BedrockInvitationService invitationService;

    public BedrockDuelService(DuelsAddonConfiguration config, DuelsGateway gateway,
                             BedrockPlayerDetector detector) {
        super(config, gateway, detector);
    }

    /**
     * Wired after construction because the two services are mutually dependent: this one pushes
     * the challenge form to the target once PhoenixDuels has accepted the challenge.
     */
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

    /**
     * Opens the duel settings for the player PhoenixDuels' menu was already about.
     *
     * <p>{@code /duel <player>} carries the target, so asking for it again would be a step
     * backwards. Mode and rounds are adopted from PhoenixDuels too, so a player who had already
     * picked a mode does not lose it. Falls back to the target picker only when the menu genuinely
     * has no target, which is the {@code /duel} with no argument case.</p>
     *
     * @param containerView the cancelled PhoenixDuels view, passed through opaquely
     */
    public void openFromMenu(Player player, Object containerView) {
        Optional<DuelDraftView> incoming = gateway.duelDraft(containerView);
        if (incoming.isEmpty()) {
            openTargetPicker(player);
            return;
        }
        DuelDraftView view = incoming.get();
        Draft draft = drafts.computeIfAbsent(player.getUniqueId(), id -> new Draft());
        draft.targetName = view.targetName();
        if (view.modeId() != null) {
            draft.modeId = view.modeId();
        }
        draft.rounds = view.rounds();
        openDuelPlayer(player, view.targetName());
    }

    /**
     * Re-opens the mode picker for the challenge already in progress, keeping the target.
     */
    public void openModeSelection(Player player) {
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null || draft.targetName == null) {
            openTargetPicker(player);
            return;
        }
        openModePicker(player, modeId -> {
            draft.modeId = modeId;
            openDuelPlayer(player, draft.targetName);
        });
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
            if (mode.allowedFor(player)) {
                form.button(mode.displayName(), fp -> onPick.accept(mode.id()));
            } else {
                form.button(render("queue.mode-button-locked", Map.of("mode", mode.displayName())),
                        fp -> fail(player, "messages.no-permission"));
            }
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
                String modeName = gateway.mode(draft.modeId).map(ModeView::displayName).orElse(draft.modeId);
                invitationService.sendDuelChallenge(target, player.getUniqueId(),
                        new it.pintux.life.duelsaddon.model.InviteView(player.getName(), modeName,
                                draft.rounds, gateway.invitationExpirationSeconds()));
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

    /**
     * Drops a disconnected player's half-built challenge.
     */
    public void forget(UUID playerId) {
        drafts.remove(playerId);
    }

    /**
     * A challenge being assembled across several forms.
     */
    private static final class Draft {
        private String targetName;
        private String modeId;
        private int rounds;
    }
}
