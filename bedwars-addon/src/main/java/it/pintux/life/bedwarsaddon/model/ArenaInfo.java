package it.pintux.life.bedwarsaddon.model;

/**
 * A joinable Bedwars arena.
 *
 * @param name        internal arena name (used to join)
 * @param displayName human-friendly name for the button
 * @param state       game state label (e.g. "waiting", "playing")
 * @param players     current player count
 * @param max         maximum players
 * @param group       arena group
 */
public record ArenaInfo(String name, String displayName, String state, int players, int max, String group) {}
