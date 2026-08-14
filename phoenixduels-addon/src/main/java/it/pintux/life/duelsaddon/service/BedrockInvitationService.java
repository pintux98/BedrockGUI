package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.InviteView;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns PhoenixDuels' clickable-chat invitations into Bedrock accept/decline forms.
 *
 * <p>Bedrock clients cannot click chat components, so the invitation is unusable for them as
 * shipped. The chat line itself is left alone: PhoenixDuels emits it from inside its facades and
 * neither Bukkit nor Paper exposes an outgoing-chat event to cancel, so the form simply arrives on
 * top.</p>
 *
 * <p>A form, once sent, cannot be recalled. So acceptance is re-validated against PhoenixDuels at
 * the moment the button is pressed rather than trusted from when the form was built, and
 * {@link #pending} suppresses duplicates while one is outstanding.</p>
 */
public final class BedrockInvitationService extends BedrockServiceSupport {

    /**
     * {@code kind:inviter:invited} keys for invitations with a form already on screen.
     */
    private final Set<String> pending = ConcurrentHashMap.newKeySet();

    public BedrockInvitationService(DuelsAddonConfiguration config, DuelsGateway gateway,
                                    BedrockPlayerDetector detector) {
        super(config, gateway, detector);
    }

    public void sendPartyInvite(Player invited, UUID leaderId, String leaderName) {
        if (!config.partyInviteFormsEnabled() || !shouldHandle(invited)) {
            return;
        }
        String key = key("party", leaderId, invited.getUniqueId());
        if (!pending.add(key)) {
            return;
        }
        BedrockGUIApi api = requireApi(invited);
        if (api == null) {
            pending.remove(key);
            return;
        }
        Map<String, String> ph = Map.of(
                "leader", leaderName == null ? "?" : leaderName,
                "time", String.valueOf(gateway.invitationExpirationSeconds()));

        api.createModalForm(text("invitations.party-title"), render("invitations.party-content", ph))
                .button1(text("invitations.accept"), fp -> {
                    pending.remove(key);
                    if (gateway.acceptPartyInvitation(invited, leaderId)) {
                        invited.sendMessage(text("messages.invite-accepted"));
                    } else {
                        fail(invited, "messages.invite-expired");
                    }
                })
                .button2(text("invitations.decline"), fp -> {
                    pending.remove(key);
                    if (gateway.declinePartyInvitation(invited, leaderId)) {
                        invited.sendMessage(text("messages.invite-declined"));
                    }
                })
                .send(wrap(invited));
    }

    public void sendDuelChallenge(Player invited, UUID inviterId) {
        if (!config.duelInviteFormsEnabled() || !shouldHandle(invited)) {
            return;
        }
        Optional<InviteView> invite = gateway.pendingChallenge(inviterId, invited.getUniqueId());
        if (invite.isEmpty()) {
            return;
        }
        String key = key("duel", inviterId, invited.getUniqueId());
        if (!pending.add(key)) {
            return;
        }
        BedrockGUIApi api = requireApi(invited);
        if (api == null) {
            pending.remove(key);
            return;
        }
        InviteView view = invite.get();
        Map<String, String> ph = Map.of(
                "player", view.inviterName() == null ? "?" : view.inviterName(),
                "mode", view.modeName() == null ? "?" : view.modeName(),
                "rounds", String.valueOf(view.rounds()),
                "time", String.valueOf(view.expiresInSeconds()));

        api.createModalForm(text("invitations.duel-title"), render("invitations.duel-content", ph))
                .button1(text("invitations.accept"), fp -> {
                    pending.remove(key);
                    if (!gateway.hasPendingChallenge(inviterId, invited.getUniqueId())) {
                        fail(invited, "messages.invite-expired");
                        return;
                    }
                    if (gateway.acceptChallenge(invited, inviterId)) {
                        invited.sendMessage(text("messages.invite-accepted"));
                    } else {
                        fail(invited, "messages.invite-expired");
                    }
                })
                .button2(text("invitations.decline"), fp -> {
                    pending.remove(key);
                    if (gateway.declineChallenge(invited, inviterId)) {
                        invited.sendMessage(text("messages.invite-declined"));
                    }
                })
                .send(wrap(invited));
    }

    /**
     * Clears the duplicate guard once PhoenixDuels reports the invitation settled, so a later
     * invitation between the same two players can raise a new form.
     */
    public void resolve(UUID inviterId, UUID invitedId) {
        pending.remove(key("party", inviterId, invitedId));
        pending.remove(key("duel", inviterId, invitedId));
    }

    public void forget(UUID playerId) {
        String suffix = ":" + playerId;
        pending.removeIf(key -> key.endsWith(suffix) || key.contains(":" + playerId + ":"));
    }

    public void clear() {
        pending.clear();
    }

    private static String key(String kind, UUID inviterId, UUID invitedId) {
        return kind + ":" + inviterId + ":" + invitedId;
    }
}
