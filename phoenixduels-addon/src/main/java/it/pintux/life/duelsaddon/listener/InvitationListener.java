package it.pintux.life.duelsaddon.listener;

import com.phoenixplugins.phoenixduels.api.events.party.PartyDisbandEvent;
import com.phoenixplugins.phoenixduels.api.events.party.PartyParticipantAcceptedEvent;
import com.phoenixplugins.phoenixduels.api.events.party.PartyParticipantAddedEvent;
import com.phoenixplugins.phoenixduels.api.events.party.PartyParticipantRemovedEvent;
import com.phoenixplugins.phoenixduels.api.participable.Participant;
import com.phoenixplugins.phoenixduels.api.party.Party;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.service.BedrockDuelService;
import it.pintux.life.duelsaddon.service.BedrockInvitationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.UUID;

/**
 * Watches PhoenixDuels for invitations that need a Bedrock form.
 *
 * <p>Party invitations have a real event. Duel challenges do not, so the direct
 * {@code /duel <player> <mode> <rounds>} form is observed at {@link EventPriority#MONITOR} and
 * deliberately <em>not</em> cancelled: PhoenixDuels must still process it, and only afterwards can
 * the invitation be confirmed and the form pushed. Challenges started from this addon's own forms
 * are pushed by {@code BedrockDuelService} instead, so both routes are covered.</p>
 *
 * <p>Not covered: a third-party plugin calling {@code ChallengeFacade} directly. Those players see
 * only the chat line.</p>
 */
public final class InvitationListener implements Listener {
    private final Plugin plugin;
    private final DuelsAddonConfiguration config;
    private final DuelsGateway gateway;
    private final BedrockInvitationService invitationService;
    private final BedrockDuelService duelService;

    public InvitationListener(Plugin plugin, DuelsAddonConfiguration config, DuelsGateway gateway,
                              BedrockInvitationService invitationService,
                              BedrockDuelService duelService) {
        this.plugin = plugin;
        this.config = config;
        this.gateway = gateway;
        this.invitationService = invitationService;
        this.duelService = duelService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onParticipantAdded(PartyParticipantAddedEvent event) {
        Participant participant = event.getParticipant();
        Party party = event.getParty();
        debug(() -> "PartyParticipantAddedEvent participant="
                + (participant == null ? "null" : participant.getName())
                + " accepted=" + (participant != null && participant.isAccepted())
                + " party=" + (party == null ? "null" : party.getLeaderUniqueId()));
        if (participant == null || party == null || participant.isAccepted()) {
            return;
        }
        UUID leaderId = party.getLeaderUniqueId();
        if (leaderId == null || leaderId.equals(participant.getUniqueId())) {
            return;
        }
        Player invited = participant.bukkit();
        if (invited == null) {
            invited = Bukkit.getPlayer(participant.getUniqueId());
        }
        if (invited == null) {
            debug(() -> "Party invite ignored: invited player is offline");
            return;
        }
        Player target = invited;
        String leaderName = nameOf(leaderId);
        debug(() -> "Party invite for " + target.getName() + ": bedrockForm="
                + invitationService.shouldHandle(target) + " formsEnabled=" + config.partyInviteFormsEnabled());
        plugin.getServer().getScheduler().runTask(plugin,
                () -> invitationService.sendPartyInvite(target, leaderId, leaderName));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onParticipantAccepted(PartyParticipantAcceptedEvent event) {
        resolve(event.getParty(), event.getParticipant());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onParticipantRemoved(PartyParticipantRemovedEvent event) {
        resolve(event.getParty(), event.getParticipant());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPartyDisband(PartyDisbandEvent event) {
        Party party = event.getParty();
        if (party == null || event.getParticipants() == null) {
            return;
        }
        UUID leaderId = party.getLeaderUniqueId();
        for (Participant participant : event.getParticipants()) {
            if (participant != null) {
                invitationService.resolve(leaderId, participant.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.length() < 2 || message.charAt(0) != '/') {
            return;
        }
        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length < 2) {
            return;
        }
        String root = parts[0].toLowerCase(Locale.ROOT);
        String duelCommand = gateway.commandName("duel", "duel");
        if (!root.equals(duelCommand) && !root.equals("duel")) {
            return;
        }
        String targetName = parts[1];
        if (targetName.equalsIgnoreCase("accept") || targetName.equalsIgnoreCase("decline")) {
            return;
        }
        UUID inviterId = event.getPlayer().getUniqueId();
        String inviterName = event.getPlayer().getName();
        String modeArg = parts.length > 2 ? parts[2] : null;
        int roundsArg = parts.length > 3 ? parseRounds(parts[3]) : 0;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null || target.getUniqueId().equals(inviterId)) {
                return;
            }
            // Deliberately not gated on the invitation registry. Looking the invitation up here
            // returned nothing often enough that the form never appeared, and a form shown for an
            // invitation PhoenixDuels no longer has is harmless: accepting runs their command,
            // which answers "invitation not found or expired" itself.
            String modeName = modeArg == null ? null
                    : gateway.mode(modeArg).map(it.pintux.life.duelsaddon.model.ModeView::displayName)
                            .orElse(modeArg);
            int rounds = roundsArg > 0 ? roundsArg
                    : gateway.mode(modeArg == null ? "" : modeArg)
                            .map(it.pintux.life.duelsaddon.model.ModeView::roundsToWin).orElse(1);
            debug(() -> "Duel challenge from " + inviterName + " to " + target.getName()
                    + ": bedrockForm=" + invitationService.shouldHandle(target));
            invitationService.sendDuelChallenge(target, inviterId,
                    new it.pintux.life.duelsaddon.model.InviteView(inviterName,
                            modeName == null ? "?" : modeName, rounds,
                            gateway.invitationExpirationSeconds()));
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        invitationService.forget(event.getPlayer().getUniqueId());
        duelService.forget(event.getPlayer().getUniqueId());
    }

    private static int parseRounds(String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void debug(java.util.function.Supplier<String> message) {
        if (config.debugEnabled()) {
            plugin.getLogger().info(message.get());
        }
    }

    private void resolve(Party party, Participant participant) {
        if (party == null || participant == null) {
            return;
        }
        invitationService.resolve(party.getLeaderUniqueId(), participant.getUniqueId());
    }

    private static String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String offline = Bukkit.getOfflinePlayer(uuid).getName();
        return offline == null ? "?" : offline;
    }
}
