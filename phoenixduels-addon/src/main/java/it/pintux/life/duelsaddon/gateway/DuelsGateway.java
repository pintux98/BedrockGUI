package it.pintux.life.duelsaddon.gateway;

import it.pintux.life.duelsaddon.model.InviteView;
import it.pintux.life.duelsaddon.model.KitView;
import it.pintux.life.duelsaddon.model.LeaderboardEntry;
import it.pintux.life.duelsaddon.model.MapView;
import it.pintux.life.duelsaddon.model.MatchView;
import it.pintux.life.duelsaddon.model.ModeView;
import it.pintux.life.duelsaddon.model.PartyView;
import it.pintux.life.duelsaddon.model.StatsKind;
import it.pintux.life.duelsaddon.model.StatsView;
import it.pintux.life.duelsaddon.model.TeamSize;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DuelsGateway {

    boolean isAvailable();

    String edition();

    int invitationExpirationSeconds();

    String commandName(String key, String fallback);

    List<ModeView> modes();

    Optional<ModeView> mode(String modeId);

    List<ModeView> modesFor(boolean ranked, TeamSize size);

    List<MapView> maps(String modeId);

    boolean isInQueue(Player player);

    Optional<String> queuedModeId(Player player);

    int queuedPlayers(boolean ranked, TeamSize size, String modeId);

    boolean joinQueue(Player player, boolean ranked, TeamSize size, String modeId);

    boolean leaveQueue(Player player);

    boolean isInMatch(Player player);

    Optional<PartyView> party(Player player);

    Optional<PartyView> partyOf(UUID playerId);

    List<PartyView> otherParties(Player player);

    boolean createParty(Player player);

    boolean invitePlayer(Player leader, String targetName);

    boolean acceptPartyInvitation(Player player, UUID leaderId);

    boolean declinePartyInvitation(Player player, UUID leaderId);

    boolean kickMember(Player leader, UUID memberId);

    boolean promoteMember(Player leader, UUID memberId);

    boolean leaveParty(Player player);

    boolean disbandParty(Player leader);

    boolean startPartyTeamFight(Player leader, String modeId);

    boolean startPartyMultiTeamFight(Player leader, int teams, String modeId);

    boolean startPartyFfa(Player leader, String modeId);

    boolean challengeParty(Player leader, UUID opponentLeaderId, String modeId, int rounds);

    boolean challengePlayer(Player from, String targetName, String modeId, int rounds);

    boolean hasPendingChallenge(UUID inviterId, UUID invitedId);

    Optional<InviteView> pendingChallenge(UUID inviterId, UUID invitedId);

    boolean acceptChallenge(Player player, UUID inviterId);

    boolean declineChallenge(Player player, UUID inviterId);

    boolean isRejectingDuelRequests(Player player);

    void setRejectingDuelRequests(Player player, boolean rejecting);

    boolean isRejectingPartyRequests(Player player);

    void setRejectingPartyRequests(Player player, boolean rejecting);

    Optional<StatsView> stats(UUID playerId, String playerName, StatsKind kind);

    List<LeaderboardEntry> leaderboard(StatsKind kind, String metric, int limit);

    List<MatchView> ongoingMatches();

    boolean spectate(Player player, UUID anyPlayerInMatch);

    List<KitView> kits();

    Optional<KitView> kit(String kitId);

    boolean hasLostItems(Player player);

    boolean claimLostItems(Player player);
}
