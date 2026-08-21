package it.pintux.life.homesteadaddon.listener;

import it.pintux.life.homesteadaddon.config.HomesteadAddonConfiguration;
import it.pintux.life.homesteadaddon.service.BedrockRegionService;
import it.pintux.life.homesteadaddon.util.CommandAliases;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class HomesteadCommandListener implements Listener {
    private final BedrockRegionService regionService;
    private final CommandAliases regionCommands;
    private final CommandAliases adminCommands;
    private final CommandAliases guiSubcommands;

    public HomesteadCommandListener(BedrockRegionService regionService, HomesteadAddonConfiguration configuration) {
        this.regionService = regionService;
        this.regionCommands = configuration.commandAliases(
                "commands.regions", "region", "rg", "hs", "homestead");
        this.adminCommands = configuration.commandAliases(
                "commands.admin", "homesteadadmin", "hsadmin");
        this.guiSubcommands = configuration.commandAliases(
                "commands.gui-subcommands", "menu", "gui");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!regionService.shouldHandle(player)) {
            return;
        }
        String root = CommandAliases.rootOf(event.getMessage());
        if (root == null) {
            return;
        }
        String[] args = CommandAliases.argsOf(event.getMessage());
        boolean guiInvocation = args.length == 0 || guiSubcommands.matches(args[0]);
        if (!guiInvocation) {
            return;
        }

        if (regionCommands.matches(root)) {
            event.setCancelled(true);
            regionService.openRegionList(player, false, 1);
        } else if (adminCommands.matches(root)) {
            event.setCancelled(true);
            regionService.openRegionList(player, true, 1);
        }
    }
}
