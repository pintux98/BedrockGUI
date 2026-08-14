package it.pintux.life.duelsaddon.gateway;

import it.pintux.life.duelsaddon.model.DuelDraftView;
import it.pintux.life.duelsaddon.model.InviteView;
import it.pintux.life.duelsaddon.model.KitView;
import it.pintux.life.duelsaddon.model.LeaderboardEntry;
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

/**
 * The only boundary between this addon and PhoenixDuels.
 *
 * <p>PhoenixDuels documents that its internal classes change between versions without
 * compatibility guarantees, and its public API covers events and read-only interfaces but none of
 * the operations this addon needs. So the addon compiles against the plugin jar and confines every
 * {@code com.phoenixplugins} type to {@link DuelsGatewayImpl}. A PhoenixDuels update therefore
 * shows up as compile errors in one file instead of scattered across the services.</p>
 *
 * <p>Implementations must never throw: every call either succeeds or reports failure through its
 * return value, so a broken PhoenixDuels degrades to the untouched Java UI rather than breaking
 * the server. Callers gate on {@link #isAvailable()} first.</p>
 */
public interface DuelsGateway {

    /**
     * @return whether PhoenixDuels is installed, enabled, and reachable through its internals
     */
    boolean isAvailable();

    /**
     * @return PhoenixDuels' edition label, for startup logging; {@code UNKNOWN} if unreadable
     */
    String edition();

    /**
     * @return how long an invitation stays open, from PhoenixDuels' own config
     */
    int invitationExpirationSeconds();

    /**
     * Resolves a PhoenixDuels command name, every one of which is remappable in their config.
     *
     * @param key      logical command, such as {@code duel} or {@code party}
     * @param fallback returned when PhoenixDuels is unreachable or the key is unknown
     */
    String commandName(String key, String fallback);

    /**
     * @return every registered mode, enabled or not
     */
    List<ModeView> modes();

    Optional<ModeView> mode(String modeId);

    /**
     * @return enabled modes queueable on the given ladder at the given team size
     */
    List<ModeView> modesFor(boolean ranked, TeamSize size);

    boolean isInQueue(Player player);

    /**
     * @return the mode the player is queued for, empty when not queued
     */
    Optional<String> queuedModeId(Player player);

    /**
     * @param modeId may be blank to count every mode at that ladder and size
     * @return players currently waiting in the matching queues
     */
    int queuedPlayers(boolean ranked, TeamSize size, String modeId);

    /**
     * Queues the player, or their party when they lead one and the size is not solo.
     *
     * @return whether PhoenixDuels accepted the profile
     */
    boolean joinQueue(Player player, boolean ranked, TeamSize size, String modeId);

    boolean leaveQueue(Player player);

    boolean isInMatch(Player player);

    Optional<PartyView> party(Player player);

    Optional<PartyView> partyOf(UUID playerId);

    /**
     * @return parties other than the player's own that are free to be challenged
     */
    List<PartyView> otherParties(Player player);

    boolean createParty(Player player);

    boolean invitePlayer(Player leader, String targetName);

    boolean acceptPartyInvitation(Player player, UUID leaderId);

    boolean declinePartyInvitation(Player player, UUID leaderId);

    boolean kickMember(Player leader, UUID memberId);

    boolean promoteMember(Player leader, UUID memberId);

    boolean leaveParty(Player player);

    boolean disbandParty(Player leader);

    /**
     * Splits the party into two teams and starts the match.
     */
    boolean startPartyTeamFight(Player leader, String modeId);

    /**
     * @param teams clamped to PhoenixDuels' supported 2 to 4
     */
    boolean startPartyMultiTeamFight(Player leader, int teams, String modeId);

    boolean startPartyFfa(Player leader, String modeId);

    boolean challengeParty(Player leader, UUID opponentLeaderId, String modeId, int rounds);

    boolean challengePlayer(Player from, String targetName, String modeId, int rounds);

    /**
     * Reads the target and mode out of a PhoenixDuels duel menu that is opening.
     *
     * @param containerView the cancelled view, passed as {@code Object} so callers outside the
     *                      gateway never name a PhoenixDuels type
     * @return the draft, or empty when the view is not a duel menu or carries no target
     */
    Optional<DuelDraftView> duelDraft(Object containerView);

    boolean hasPendingChallenge(UUID inviterId, UUID invitedId);

    Optional<InviteView> pendingChallenge(UUID inviterId, UUID invitedId);

    boolean acceptChallenge(Player player, UUID inviterId);

    boolean declineChallenge(Player player, UUID inviterId);

    boolean isRejectingDuelRequests(Player player);

    void setRejectingDuelRequests(Player player, boolean rejecting);

    boolean isRejectingPartyRequests(Player player);

    void setRejectingPartyRequests(Player player, boolean rejecting);

    /**
     * @param playerName used only when PhoenixDuels has no name recorded for the uuid
     */
    Optional<StatsView> stats(UUID playerId, String playerName, StatsKind kind);

    /**
     * @param metric one of {@code wins}, {@code kills}, {@code losses}, {@code deaths};
     *               anything else falls back to wins
     */
    List<LeaderboardEntry> leaderboard(StatsKind kind, String metric, int limit);

    List<MatchView> ongoingMatches();

    /**
     * @param anyPlayerInMatch any participant of the target match, since PhoenixDuels keys
     *                         spectating off a player rather than a match id
     */
    boolean spectate(Player player, UUID anyPlayerInMatch);

    List<KitView> kits();

    Optional<KitView> kit(String kitId);

    /**
     * @return whether PhoenixDuels is holding an inventory it saved for this player
     */
    boolean hasLostItems(Player player);

    boolean claimLostItems(Player player);
}
