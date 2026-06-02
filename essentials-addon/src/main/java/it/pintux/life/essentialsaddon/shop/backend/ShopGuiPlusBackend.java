package it.pintux.life.essentialsaddon.shop.backend;

import it.pintux.life.shopguiaddon.service.BedrockShopGuiService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

public final class ShopGuiPlusBackend implements ShopBackend {
    private final Plugin plugin;
    private final BedrockShopGuiService service;

    public ShopGuiPlusBackend(Plugin plugin, BedrockShopGuiService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public int priority() {
        // Prefer ShopGUI+ for /shop when both backends are installed.
        return 100;
    }

    @Override
    public void bootstrap() {
        // ShopGUI+ bootstraps itself via its lifecycle listener; nothing extra here.
    }

    @Override
    public boolean handleCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!service.shouldHandle(player)) {
            return false;
        }

        String[] parts = event.getMessage().substring(1).split("\\s+");
        if (parts.length == 0) {
            return false;
        }
        String label = parts[0].toLowerCase(Locale.ROOT);

        if (label.equals("shop") || label.equals("shopgui") || label.equals("guishop")) {
            if (parts.length == 1) {
                event.setCancelled(true);
                service.openMainMenu(player);
                return true;
            }
            String firstArg = parts[1].toLowerCase(Locale.ROOT);
            if (!firstArg.equals("reload") && !firstArg.equals("check") && !firstArg.equals("addmodifier")
                    && !firstArg.equals("resetmodifier") && !firstArg.equals("checkmodifiers")) {
                event.setCancelled(true);
                service.openShop(player, parts[1], 1);
                return true;
            }
            return false;
        }

        if (label.equals("sell") && parts.length >= 2 && parts[1].equalsIgnoreCase("all")) {
            event.setCancelled(true);
            service.openMainMenu(player);
            return true;
        }

        return false;
    }

    @Override
    public boolean handleInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !service.shouldHandle(player)) {
            return false;
        }
        if (!service.looksLikeShopGuiInventory(event.getView().getTitle(), event.getInventory().getHolder())) {
            return false;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> service.openFromInventoryTitle(player, event.getView().getTitle()));
        return true;
    }
}

