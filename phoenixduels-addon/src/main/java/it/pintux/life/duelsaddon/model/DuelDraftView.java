package it.pintux.life.duelsaddon.model;

/**
 * The half-built challenge PhoenixDuels already holds when its duel menu opens.
 *
 * <p>Running {@code /duel <player>} makes PhoenixDuels construct its duel layout with the target
 * and any mode already chosen. Reading that back is what lets the Bedrock form open on the duel
 * settings for that player instead of asking who to duel a second time.</p>
 *
 * @param targetName who is being challenged; never blank when this view exists
 * @param modeId     mode already selected, or {@code null} when the player has not picked one
 * @param rounds     rounds to win as PhoenixDuels currently has it
 */
public record DuelDraftView(String targetName, String modeId, int rounds) {
}
