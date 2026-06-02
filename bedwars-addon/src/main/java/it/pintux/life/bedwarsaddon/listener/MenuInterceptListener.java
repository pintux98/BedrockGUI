package it.pintux.life.bedwarsaddon.listener;

import it.pintux.life.bedwarsaddon.service.BedrockArenaService;
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

    public MenuInterceptListener(Plugin plugin, BedrockArenaService arenaService, BedrockUpgradeService upgradeService) {
        this.plugin = plugin;
        this.arenaService = arenaService;
        this.upgradeService = upgradeService;
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

        if (upgradeService != null && upgradeService.shouldHandle(player)
                && upgradeService.isWatching(player)) {
            event.setCancelled(true);
            upgradeService.stopWatching(player); // clear native state we just cancelled
            plugin.getServer().getScheduler().runTask(plugin, () -> upgradeService.openMain(player));
        }
    }
}
