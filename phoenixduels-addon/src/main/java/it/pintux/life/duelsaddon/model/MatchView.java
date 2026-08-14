package it.pintux.life.duelsaddon.model;

import java.util.List;
import java.util.UUID;

public record MatchView(UUID anyPlayerId,
                        String modeId,
                        String modeName,
                        List<String> playerNames,
                        int currentRound,
                        int roundsToWin,
                        boolean spectatable) {
}
