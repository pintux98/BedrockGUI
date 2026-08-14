package it.pintux.life.duelsaddon.model;

import java.util.List;
import java.util.Set;

public record ModeView(String id,
                       String displayName,
                       List<String> description,
                       boolean enabled,
                       Set<TeamSize> unrankedSizes,
                       Set<TeamSize> rankedSizes,
                       boolean challengeAllowed,
                       boolean ffaAllowed,
                       boolean permissionRequired,
                       String permission,
                       int roundsToWin,
                       int cooldownSeconds) {

    public boolean supports(boolean ranked, TeamSize size) {
        return ranked ? rankedSizes.contains(size) : unrankedSizes.contains(size);
    }
}
