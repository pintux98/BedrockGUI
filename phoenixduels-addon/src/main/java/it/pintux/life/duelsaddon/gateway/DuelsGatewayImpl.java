package it.pintux.life.duelsaddon.gateway;

import com.phoenixplugins.phoenixduels.PhoenixDuels;
import com.phoenixplugins.phoenixduels.api.enums.PlayerMode;
import com.phoenixplugins.phoenixduels.api.participable.Participant;
import com.phoenixplugins.phoenixduels.api.profiles.MatchProfile;
import com.phoenixplugins.phoenixduels.facades.ChallengeFacade;
import com.phoenixplugins.phoenixduels.facades.InvitationFacade;
import com.phoenixplugins.phoenixduels.facades.PartiesFacade;
import com.phoenixplugins.phoenixduels.managers.MatchsManager;
import com.phoenixplugins.phoenixduels.managers.PartyManager;
import com.phoenixplugins.phoenixduels.managers.PlayersManager;
import com.phoenixplugins.phoenixduels.managers.StatsManager;
import com.phoenixplugins.phoenixduels.managers.kits.Kit;
import com.phoenixplugins.phoenixduels.managers.matchs.match.ArenaMatch;
import com.phoenixplugins.phoenixduels.managers.matchs.match.player.MatchAbstractPlayer;
import com.phoenixplugins.phoenixduels.managers.matchs.profiles.ChallengeMatchProfile;
import com.phoenixplugins.phoenixduels.managers.matchs.profiles.RankedMatchProfile;
import com.phoenixplugins.phoenixduels.managers.matchs.profiles.UnrankedMatchProfile;
import com.phoenixplugins.phoenixduels.managers.modes.Mode;
import com.phoenixplugins.phoenixduels.lib.common.uicomponents.newest.container.holders.ContainerView;
import com.phoenixplugins.phoenixduels.managers.party.PartyImpl;
import com.phoenixplugins.phoenixduels.managers.players.PlayerData;
import com.phoenixplugins.phoenixduels.managers.stats.PlayerStats;
import com.phoenixplugins.phoenixduels.registry.loadout.buitin.PremadeKitLoadout;
import com.phoenixplugins.phoenixduels.registry.menus.generic.duel.DuelPlayerLayoutMenu;
import it.pintux.life.duelsaddon.model.DuelDraftView;
import it.pintux.life.duelsaddon.model.InviteView;
import it.pintux.life.duelsaddon.model.KitView;
import it.pintux.life.duelsaddon.model.LeaderboardEntry;
import it.pintux.life.duelsaddon.model.MatchView;
import it.pintux.life.duelsaddon.model.MemberView;
import it.pintux.life.duelsaddon.model.ModeView;
import it.pintux.life.duelsaddon.model.PartyView;
import it.pintux.life.duelsaddon.model.StatsKind;
import it.pintux.life.duelsaddon.model.StatsView;
import it.pintux.life.duelsaddon.model.TeamSize;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The one class in this addon that touches PhoenixDuels types.
 *
 * <p>Every method swallows {@link Throwable} rather than just {@link Exception}, because a
 * PhoenixDuels update that removes an internal member surfaces as {@link NoSuchMethodError} or
 * {@link NoClassDefFoundError} at call time. Catching those turns a broken integration into a
 * disabled one, so the Bedrock player falls back to PhoenixDuels' own Java menu instead of the
 * server throwing on every inventory open.</p>
 */
public final class DuelsGatewayImpl implements DuelsGateway {

    /**
     * The free build registers as {@code PhoenixDuelsLite}, the paid build as {@code PhoenixDuels}.
     */
    private static final String[] PLUGIN_NAMES = {"PhoenixDuels", "PhoenixDuelsLite"};

    private final Logger logger;

