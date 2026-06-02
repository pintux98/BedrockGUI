package it.pintux.life.bedwarsaddon.model;

/** A player a spectator can teleport to.
 *
 * @param uuid target player UUID (string form, used in the action payload)
 * @param name target display name
 */
public record SpectateTarget(String uuid, String name) {}
