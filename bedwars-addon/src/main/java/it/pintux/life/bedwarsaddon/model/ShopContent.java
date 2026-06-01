package it.pintux.life.bedwarsaddon.model;

/**
 * A purchasable shop item resolved live for one player.
 *
 * @param id         provider identifier used to purchase
 * @param name       display name (already colorized by the provider)
 * @param cost       price in the item's currency
 * @param currency   human-readable currency name (e.g. "Iron", "Gold", "Emerald")
 * @param affordable whether the player can currently afford it
 * @param tier       current tier label for tiered items (e.g. "II"); empty if untiered
 */
public record ShopContent(String id, String name, int cost, String currency,
                          boolean affordable, String tier) {}