    public DuelsGatewayImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isAvailable() {
        try {
            return plugin() != null && duels() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public String edition() {
        try {
            PhoenixDuels duels = duels();
            return duels == null || duels.getEdition() == null ? "UNKNOWN" : duels.getEdition().toString();
        } catch (Throwable ignored) {
            return "UNKNOWN";
        }
    }

    @Override
    public int invitationExpirationSeconds() {
        try {
            return Math.max(1, duels().getConfiguration().getInvitationExpirationTime());
        } catch (Throwable ignored) {
            return 60;
        }
    }

    @Override
    public String commandName(String key, String fallback) {
        try {
            var commands = duels().getConfiguration().getCommands();
            String value = switch (key) {
                case "duel" -> commands.getDuelAlternative();
                case "queue" -> commands.getQueueAlternative();
                case "party" -> commands.getPartyAlternative();
                case "settings" -> commands.getDuelSettingsAlternative();
                case "stats" -> commands.getStatsAlternative();
                case "spectate" -> commands.getSpectateAlternative();
                case "lostitems" -> commands.getLostItemsAlternative();
                case "kiteditor" -> commands.getKitEditorAlternative();
                case "killeffects" -> commands.getKillEffectsAlternative();
                case "matchhistory" -> commands.getMatchHistoryAlternative();
                case "duelmatches" -> commands.getDuelMatchesAlternative();
                case "leave" -> commands.getLeaveAlternative();
                default -> null;
            };
            return value == null || value.isBlank() ? fallback : value.toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    @Override
    public List<ModeView> modes() {
        List<ModeView> out = new ArrayList<>();
        try {
            for (Mode mode : duels().getModesManager().getRegisteredModes()) {
                out.add(toModeView(mode));
            }
        } catch (Throwable t) {
            warn("modes", t);
        }
        return out;
    }

    @Override
    public Optional<ModeView> mode(String modeId) {
        try {
            Mode mode = duels().getModesManager().getModeOrNull(modeId);
            return mode == null ? Optional.empty() : Optional.of(toModeView(mode));
        } catch (Throwable t) {
            warn("mode", t);
            return Optional.empty();
        }
    }

    @Override
    public List<ModeView> modesFor(boolean ranked, TeamSize size) {
        List<ModeView> out = new ArrayList<>();
        for (ModeView mode : modes()) {
            if (mode.enabled() && mode.supports(ranked, size)) {
                out.add(mode);
            }
        }
        return out;
    }

    @Override
    public boolean isInQueue(Player player) {
        try {
            return duels().getMatchsManager().isInQueue(player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public Optional<String> queuedModeId(Player player) {
        try {
            MatchProfile profile = duels().getMatchsManager().getInQueue().get(player.getUniqueId());
            return profile == null ? Optional.empty() : Optional.ofNullable(profile.getSelectedModeId());
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    @Override
    public int queuedPlayers(boolean ranked, TeamSize size, String modeId) {
        try {
            PlayerMode playerMode = playerMode(ranked, size);
            int total = 0;
            for (var entry : duels().getMatchsManager().getQueues().entrySet()) {
                var options = entry.getKey();
                if (options.getPlayerMode() != playerMode) {
                    continue;
                }
                if (modeId != null && !modeId.isBlank() && !modeId.equals(options.getModeId())) {
                    continue;
                }
                total += entry.getValue().countQueuedPlayers();
            }
            return total;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Override
    public boolean joinQueue(Player player, boolean ranked, TeamSize size, String modeId) {
        try {
            MatchsManager matchs = duels().getMatchsManager();
            PlayerMode playerMode = playerMode(ranked, size);
            PartyImpl party = partyImpl(player.getUniqueId());
            MatchProfile profile;
            if (party != null && party.isLeader(player.getUniqueId()) && size != TeamSize.SOLO) {
                profile = ranked
                        ? new RankedMatchProfile(matchs, party, playerMode, modeId)
                        : new UnrankedMatchProfile(matchs, party, playerMode, modeId);
            } else {
                profile = ranked
                        ? new RankedMatchProfile(matchs, player, playerMode, modeId)
                        : new UnrankedMatchProfile(matchs, player, playerMode, modeId);
            }
            return matchs.addProfileToQueue(profile);
        } catch (Throwable t) {
            warn("joinQueue", t);
            return false;
        }
    }

    @Override
    public boolean leaveQueue(Player player) {
        try {
            return duels().getMatchsManager().removePlayerFromQueue(player, true);
        } catch (Throwable t) {
            warn("leaveQueue", t);
            return false;
        }
    }

    @Override
    public boolean isInMatch(Player player) {
        try {
            return ArenaMatch.PLAYER_LOOKUP_TABLE.containsKey(player.getUniqueId());
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public Optional<PartyView> party(Player player) {
        return partyOf(player.getUniqueId());
    }

    @Override
    public Optional<PartyView> partyOf(UUID playerId) {
        try {
            PartyImpl party = partyImpl(playerId);
            return party == null ? Optional.empty() : Optional.of(toPartyView(party));
        } catch (Throwable t) {
            warn("partyOf", t);
            return Optional.empty();
        }
    }

    @Override
    public List<PartyView> otherParties(Player player) {
        List<PartyView> out = new ArrayList<>();
        try {
            PartyImpl own = partyImpl(player.getUniqueId());
            Map<PartyImpl, Boolean> seen = new IdentityHashMap<>();
            for (PartyImpl party : PartyImpl.PARTIES_PLAYERS_LOOKUP_TABLE.values()) {
                if (party == null || party.isDisbanded() || party == own || seen.put(party, Boolean.TRUE) != null) {
                    continue;
                }
                if (party.getPendingChallenge() != null) {
                    continue;
                }
                out.add(toPartyView(party));
            }
        } catch (Throwable t) {
            warn("otherParties", t);
        }
        return out;
    }

    @Override
    public boolean createParty(Player player) {
        try {
            return duels().getPartyManager().getOrCreateParty(player) != null;
        } catch (Throwable t) {
            warn("createParty", t);
            return false;
        }
    }

    @Override
    public boolean invitePlayer(Player leader, String targetName) {
        try {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                return false;
            }
            PartyImpl party = duels().getPartyManager().getOrCreateParty(leader);
            if (party == null) {
                return false;
            }
            PartiesFacade.invitePlayer(party, target);
            return true;
        } catch (Throwable t) {
            warn("invitePlayer", t);
            return false;
        }
    }

    @Override
    public boolean acceptPartyInvitation(Player player, UUID leaderId) {
        try {
            PartyImpl party = partyImpl(leaderId);
            if (party == null) {
                return false;
            }
            PartiesFacade.acceptInvitation(player, party);
            return true;
        } catch (Throwable t) {
            warn("acceptPartyInvitation", t);
            return false;
        }
    }

    @Override
    public boolean declinePartyInvitation(Player player, UUID leaderId) {
        try {
            PartyImpl party = partyImpl(leaderId);
            if (party == null) {
                return false;
            }
            PartiesFacade.declineInvitation(player, party);
            return true;
        } catch (Throwable t) {
            warn("declinePartyInvitation", t);
            return false;
        }
    }

    @Override
    public boolean kickMember(Player leader, UUID memberId) {
        try {
            PartyImpl party = partyImpl(leader.getUniqueId());
            if (party == null || !party.isLeader(leader.getUniqueId())) {
                return false;
            }
            PartiesFacade.kickPlayer(party, memberId);
            return true;
        } catch (Throwable t) {
            warn("kickMember", t);
            return false;
        }
    }

    @Override
    public boolean promoteMember(Player leader, UUID memberId) {
        try {
            PartyImpl party = partyImpl(leader.getUniqueId());
            if (party == null || !party.isLeader(leader.getUniqueId())) {
                return false;
            }
            PartiesFacade.promotePlayer(party, memberId);
            return true;
        } catch (Throwable t) {
            warn("promoteMember", t);
            return false;
        }
    }

    @Override
    public boolean leaveParty(Player player) {
        try {
            PartyImpl party = partyImpl(player.getUniqueId());
            return party != null && PartiesFacade.leaveParty(party, player);
        } catch (Throwable t) {
            warn("leaveParty", t);
            return false;
        }
    }

    @Override
    public boolean disbandParty(Player leader) {
        try {
            PartyImpl party = partyImpl(leader.getUniqueId());
            if (party == null || !party.isLeader(leader.getUniqueId())) {
                return false;
            }
            duels().getPartyManager().disbandParty(party);
            return true;
        } catch (Throwable t) {
            warn("disbandParty", t);
            return false;
        }
    }

    @Override
    public boolean startPartyTeamFight(Player leader, String modeId) {
        return startPartyMultiTeamFight(leader, 2, modeId);
    }

    /**
     * Splits the party and queues the resulting challenge profile.
     *
     * <p>PhoenixDuels exposes no single "start party fight" call, so this reproduces what their
     * menu does: build the team groups, wrap them in a challenge profile, and hand it to the
     * matchmaker. Derived from bytecode and not yet exercised against a live server.</p>
     */
    @Override
    public boolean startPartyMultiTeamFight(Player leader, int teams, String modeId) {
        try {
            PartyImpl party = partyImpl(leader.getUniqueId());
            Mode mode = duels().getModesManager().getModeOrNull(modeId);
            if (party == null || mode == null || !party.isLeader(leader.getUniqueId())) {
                return false;
            }
            PartyManager manager = duels().getPartyManager();
            var groups = manager.createTeamFightGroups(party, Math.max(2, Math.min(4, teams)));
            if (groups == null || groups.length < 2) {
                return false;
            }
            ChallengeMatchProfile profile = new ChallengeMatchProfile(Arrays.asList(groups));
            profile.setSelectedModeId(mode.getIdentifier());
            profile.setRoundsToWin(Math.max(1, mode.getRoundsToWin()));
            return duels().getMatchsManager().addProfileToQueue(profile);
        } catch (Throwable t) {
            warn("startPartyMultiTeamFight", t);
            return false;
        }
    }

    @Override
    public boolean startPartyFfa(Player leader, String modeId) {
        try {
            PartyImpl party = partyImpl(leader.getUniqueId());
            Mode mode = duels().getModesManager().getModeOrNull(modeId);
            if (party == null || mode == null || !party.isLeader(leader.getUniqueId())) {
                return false;
            }
            int members = party.getMembers().size();
            if (members < 2) {
                return false;
            }
            var groups = duels().getPartyManager().createTeamFightGroups(party, Math.min(4, members));
            if (groups == null || groups.length < 2) {
                return false;
            }
            ChallengeMatchProfile profile = new ChallengeMatchProfile(Arrays.asList(groups));
            profile.setSelectedModeId(mode.getIdentifier());
            profile.setRoundsToWin(Math.max(1, mode.getRoundsToWin()));
            return duels().getMatchsManager().addProfileToQueue(profile);
        } catch (Throwable t) {
            warn("startPartyFfa", t);
            return false;
        }
    }

    @Override
    public boolean challengeParty(Player leader, UUID opponentLeaderId, String modeId, int rounds) {
        try {
            PartyImpl own = partyImpl(leader.getUniqueId());
            PartyImpl opponent = partyImpl(opponentLeaderId);
            Mode mode = duels().getModesManager().getModeOrNull(modeId);
            if (own == null || opponent == null || mode == null || own == opponent) {
                return false;
            }
            if (!own.isLeader(leader.getUniqueId())) {
                return false;
            }
            var groups = duels().getPartyManager().createTeamFightGroups(own, 1);
            if (groups == null || groups.length == 0) {
                return false;
            }
            ChallengeMatchProfile profile = new ChallengeMatchProfile(Arrays.asList(groups));
            profile.setSelectedModeId(mode.getIdentifier());
            profile.setRoundsToWin(Math.max(1, rounds));
            ChallengeFacade.challengeParty(profile, opponent);
            return true;
        } catch (Throwable t) {
            warn("challengeParty", t);
            return false;
        }
    }

    @Override
    public boolean challengePlayer(Player from, String targetName, String modeId, int rounds) {
        try {
            Player target = Bukkit.getPlayerExact(targetName);
            Mode mode = duels().getModesManager().getModeOrNull(modeId);
            if (target == null || mode == null || target.getUniqueId().equals(from.getUniqueId())) {
                return false;
            }
            ChallengeFacade.challengePlayer(mode, Math.max(1, rounds), from, target);
            return true;
        } catch (Throwable t) {
            warn("challengePlayer", t);
            return false;
        }
    }

    @Override
    public Optional<DuelDraftView> duelDraft(Object containerView) {
        try {
            if (!(containerView instanceof ContainerView view)) {
                return Optional.empty();
            }
            if (!(view.getContainer() instanceof DuelPlayerLayoutMenu menu)) {
                return Optional.empty();
            }
            Player target = menu.getTarget();
            if (target == null) {
                return Optional.empty();
            }
            Mode mode = menu.getSelectedMode();
            return Optional.of(new DuelDraftView(target.getName(),
                    mode == null ? null : mode.getIdentifier(),
                    Math.max(1, menu.getRoundsToWin())));
        } catch (Throwable t) {
            warn("duelDraft", t);
            return Optional.empty();
        }
    }

    @Override
    public boolean hasPendingChallenge(UUID inviterId, UUID invitedId) {
        try {
            return InvitationFacade.getFactory().isInvited(inviterId, invitedId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public Optional<InviteView> pendingChallenge(UUID inviterId, UUID invitedId) {
        try {
            InvitationFacade.Invitation invitation = InvitationFacade.getFactory()
                    .getInvitationOrNull(inviterId, invitedId);
            if (invitation == null) {
                return Optional.empty();
            }
            ChallengeMatchProfile profile = invitation.getInviter();
            String modeId = profile == null ? null : profile.getSelectedModeId();
            String modeName = mode(modeId).map(ModeView::displayName).orElse(modeId == null ? "?" : modeId);
            int rounds = profile == null ? 1 : Math.max(1, profile.getRoundsToWin());
            return Optional.of(new InviteView(nameOf(inviterId), modeName, rounds,
                    invitationExpirationSeconds()));
        } catch (Throwable t) {
            warn("pendingChallenge", t);
            return Optional.empty();
        }
    }

    @Override
    public boolean acceptChallenge(Player player, UUID inviterId) {
        try {
            ChallengeMatchProfile profile = challengeProfile(inviterId, player.getUniqueId());
            if (profile == null) {
                return false;
            }
            PartyImpl party = partyImpl(player.getUniqueId());
            if (party != null && party.isLeader(player.getUniqueId()) && party.getMembers().size() > 1) {
                ChallengeFacade.acceptChallenge(profile, party);
            } else {
                ChallengeFacade.acceptChallenge(profile, player);
            }
            return true;
        } catch (Throwable t) {
            warn("acceptChallenge", t);
            return false;
        }
    }

    @Override
    public boolean declineChallenge(Player player, UUID inviterId) {
        try {
            ChallengeMatchProfile profile = challengeProfile(inviterId, player.getUniqueId());
            if (profile == null) {
                return false;
            }
            PartyImpl party = partyImpl(player.getUniqueId());
            if (party != null && party.isLeader(player.getUniqueId()) && party.getMembers().size() > 1) {
                ChallengeFacade.declineChallenge(profile, party);
            } else {
                ChallengeFacade.declineChallenge(profile, player);
            }
            return true;
        } catch (Throwable t) {
            warn("declineChallenge", t);
            return false;
        }
    }

    @Override
    public boolean isRejectingDuelRequests(Player player) {
        PlayerData data = playerData(player);
        return data != null && data.isRejectingDuelsRequest();
    }

    @Override
    public void setRejectingDuelRequests(Player player, boolean rejecting) {
        PlayerData data = playerData(player);
        if (data == null) {
            return;
        }
        data.setRejectingDuelsRequest(rejecting);
        persist(data);
    }

    @Override
    public boolean isRejectingPartyRequests(Player player) {
        PlayerData data = playerData(player);
        return data != null && data.isRejectingPartyRequest();
    }

    @Override
    public void setRejectingPartyRequests(Player player, boolean rejecting) {
        PlayerData data = playerData(player);
        if (data == null) {
            return;
        }
        data.setRejectingPartyRequest(rejecting);
        persist(data);
    }

    @Override
    public Optional<StatsView> stats(UUID playerId, String playerName, StatsKind kind) {
        try {
            PlayerStats stats = duels().getStatsManager().getPlayerStats(playerId, matchType(kind));
            if (stats == null) {
                return Optional.empty();
            }
            return Optional.of(new StatsView(
                    stats.getPlayerName() == null ? playerName : stats.getPlayerName(),
                    stats.getTotalWins(),
                    stats.getTotalLosses(),
                    stats.getTotalDraws(),
                    stats.getTotalKills(),
                    stats.getTotalDeaths()));
        } catch (Throwable t) {
            warn("stats", t);
            return Optional.empty();
        }
    }

    @Override
    public List<LeaderboardEntry> leaderboard(StatsKind kind, String metric, int limit) {
        List<LeaderboardEntry> out = new ArrayList<>();
        try {
            StatsManager.Sorting sorting = sorting(metric);
            List<PlayerStats> rows = duels().getStatsManager().getCachedOrFetchNow(
                    matchType(kind), StatsManager.Period.ALL, sorting, null, Math.max(1, limit), 0);
            if (rows == null) {
                return out;
            }
            int rank = 1;
            for (PlayerStats row : rows) {
                out.add(new LeaderboardEntry(rank++, row.getPlayerName(), metricValue(row, metric)));
            }
        } catch (Throwable t) {
            warn("leaderboard", t);
        }
        return out;
    }

    @Override
    public List<MatchView> ongoingMatches() {
        List<MatchView> out = new ArrayList<>();
        try {
            for (ArenaMatch match : duels().getMatchsManager().getMatchs().values()) {
                if (match == null) {
                    continue;
                }
                List<String> names = new ArrayList<>();
                UUID any = null;
                for (MatchAbstractPlayer player : match.getTeamsConnectedPlayers()) {
                    names.add(player.getName());
                    if (any == null) {
                        any = player.getUniqueId();
                    }
                }
                if (any == null) {
                    continue;
                }
                Mode mode = match.getMode();
                out.add(new MatchView(any,
                        mode == null ? "?" : displayName(mode.getDisplayName(), mode.getIdentifier()),
                        names,
                        match.getCurrentRound(),
                        match.getRoundsToWin()));
            }
        } catch (Throwable t) {
            warn("ongoingMatches", t);
        }
        return out;
    }

    @Override
    public boolean spectate(Player player, UUID anyPlayerInMatch) {
        try {
            MatchAbstractPlayer target = ArenaMatch.PLAYER_LOOKUP_TABLE.get(anyPlayerInMatch);
            if (target == null || target.getMatch() == null) {
                return false;
            }
            ArenaMatch match = target.getMatch();
            duels().getMatchsManager().joinSpectator(match, player,
                    target.getBukkit() == null ? null : target.getBukkit().getLocation());
            return true;
        } catch (Throwable t) {
            warn("spectate", t);
            return false;
        }
    }

    @Override
    public List<KitView> kits() {
        List<KitView> out = new ArrayList<>();
        try {
            for (Kit kit : duels().getKitsManager().getKits()) {
                out.add(toKitView(kit));
            }
        } catch (Throwable t) {
            warn("kits", t);
        }
        return out;
    }

    @Override
    public Optional<KitView> kit(String kitId) {
        try {
            Kit kit = duels().getKitsManager().getKitOrNull(kitId);
            return kit == null ? Optional.empty() : Optional.of(toKitView(kit));
        } catch (Throwable t) {
            warn("kit", t);
            return Optional.empty();
        }
    }

    @Override
    public boolean hasLostItems(Player player) {
        PlayerData data = playerData(player);
        return data != null && data.validCachedInventory();
    }

    @Override
    public boolean claimLostItems(Player player) {
        PlayerData data = playerData(player);
        if (data == null || !data.validCachedInventory()) {
            return false;
        }
        try {
            data.restoreInventory(player);
            return true;
        } catch (Throwable t) {
            warn("claimLostItems", t);
            return false;
        }
    }

    private ChallengeMatchProfile challengeProfile(UUID inviterId, UUID invitedId) {
        InvitationFacade.Invitation invitation = InvitationFacade.getFactory()
                .getInvitationOrNull(inviterId, invitedId);
        return invitation == null ? null : invitation.getInviter();
    }

    /**
     * Resolves a player's party.
     *
     * <p>{@code PartyImpl.PARTIES_PLAYERS_LOOKUP_TABLE} is a public static map covering every
     * member of every party, so it answers for players other than the caller, which
     * {@code PartyManager.getPartyOrNull} is consulted for as a fallback.</p>
     *
     * @return the party, or {@code null} when the player has none or it has been disbanded
     */
    private PartyImpl partyImpl(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        PartyImpl party = PartyImpl.PARTIES_PLAYERS_LOOKUP_TABLE.get(playerId);
        if (party != null && !party.isDisbanded()) {
            return party;
        }
        PartyManager manager = duels().getPartyManager();
        PartyImpl fallback = manager == null ? null : manager.getPartyOrNull(playerId);
        return fallback != null && !fallback.isDisbanded() ? fallback : null;
    }

    private PlayerData playerData(Player player) {
        try {
            PlayersManager players = duels().getPlayersManager();
            PlayerData data = players.getCachedDataNow(player);
            return data != null ? data : players.getPlayerIfCached(player.getUniqueId(), false);
        } catch (Throwable t) {
            warn("playerData", t);
            return null;
        }
    }

    private void persist(PlayerData data) {
        try {
            duels().getPlayersManager().updatePlayerData(data);
        } catch (Throwable t) {
            warn("persist", t);
        }
    }

    private ModeView toModeView(Mode mode) {
        Set<TeamSize> unranked = EnumSet.noneOf(TeamSize.class);
        Set<TeamSize> ranked = EnumSet.noneOf(TeamSize.class);
        boolean challenge = false;
        Set<PlayerMode> allowed = new HashSet<>(mode.getAllowedPlayerType());
        for (PlayerMode playerMode : allowed) {
            switch (playerMode) {
                case UNRANKED_SOLO -> unranked.add(TeamSize.SOLO);
                case UNRANKED_DUO -> unranked.add(TeamSize.DUO);
                case UNRANKED_TRIO -> unranked.add(TeamSize.TRIO);
                case UNRANKED_QUAD -> unranked.add(TeamSize.QUAD);
                case RANKED_SOLO -> ranked.add(TeamSize.SOLO);
                case RANKED_DUO -> ranked.add(TeamSize.DUO);
                case RANKED_TRIO -> ranked.add(TeamSize.TRIO);
                case RANKED_QUAD -> ranked.add(TeamSize.QUAD);
                case SOLO_CHALLENGE, PARTY_CHALLENGE -> challenge = true;
                default -> {
                }
            }
        }
        List<String> description = mode.getDisplayDescription() == null
                ? List.of() : List.copyOf(mode.getDisplayDescription());
        return new ModeView(mode.getIdentifier(),
                displayName(mode.getDisplayName(), mode.getIdentifier()),
                description,
                mode.isEnabled(),
                unranked,
                ranked,
                challenge,
                mode.isFFAAllowed(),
                mode.isPermissionRequired(),
                mode.getPermission(),
                Math.max(1, mode.getRoundsToWin()));
    }

    private PartyView toPartyView(PartyImpl party) {
        UUID leaderId = party.getLeaderUniqueId();
        List<MemberView> members = new ArrayList<>();
        for (Map.Entry<UUID, Participant> entry : party.getParticipants().entrySet()) {
            Participant participant = entry.getValue();
            if (participant == null) {
                continue;
            }
            members.add(new MemberView(entry.getKey(),
                    participant.getName() == null ? nameOf(entry.getKey()) : participant.getName(),
                    entry.getKey().equals(leaderId),
                    !participant.isAccepted(),
                    participant.isOnline()));
        }
        members.sort(Comparator.comparing(MemberView::leader).reversed()
                .thenComparing(MemberView::pending)
                .thenComparing(MemberView::playerName, String.CASE_INSENSITIVE_ORDER));
        return new PartyView(leaderId, nameOf(leaderId), party.getMaximumSlots(), members,
                party.getPendingChallenge() != null);
    }

    private KitView toKitView(Kit kit) {
        List<KitView.KitItem> items = new ArrayList<>();
        List<PremadeKitLoadout.KitItem> kitItems = kit.getItems();
        if (kitItems != null) {
            for (PremadeKitLoadout.KitItem item : kitItems) {
                ItemStack stack = item == null ? null : item.getItem();
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                items.add(new KitView.KitItem(itemName(stack), stack.getAmount()));
            }
        }
        return new KitView(kit.getIdentifier(), displayName(kit.getDisplayName(), kit.getIdentifier()), items);
    }

    private static String itemName(ItemStack stack) {
        if (stack.hasItemMeta() && stack.getItemMeta() != null && stack.getItemMeta().hasDisplayName()) {
            return stack.getItemMeta().getDisplayName();
        }
        return it.pintux.life.duelsaddon.util.Formatting.prettify(stack.getType().name());
    }

    private static PlayerStats.MatchType matchType(StatsKind kind) {
        return switch (kind) {
            case RANKED -> PlayerStats.MatchType.RANKED;
            case CHALLENGE -> PlayerStats.MatchType.CHALLENGE;
            default -> PlayerStats.MatchType.UNRANKED;
        };
    }

    private static StatsManager.Sorting sorting(String metric) {
        return switch (metric == null ? "" : metric.toLowerCase(Locale.ROOT)) {
            case "kills" -> StatsManager.Sorting.TOTAL_KILLS;
            case "deaths" -> StatsManager.Sorting.TOTAL_DEATHS;
            case "losses" -> StatsManager.Sorting.TOTAL_LOSSES;
            default -> StatsManager.Sorting.TOTAL_WINS;
        };
    }

    private static int metricValue(PlayerStats stats, String metric) {
        return switch (metric == null ? "" : metric.toLowerCase(Locale.ROOT)) {
            case "kills" -> stats.getTotalKills();
            case "deaths" -> stats.getTotalDeaths();
            case "losses" -> stats.getTotalLosses();
            default -> stats.getTotalWins();
        };
    }

    private static PlayerMode playerMode(boolean ranked, TeamSize size) {
        if (ranked) {
            return switch (size) {
                case DUO -> PlayerMode.RANKED_DUO;
                case TRIO -> PlayerMode.RANKED_TRIO;
                case QUAD -> PlayerMode.RANKED_QUAD;
                default -> PlayerMode.RANKED_SOLO;
            };
        }
        return switch (size) {
            case DUO -> PlayerMode.UNRANKED_DUO;
            case TRIO -> PlayerMode.UNRANKED_TRIO;
            case QUAD -> PlayerMode.UNRANKED_QUAD;
            default -> PlayerMode.UNRANKED_SOLO;
        };
    }

    private static String displayName(String candidate, String fallback) {
        return candidate == null || candidate.isBlank()
                ? it.pintux.life.duelsaddon.util.Formatting.prettify(fallback)
                : candidate;
    }

    private static String nameOf(UUID uuid) {
        if (uuid == null) {
            return "?";
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String offline = Bukkit.getOfflinePlayer(uuid).getName();
        return offline == null ? uuid.toString().substring(0, 8) : offline;
    }

    private static PhoenixDuels duels() {
        return PhoenixDuels.getInstance();
    }

    private static Plugin plugin() {
        for (String name : PLUGIN_NAMES) {
            Plugin found = Bukkit.getPluginManager().getPlugin(name);
            if (found != null && found.isEnabled()) {
                return found;
            }
        }
        return null;
    }

    private void warn(String operation, Throwable t) {
        logger.warning("PhoenixDuels gateway call '" + operation + "' failed: " + t);
    }
}
