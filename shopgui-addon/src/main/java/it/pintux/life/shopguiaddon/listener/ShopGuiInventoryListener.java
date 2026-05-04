package it.pintux.life.shopguiaddon.listener;

import it.pintux.life.shopguiaddon.service.BedrockShopGuiService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShopGuiInventoryListener implements Listener {
    private final JavaPlugin plugin;
    private final BedrockShopGuiService service;

    public ShopGuiInventoryListener(JavaPlugin plugin, BedrockShopGuiService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !service.shouldHandle(player)) {
            return;
        }
        if (!service.looksLikeShopGuiInventory(event.getView().getTitle(), event.getInventory().getHolder())) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> service.openFromInventoryTitle(player, event.getView().getTitle()));
    }
}
