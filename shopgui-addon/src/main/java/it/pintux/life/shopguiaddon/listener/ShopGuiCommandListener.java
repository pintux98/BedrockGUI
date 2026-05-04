package it.pintux.life.shopguiaddon.listener;

import it.pintux.life.shopguiaddon.service.BedrockShopGuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

public final class ShopGuiCommandListener implements Listener {
    private final BedrockShopGuiService service;

    public ShopGuiCommandListener(BedrockShopGuiService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!service.shouldHandle(player)) {
            return;
        }
        String[] parts = event.getMessage().substring(1).split("\\s+");
        if (parts.length == 0) {
            return;
        }
        String label = parts[0].toLowerCase(Locale.ROOT);
        if (label.equals("shop") || label.equals("shopgui") || label.equals("guishop")) {
            if (parts.length == 1) {
                event.setCancelled(true);
                service.openMainMenu(player);
                return;
            }
            String firstArg = parts[1].toLowerCase(Locale.ROOT);
            if (!firstArg.equals("reload") && !firstArg.equals("check") && !firstArg.equals("addmodifier") && !firstArg.equals("resetmodifier") && !firstArg.equals("checkmodifiers")) {
                event.setCancelled(true);
                service.openShop(player, parts[1], 1);
            }
            return;
        }
        if (label.equals("sell") && parts.length >= 2 && parts[1].equalsIgnoreCase("all")) {
            event.setCancelled(true);
            service.openMainMenu(player);
        }
    }
}
