package it.pintux.life.duelsaddon.model;

import java.util.List;
import java.util.UUID;

/**
 * A running PhoenixDuels match, as shown in the spectator list.
 *
 * @param anyPlayerId  any connected participant, used to resolve the match again when the viewer
 *                     picks it; PhoenixDuels keys spectating off a player, not a match id
 * @param modeName     display name of the mode being played
 * @param playerNames  connected participants, in team order
 * @param currentRound round currently in progress
 * @param roundsToWin  rounds needed to win, so the list can show progress
 */
public record MatchView(UUID anyPlayerId,
                        String modeName,
                        List<String> playerNames,
                        int currentRound,
                        int roundsToWin) {
}
