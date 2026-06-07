package it.pintux.life.bedwarsaddon.listener;

import com.tomkeuper.bedwars.api.events.shop.ShopOpenEvent;
import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class ShopOpenListener implements Listener {
    private final Plugin plugin;
    private final BedrockShopService shopService;

    public ShopOpenListener(Plugin plugin, BedrockShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShopOpen(ShopOpenEvent event) {
        Player player = event.getPlayer();
        if (!shopService.shouldHandle(player)) {
            return; // Java player: native chest GUI proceeds untouched.
        }
        event.setCancelled(true);
        // Open the form next tick so the cancelled inventory open fully settles first.
        plugin.getServer().getScheduler().runTask(plugin, () -> shopService.openMain(player));
    }
}
