package it.pintux.life.essentialsaddon.backend;

import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.service.BedrockShopGuiService;
import it.pintux.life.essentialsaddon.util.CommandAliases;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Set;

public final class ShopGuiPlusBackend implements ShopBackend {
    private static final Set<String> ADMIN_SUBCOMMANDS =
            Set.of("reload", "check", "addmodifier", "resetmodifier", "checkmodifiers");

    private final Plugin plugin;
    private final BedrockShopGuiService service;
    private final EssentialsAddonConfiguration configuration;

    public ShopGuiPlusBackend(Plugin plugin, BedrockShopGuiService service,
                              EssentialsAddonConfiguration configuration) {
        this.plugin = plugin;
        this.service = service;
        this.configuration = configuration;
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

        String root = CommandAliases.rootOf(event.getMessage());
        if (root == null) {
            return false;
        }
        String[] args = CommandAliases.argsOf(event.getMessage());

        if (configuration.commandShop().matches(root)) {
            if (args.length == 0) {
                event.setCancelled(true);
                service.openMainMenu(player);
                return true;
            }
            if (!ADMIN_SUBCOMMANDS.contains(args[0].toLowerCase(Locale.ROOT))) {
                event.setCancelled(true);
                service.openShop(player, args[0], 1);
                return true;
            }
            return false;
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
        if (!service.looksLikeShopGuiInventory(event.getView().getTitle(), event.getInventory().getHolder())) {
            return false;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> service.openFromInventoryTitle(player, event.getView().getTitle()));
        return true;
    }
}

