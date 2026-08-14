package it.pintux.life.duelsaddon.model;

import java.util.List;
import java.util.Set;

/**
 * A PhoenixDuels duel mode, flattened to what the Bedrock forms need.
 *
 * <p>PhoenixDuels expresses "which queues may use this mode" as a single {@code EnumSet} of
 * {@code PlayerMode} values that mixes ranked, unranked and challenge entries. That is split here
 * into {@link #unrankedSizes()}, {@link #rankedSizes()} and {@link #challengeAllowed()} so the
 * queue forms can filter without knowing PhoenixDuels' enum.</p>
 *
 * @param id                 registry identifier, used when joining a queue or sending a challenge
 * @param displayName        already-coloured name, falling back to a prettified id
 * @param description        mode lore lines, shown under the button label
 * @param enabled            whether the server has this mode switched on
 * @param unrankedSizes      team sizes queueable unranked
 * @param rankedSizes        team sizes queueable ranked
 * @param challengeAllowed   whether the mode may be used for a direct duel or party challenge
 * @param ffaAllowed         whether the mode supports party free-for-all
 * @param permissionRequired whether {@link #permission()} must be held to use the mode
 * @param permission         permission node, meaningful only when {@code permissionRequired}
 * @param roundsToWin        the mode's configured rounds, used as the challenge default
 */
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
                       int roundsToWin) {

    /**
     * @return whether this mode may be queued for the given ladder and team size
     */
    public boolean supports(boolean ranked, TeamSize size) {
        return ranked ? rankedSizes.contains(size) : unrankedSizes.contains(size);
    }

    /**
     * @return whether {@code player} may use this mode, treating a blank permission as open
     */
    public boolean allowedFor(org.bukkit.entity.Player player) {
        return !permissionRequired || permission == null || permission.isBlank()
                || player.hasPermission(permission);
    }

    /**
     * @return the first description line, or an empty string when the mode has no lore
     */
    public String summary() {
        return description == null || description.isEmpty() ? "" : description.get(0);
    }
}
