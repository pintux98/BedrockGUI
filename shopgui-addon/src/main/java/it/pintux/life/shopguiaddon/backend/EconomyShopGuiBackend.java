package it.pintux.life.shopguiaddon.backend;

import it.pintux.life.shopguiaddon.service.BedrockEconomyShopService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

public final class EconomyShopGuiBackend implements ShopBackend {
    private final Plugin plugin;
    private final BedrockEconomyShopService service;

    public EconomyShopGuiBackend(Plugin plugin, BedrockEconomyShopService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public int priority() {
        // Lower than ShopGUI+ so /shop prefers ShopGUI+ if both are installed.
        return 50;
    }

    @Override
    public void bootstrap() {
        // EconomyShopGUI bootstraps itself via ShopItemsLoadEvent; nothing extra here.
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
            event.setCancelled(true);
            if (parts.length == 1) {
                service.openMainMenu(player);
            } else {
                service.openShop(player, parts[1], 1);
            }
            return true;
        }

        if (label.equals("sellall")) {
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
        if (!service.looksLikeInventory(event.getView().getTitle(), event.getInventory().getHolder())) {
            return false;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> service.openFromInventoryTitle(player, event.getView().getTitle()));
        return true;
    }
}

