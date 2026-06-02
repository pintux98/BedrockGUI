package it.pintux.life.bedwarsaddon.model;

/**
 * A team-upgrade menu entry resolved for one player.
 * Cost/feedback are handled by BedWars on purchase, so only id + display name are needed.
 *
 * @param id   provider identifier (MenuContent name) used to purchase
 * @param name display name (from the upgrade's display item)
 */
public record UpgradeContent(String id, String name) {}
