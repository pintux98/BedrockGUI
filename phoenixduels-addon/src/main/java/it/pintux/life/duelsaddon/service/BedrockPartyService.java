package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.MemberView;
import it.pintux.life.duelsaddon.model.ModeView;
import it.pintux.life.duelsaddon.model.PartyView;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Bedrock forms for the whole party flow: creating, inviting, managing members, and starting the
 * team fight, multi-team and free-for-all matches.
 *
 * <p>PhoenixDuels keeps unanswered invitations inside the party as not-yet-accepted participants,
 * so the member list shows them as pending rather than as members, and they are excluded from the
 * counts that decide whether a fight can start.</p>
 */
public final class BedrockPartyService extends BedrockServiceSupport {

    public BedrockPartyService(DuelsAddonConfiguration config, DuelsGateway gateway,
                               BedrockPlayerDetector detector) {
        super(config, gateway, detector);
    }

    public void openMain(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        Optional<PartyView> party = gateway.party(player);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("party.title"));

        if (party.isEmpty()) {
            form.content(text("party.no-party-content"));
            form.button(text("party.button-create"), fp -> {
                if (gateway.createParty(player)) {
                    player.sendMessage(text("messages.party-created"));
                    openMain(player);
                } else {
                    fail(player, "messages.party-create-failed");
                }
            });
            form.button(text("common.close-button"), fp -> {
            });
            form.send(wrap(player));
            return;
        }

        PartyView view = party.get();
        boolean leader = view.isLeader(player.getUniqueId());
        form.content(render("party.content", Map.of(
                "leader", view.leaderName(),
                "members", String.valueOf(view.memberCount()),
                "slots", String.valueOf(view.maximumSlots()))));

