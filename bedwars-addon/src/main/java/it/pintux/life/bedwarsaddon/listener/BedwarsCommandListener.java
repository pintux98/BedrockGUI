package it.pintux.life.bedwarsaddon.listener;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.service.BedrockArenaService;
import it.pintux.life.bedwarsaddon.service.BedrockPartyService;
import it.pintux.life.bedwarsaddon.service.BedrockShopService;
import it.pintux.life.bedwarsaddon.service.BedrockSpectatorService;
import it.pintux.life.bedwarsaddon.service.BedrockStatsService;
import it.pintux.life.bedwarsaddon.service.BedrockUpgradeService;
import it.pintux.life.bedwarsaddon.util.CommandAliases;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

public final class BedwarsCommandListener implements Listener {
    private final Plugin plugin;
    private final BedrockArenaService arenaService;
    private final BedrockStatsService statsService;
    private final BedrockPartyService partyService;
    private final BedrockShopService shopService;
    private final BedrockUpgradeService upgradeService;
    private final BedrockSpectatorService spectatorService;

    private final CommandAliases arenaCommands;
    private final CommandAliases statsCommands;
    private final CommandAliases partyCommands;
    private final CommandAliases shopCommands;
    private final CommandAliases upgradeCommands;
    private final CommandAliases spectatorCommands;

    public BedwarsCommandListener(Plugin plugin, BedwarsAddonConfiguration configuration,
                                  BedrockArenaService arenaService, BedrockStatsService statsService,
                                  BedrockPartyService partyService, BedrockShopService shopService,
                                  BedrockUpgradeService upgradeService,
                                  BedrockSpectatorService spectatorService) {
        this.plugin = plugin;
        this.arenaService = arenaService;
        this.statsService = statsService;
        this.partyService = partyService;
        this.shopService = shopService;
        this.upgradeService = upgradeService;
        this.spectatorService = spectatorService;
        this.arenaCommands = configuration.commandAliases("commands.arena");
        this.statsCommands = configuration.commandAliases("commands.stats");
        this.partyCommands = configuration.commandAliases("commands.party");
        this.shopCommands = configuration.commandAliases("commands.shop");
        this.upgradeCommands = configuration.commandAliases("commands.upgrades");
        this.spectatorCommands = configuration.commandAliases("commands.spectator");
    }

    public boolean hasAnyCommand() {
        return !arenaCommands.isEmpty() || !statsCommands.isEmpty() || !partyCommands.isEmpty()
                || !shopCommands.isEmpty() || !upgradeCommands.isEmpty() || !spectatorCommands.isEmpty();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String root = CommandAliases.rootOf(event.getMessage());
        if (root == null || CommandAliases.argsOf(event.getMessage()).length > 0) {
            return;
        }

        if (arenaCommands.matches(root)) {
            open(event, arenaService == null || !arenaService.shouldHandle(player) ? null
                    : () -> arenaService.openMain(player));
        } else if (statsCommands.matches(root)) {
            open(event, statsService == null || !statsService.shouldHandle(player) ? null
                    : () -> statsService.openStats(player));
        } else if (partyCommands.matches(root)) {
            open(event, partyService == null || !partyService.shouldHandle(player) ? null
                    : () -> partyService.openMain(player));
        } else if (shopCommands.matches(root)) {
            open(event, shopService == null || !shopService.shouldHandle(player) ? null
                    : () -> shopService.openMain(player));
        } else if (upgradeCommands.matches(root)) {
            open(event, upgradeService == null || !upgradeService.shouldHandle(player) ? null
                    : () -> upgradeService.openMain(player));
        } else if (spectatorCommands.matches(root)) {
            open(event, spectatorService == null || !spectatorService.shouldHandle(player) ? null
                    : () -> spectatorService.openTeleporter(player));
        }
    }

    private void open(PlayerCommandPreprocessEvent event, Runnable action) {
        if (action == null) {
            return;
        }
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, action);
    }
}
