package it.pintux.life.essentialsaddon.listener;

import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.service.BedrockEssentialsService;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.service.BedrockTpaService;
import it.pintux.life.essentialsaddon.util.CommandAliases;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class EssentialsCommandListener implements Listener {
    private final BedrockPlayerDetector bedrockPlayerDetector;
    private final EssentialsAddonConfiguration configuration;
    private BedrockEssentialsService service;
    private BedrockHomeService homeService;
    private BedrockTpaService tpaService;

    /**
     * The detector is taken directly rather than through {@link BedrockEssentialsService} so this
     * listener still works on a server that enables only homes or only TPA — that service is
     * built by the warps/kits module and is null otherwise.
     */
    public EssentialsCommandListener(BedrockPlayerDetector bedrockPlayerDetector,
                                     EssentialsAddonConfiguration configuration) {
        this.bedrockPlayerDetector = bedrockPlayerDetector;
        this.configuration = configuration;
    }

    public void setService(BedrockEssentialsService service) {
        this.service = service;
    }

    public void setHomeService(BedrockHomeService homeService) {
        this.homeService = homeService;
    }

    public void setTpaService(BedrockTpaService tpaService) {
        this.tpaService = tpaService;
    }

    public boolean hasAnyService() {
        return service != null || homeService != null || tpaService != null;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!bedrockPlayerDetector.isBedrockPlayer(event.getPlayer())) {
            return;
        }

        String message = event.getMessage();
        String root = CommandAliases.rootOf(message);
        if (root == null) {
            return;
        }
        // Arguments mean the player asked for something specific (/home base, /tpa Steve), so the
        // backing plugin handles it and no form is shown.
        if (CommandAliases.argsOf(message).length > 0) {
            return;
        }

        if (configuration.commandWarps().matches(root)) {
            if (service != null) {
                event.setCancelled(true);
                service.openWarpMenu(event.getPlayer());
            }
        } else if (configuration.commandKits().matches(root)) {
            if (service != null) {
                event.setCancelled(true);
                service.openKitMenu(event.getPlayer());
            }
        } else if (configuration.commandHomes().matches(root)) {
            if (homeService != null) {
                event.setCancelled(true);
                homeService.openHomeMenu(event.getPlayer(), 1);
            }
        } else if (configuration.commandSetHome().matches(root)) {
            if (homeService != null) {
                event.setCancelled(true);
                homeService.showSetHomeForm(event.getPlayer());
            }
        } else if (configuration.commandDeleteHome().matches(root)) {
            if (homeService != null) {
                event.setCancelled(true);
                homeService.showDeleteHomeForm(event.getPlayer());
            }
        } else if (configuration.commandTpa().matches(root)) {
            if (tpaService != null) {
                event.setCancelled(true);
                tpaService.openTpaMenu(event.getPlayer());
            }
        }
    }
}
