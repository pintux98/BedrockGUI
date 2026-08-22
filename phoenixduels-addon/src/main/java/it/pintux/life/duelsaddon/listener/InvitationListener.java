package it.pintux.life.duelsaddon.listener;

import com.phoenixplugins.phoenixduels.api.events.party.PartyDisbandEvent;
import com.phoenixplugins.phoenixduels.api.events.party.PartyParticipantAcceptedEvent;
import com.phoenixplugins.phoenixduels.api.events.party.PartyParticipantAddedEvent;
import com.phoenixplugins.phoenixduels.api.events.party.PartyParticipantRemovedEvent;
import com.phoenixplugins.phoenixduels.api.participable.Participant;
import com.phoenixplugins.phoenixduels.api.party.Party;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.InviteView;
import it.pintux.life.duelsaddon.model.ModeView;
import it.pintux.life.duelsaddon.service.BedrockDuelService;
import it.pintux.life.duelsaddon.service.BedrockInvitationService;
import it.pintux.life.duelsaddon.util.CommandAliases;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;
import java.util.UUID;

/**
 * Watches PhoenixDuels for invitations that need a Bedrock form.
 *
 * <p>Party invitations have a real event. Duel challenges do not, and the duel command is not the
 * moment one is sent - it usually just opens the sender's builder - so the command is observed at
 * {@link EventPriority#MONITOR}, never cancelled, and used only to start watching for the
 * invitation to appear. Challenges sent through this addon's own duel form push their form
 * directly, and the duplicate guard keeps the two from doubling up.</p>
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
    private final CommandAliases duelCommands;

    public InvitationListener(Plugin plugin, DuelsAddonConfiguration config, DuelsGateway gateway,
                              BedrockInvitationService invitationService,
                              BedrockDuelService duelService) {
        this.plugin = plugin;
        this.config = config;
        this.gateway = gateway;
        this.invitationService = invitationService;
        this.duelService = duelService;
        this.duelCommands = config.commandAliases("commands.duel", "duel");
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
        String root = CommandAliases.rootOf(message);
        String[] args = CommandAliases.argsOf(message);
        if (root == null || args.length == 0) {
            return;
        }
        // PhoenixDuels' own configured command is watched on top of the configured aliases, so a
        // renamed duel command keeps working without an edit here.
        if (!duelCommands.matches(root) && !root.equals(normalize(gateway.commandName("duel", "duel")))) {
            return;
        }
        String targetName = args[0];
        if (targetName.equalsIgnoreCase("accept") || targetName.equalsIgnoreCase("decline")) {
            return;
        }
        UUID inviterId = event.getPlayer().getUniqueId();
        String inviterName = event.getPlayer().getName();
        String modeArg = args.length > 1 ? args[1] : null;
        int roundsArg = args.length > 2 ? parseRounds(args[2]) : 0;
        watchForChallenge(inviterId, inviterName, targetName, modeArg, roundsArg);
    }

    /**
     * Waits for the challenge to actually exist, then shows the form.
     *
     * <p>The command itself is not the moment a challenge is sent. {@code /duel <player>} opens
     * PhoenixDuels' builder for the sender, who still has to choose a mode and rounds and press
     * send, so firing the accept form off the command showed it to the target before the sender had
     * chosen anything. Passing a mode inline sends immediately instead, and there is no event for
     * either case, so this polls the invitation registry until one appears.</p>
     *
     * <p>Bounded by PhoenixDuels' own invitation lifetime, and stops as soon as the invitation shows
     * up, the target leaves, or that window passes.</p>
     */
    private void watchForChallenge(UUID inviterId, String inviterName, String targetName,
                                   String modeArg, int roundsArg) {
        long interval = 10L;
        int maxChecks = Math.max(1, (int) ((gateway.invitationExpirationSeconds() * 20L) / interval) + 2);
        int[] checks = {0};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Player target = Bukkit.getPlayerExact(targetName);
            boolean expired = ++checks[0] > maxChecks;
            if (target == null || target.getUniqueId().equals(inviterId) || expired) {
                cancel(task);
                return;
            }
            if (!gateway.hasPendingChallenge(inviterId, target.getUniqueId())) {
                return;
            }
            cancel(task);
            InviteView known = gateway.pendingChallenge(inviterId, target.getUniqueId())
                    .orElseGet(() -> fallbackView(inviterName, modeArg, roundsArg));
            debug(() -> "Duel challenge from " + inviterName + " to " + target.getName()
                    + " after " + checks[0] + " checks: bedrockForm="
                    + invitationService.shouldHandle(target));
            invitationService.sendDuelChallenge(target, inviterId, known);
        }, interval, interval);
    }

    private InviteView fallbackView(String inviterName, String modeArg, int roundsArg) {
        String modeName = modeArg == null ? null
                : gateway.mode(modeArg).map(ModeView::displayName).orElse(modeArg);
        int rounds = roundsArg > 0 ? roundsArg
                : gateway.mode(modeArg == null ? "" : modeArg).map(ModeView::roundsToWin).orElse(1);
        return new InviteView(inviterName, modeName == null ? "?" : modeName, rounds,
                gateway.invitationExpirationSeconds());
    }

    private static void cancel(BukkitTask[] task) {
        if (task[0] != null) {
            task[0].cancel();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        invitationService.forget(event.getPlayer().getUniqueId());
        duelService.forget(event.getPlayer().getUniqueId());
    }

    private static String normalize(String command) {
        return command == null ? null : command.trim().toLowerCase(Locale.ROOT);
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
