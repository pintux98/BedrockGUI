package it.pintux.life.bedwarsaddon.listener;

import it.pintux.life.bedwarsaddon.service.BedrockArenaService;
import it.pintux.life.bedwarsaddon.service.BedrockStatsService;
import it.pintux.life.bedwarsaddon.service.BedrockUpgradeService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.Plugin;

/**
 * Intercepts the native chest GUIs that have no dedicated open-event:
 * the arena selector (detected by inventory holder) and the team-upgrades menu
 * (detected via the API's "watching upgrades" state). Each is replaced with a
 * Cumulus form for Bedrock players; Java players are untouched.
 */
public final class MenuInterceptListener implements Listener {
    private final Plugin plugin;
    private final BedrockArenaService arenaService;     // nullable (module disabled)
    private final BedrockUpgradeService upgradeService; // nullable (module disabled)
    private final BedrockStatsService statsService;     // nullable (module disabled)

    public MenuInterceptListener(Plugin plugin, BedrockArenaService arenaService,
                                 BedrockUpgradeService upgradeService, BedrockStatsService statsService) {
        this.plugin = plugin;
        this.arenaService = arenaService;
        this.upgradeService = upgradeService;
        this.statsService = statsService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (arenaService != null && arenaService.shouldHandle(player)
                && arenaService.ownsInventory(event.getInventory())) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> arenaService.openMain(player));
            return;
        }

        // Stats GUI: null holder, no flag — identified by its (config-matched) title, which is
        // available now, so we can cancel directly and avoid any chest flash.
        if (statsService != null && statsService.shouldHandle(player)
                && statsService.matchesTitle(event.getView().getTitle())) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> statsService.openStats(player));
            return;
        }

        // The upgrades menu sets its "watching" flag AFTER calling openInventory (and uses a null
        // inventory holder), so it cannot be detected during this event. Re-check next tick: if the
        // player is now flagged, the inventory that just opened was the upgrades/traps menu -> close
        // it and show the Cumulus form instead.
        if (upgradeService != null && upgradeService.shouldHandle(player)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (upgradeService.isWatching(player)) {
                    upgradeService.stopWatching(player);
                    player.closeInventory();
                    upgradeService.openMain(player);
                }
            });
        }
    }
}
