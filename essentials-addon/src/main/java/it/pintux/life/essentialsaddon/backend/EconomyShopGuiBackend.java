package it.pintux.life.essentialsaddon.backend;

import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.service.BedrockEconomyShopService;
import it.pintux.life.essentialsaddon.util.CommandAliases;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

public final class EconomyShopGuiBackend implements ShopBackend {
    private final Plugin plugin;
    private final BedrockEconomyShopService service;
    private final EssentialsAddonConfiguration configuration;

    public EconomyShopGuiBackend(Plugin plugin, BedrockEconomyShopService service,
                                 EssentialsAddonConfiguration configuration) {
        this.plugin = plugin;
        this.service = service;
        this.configuration = configuration;
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

        String root = CommandAliases.rootOf(event.getMessage());
        if (root == null) {
            return false;
        }
        String[] args = CommandAliases.argsOf(event.getMessage());

        if (configuration.commandShop().matches(root)) {
            event.setCancelled(true);
            if (args.length == 0) {
                service.openMainMenu(player);
            } else {
                service.openShop(player, args[0], 1);
            }
            return true;
        }

        if (configuration.commandSellAll().matches(root) && sellsEverything(args)) {
            event.setCancelled(true);
            service.openMainMenu(player);
            return true;
        }

        return false;
    }

    private boolean sellsEverything(String[] args) {
        return args.length == 0 || args[0].equalsIgnoreCase("all");
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

