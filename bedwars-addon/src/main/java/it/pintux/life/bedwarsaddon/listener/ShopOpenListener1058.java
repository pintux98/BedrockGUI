package it.pintux.life.bedwarsaddon.listener;

import com.andrei1058.bedwars.api.events.shop.ShopOpenEvent;
import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/** BedWars1058 shop interception — cancels the native chest and opens the Cumulus form for Bedrock players. */
public final class ShopOpenListener1058 implements Listener {
    private final Plugin plugin;
    private final BedrockShopService shopService;

    public ShopOpenListener1058(Plugin plugin, BedrockShopService shopService) {
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
        plugin.getServer().getScheduler().runTask(plugin, () -> shopService.openMain(player));
    }
}
