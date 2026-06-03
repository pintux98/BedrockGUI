package it.pintux.life.bedwarsaddon.model;

/**
 * A team-upgrade menu entry resolved live for one player.
 *
 * @param id         provider identifier used to purchase (opaque; index-qualified to survive duplicates)
 * @param name       display name (from the upgrade's display item)
 * @param cost       price of the NEXT tier (0 when unknown/maxed)
 * @param currency   human-readable currency name ("" when unknown)
 * @param affordable whether the player can currently afford the next tier
 * @param maxed      whether the team already owns the highest tier
 */
public record UpgradeContent(String id, String name, int cost, String currency,
                             boolean affordable, boolean maxed) {}