        form.button(text("party.button-info"), fp -> openInfo(player, 1));
        if (leader) {
            form.button(text("party.button-invite"), fp -> openInvitePicker(player));
            form.button(text("party.button-fight"), fp -> openTeamFight(player));
            form.button(text("party.button-multiteam"), fp -> openMultiTeam(player));
            form.button(text("party.button-ffa"), fp -> openFfa(player));
            form.button(text("party.button-challenge"), fp -> openChallengeOpponent(player, 1));
            form.button(text("party.button-disband"), fp -> confirmDisband(player));
        } else {
            form.button(text("party.button-leave"), fp -> confirmLeave(player));
        }
        form.button(text("common.close-button"), fp -> {
        });
        form.send(wrap(player));
    }

    public void openInfo(Player player, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        Optional<PartyView> party = gateway.party(player);
        if (party.isEmpty()) {
            fail(player, "messages.party-none");
            return;
        }
        PartyView view = party.get();
        boolean leader = view.isLeader(player.getUniqueId());

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("party.info-title"));
        form.content(text("party.info-content"));

        List<MemberView> members = view.members();
        Pagination pagination = new Pagination(members.size(), page);
        for (int i = pagination.start; i < pagination.end; i++) {
            MemberView member = members.get(i);
            if (member.pending()) {
                form.button(render("party.member-pending-button", Map.of("player", member.playerName())), fp -> {
                });
                continue;
            }
            String label = render(member.online() ? "party.member-button" : "party.member-offline-button",
                    Map.of("player", member.playerName(),
                            "leader_tag", member.leader() ? text("party.leader-tag") : ""));
            if (leader && !member.leader()) {
                form.button(label, fp -> openManageMember(player, member.playerId()));
            } else {
                form.button(label, fp -> openInfo(player, page));
            }
        }
        pagination.addNav(form, p -> openInfo(player, p));
        form.button(text("common.back-button"), fp -> openMain(player));
        form.send(wrap(player));
    }

    public void openManageMember(Player player, UUID memberId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        Optional<PartyView> party = gateway.party(player);
        if (party.isEmpty() || !party.get().isLeader(player.getUniqueId())) {
            fail(player, "messages.party-not-leader");
            return;
        }
        MemberView member = findMember(party.get(), memberId);
        if (member == null) {
            openInfo(player, 1);
            return;
        }
        Map<String, String> ph = Map.of("player", member.playerName());
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(render("party.manage-title", ph));
        form.content(text("party.manage-content"));
        form.button(text("party.button-promote"), fp -> confirmPromote(player, member));
        form.button(text("party.button-kick"), fp -> confirmKick(player, member));
        form.button(text("common.back-button"), fp -> openInfo(player, 1));
        form.send(wrap(player));
    }

    public void openInvitePicker(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        Optional<PartyView> party = gateway.party(player);
        if (party.isEmpty() || !party.get().isLeader(player.getUniqueId())) {
            fail(player, "messages.party-not-leader");
            return;
        }
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            if (gateway.partyOf(online.getUniqueId()).isPresent()) {
                continue;
            }
            candidates.add(online);
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("party.invite-title"));
        form.content(text("party.invite-online-content"));
        for (Player candidate : candidates) {
            form.button(candidate.getName(), fp -> invite(player, candidate.getName()));
        }
        form.button(text("party.invite-manual-button"), fp -> openInviteInput(player));
        form.button(text("common.back-button"), fp -> openMain(player));
        form.send(wrap(player));
    }

    public void openInviteInput(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        String label = text("party.invite-label");
        api.createCustomForm(text("party.invite-title"))
                .input(label, text("party.invite-placeholder"), "")
                .onSubmit(results -> {
                    String name = formValue(results, label);
                    if (name.isBlank()) {
                        fail(player, "messages.invalid-input");
                        return;
                    }
                    invite(player, name);
                })
                .send(wrap(player));
    }

    public void openTeamFight(Player player) {
        openModePicker(player, "party.teamfight-title", "party.teamfight-content",
                ModeView::challengeAllowed, "queue.no-modes", modeId -> {
                    if (!gateway.startPartyTeamFight(player, modeId)) {
                        fail(player, "messages.action-failed");
                    }
                });
    }

    /**
     * Free-for-all is opt-in per mode in PhoenixDuels, so the picker only offers modes that allow
     * it rather than failing after the player has already chosen.
     */
    public void openFfa(Player player) {
        openModePicker(player, "party.ffa-title", "party.ffa-content",
                ModeView::ffaAllowed, "party.ffa-no-modes", modeId -> {
                    if (!gateway.startPartyFfa(player, modeId)) {
                        fail(player, "messages.action-failed");
                    }
                });
    }

    public void openMultiTeam(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        Optional<PartyView> party = gateway.party(player);
        if (party.isEmpty() || !party.get().isLeader(player.getUniqueId())) {
            fail(player, "messages.party-not-leader");
            return;
        }
        int members = party.get().memberCount();
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("party.multiteam-title"));
        form.content(text("party.multiteam-content"));
        boolean any = false;
        for (int teams = 2; teams <= 4; teams++) {
            if (members < teams) {
                continue;
            }
            any = true;
            int chosen = teams;
            form.button(render("party.multiteam-button", Map.of("teams", String.valueOf(teams))),
                    fp -> openModePicker(player, "party.multiteam-title", "party.multiteam-content",
                            ModeView::challengeAllowed, "queue.no-modes",
                            modeId -> {
                                if (!gateway.startPartyMultiTeamFight(player, chosen, modeId)) {
                                    fail(player, "messages.action-failed");
                                }
                            }));
        }
        if (!any) {
            form.content(text("party.too-few-members"));
        }
        form.button(text("common.back-button"), fp -> openMain(player));
        form.send(wrap(player));
    }

    public void openSpectators(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        Optional<PartyView> party = gateway.party(player);
        if (party.isEmpty()) {
            fail(player, "messages.party-none");
            return;
        }
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("party.spectators-title"));
        form.content(text("party.spectators-content"));
        for (MemberView member : party.get().members()) {
            if (member.pending()) {
                continue;
            }
            form.button(render("party.spectator-button", Map.of("player", member.playerName())), fp -> {
            });
        }
        form.button(text("common.back-button"), fp -> openMain(player));
        form.send(wrap(player));
    }

    public void openChallengeOpponent(Player player, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        Optional<PartyView> own = gateway.party(player);
        if (own.isEmpty() || !own.get().isLeader(player.getUniqueId())) {
            fail(player, "messages.party-not-leader");
            return;
        }
        List<PartyView> opponents = gateway.otherParties(player);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("duel.challenge-opponent-title"));
        if (opponents.isEmpty()) {
            form.content(text("duel.no-opponents"));
        } else {
            form.content(text("duel.challenge-opponent-content"));
        }

        Pagination pagination = new Pagination(opponents.size(), page);
        for (int i = pagination.start; i < pagination.end; i++) {
            PartyView opponent = opponents.get(i);
            form.button(render("duel.opponent-button", Map.of(
                            "leader", opponent.leaderName(),
                            "members", String.valueOf(opponent.memberCount()))),
                    fp -> openModePicker(player, "duel.challenge-opponent-title", "duel.select-mode-content",
                            ModeView::challengeAllowed, "queue.no-modes",
                            modeId -> {
                                ModeView mode = gateway.mode(modeId).orElse(null);
                                int rounds = mode == null ? config.defaultRounds() : mode.roundsToWin();
                                if (gateway.challengeParty(player, opponent.leaderId(), modeId, rounds)) {
                                    player.sendMessage(config.apply(text("messages.challenge-sent"),
                                            Map.of("player", opponent.leaderName())));
                                } else {
                                    fail(player, "messages.challenge-failed");
                                }
                            }));
        }
        pagination.addNav(form, p -> openChallengeOpponent(player, p));
        form.button(text("common.back-button"), fp -> openMain(player));
        form.send(wrap(player));
    }

    /**
     * Shared mode picker for the party fight flows.
     *
     * @param eligible    which modes this flow accepts, checked before the player picks
     * @param emptyPath   config key explaining why the list is empty
     * @param onPick      receives the chosen mode id
     */
    private void openModePicker(Player player, String titlePath, String contentPath,
                                Predicate<ModeView> eligible, String emptyPath, Consumer<String> onPick) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        List<ModeView> modes = new ArrayList<>();
        for (ModeView mode : gateway.modes()) {
            if (mode.enabled() && eligible.test(mode) && mode.allowedFor(player)) {
                modes.add(mode);
            }
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text(titlePath));
        form.content(text(modes.isEmpty() ? emptyPath : contentPath));
        for (ModeView mode : modes) {
            form.button(mode.displayName(), fp -> onPick.accept(mode.id()));
        }
        form.button(text("common.back-button"), fp -> openMain(player));
        form.send(wrap(player));
    }

    private void invite(Player player, String targetName) {
        if (gateway.invitePlayer(player, targetName)) {
            player.sendMessage(config.apply(text("messages.party-invited"), Map.of("player", targetName)));
        } else {
            fail(player, "messages.party-invite-failed");
        }
    }

    private void confirmDisband(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        api.createModalForm(text("party.disband-title"), text("party.disband-content"))
                .button1(text("party.button-disband"), fp -> {
                    if (gateway.disbandParty(player)) {
                        player.sendMessage(text("messages.party-disbanded"));
                    } else {
                        fail(player, "messages.action-failed");
                    }
                })
                .button2(text("common.confirm-no"), fp -> openMain(player))
                .send(wrap(player));
    }

    private void confirmLeave(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        api.createModalForm(text("party.leave-title"), text("party.leave-content"))
                .button1(text("party.button-leave"), fp -> {
                    if (gateway.leaveParty(player)) {
                        player.sendMessage(text("messages.party-left"));
                    } else {
                        fail(player, "messages.action-failed");
                    }
                })
                .button2(text("common.confirm-no"), fp -> openMain(player))
                .send(wrap(player));
    }

    private void confirmKick(Player player, MemberView member) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        Map<String, String> ph = Map.of("player", member.playerName());
        api.createModalForm(render("party.kick-title", ph), text("party.kick-content"))
                .button1(text("party.button-kick"), fp -> {
                    if (gateway.kickMember(player, member.playerId())) {
                        player.sendMessage(config.apply(text("messages.party-kicked"), ph));
                    } else {
                        fail(player, "messages.action-failed");
                    }
                    openInfo(player, 1);
                })
                .button2(text("common.confirm-no"), fp -> openManageMember(player, member.playerId()))
                .send(wrap(player));
    }

    private void confirmPromote(Player player, MemberView member) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        Map<String, String> ph = Map.of("player", member.playerName());
        api.createModalForm(render("party.promote-title", ph), text("party.promote-content"))
                .button1(text("party.button-promote"), fp -> {
                    if (gateway.promoteMember(player, member.playerId())) {
                        player.sendMessage(config.apply(text("messages.party-promoted"), ph));
                    } else {
                        fail(player, "messages.action-failed");
                    }
                    openMain(player);
                })
                .button2(text("common.confirm-no"), fp -> openManageMember(player, member.playerId()))
                .send(wrap(player));
    }

    private static MemberView findMember(PartyView party, UUID memberId) {
        for (MemberView member : party.members()) {
            if (member.playerId() != null && member.playerId().equals(memberId)) {
                return member;
            }
        }
        return null;
    }
}
