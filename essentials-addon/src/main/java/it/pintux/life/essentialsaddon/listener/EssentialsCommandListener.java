package it.pintux.life.essentialsaddon.listener;

import it.pintux.life.essentialsaddon.service.BedrockEssentialsService;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.service.BedrockTpaService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class EssentialsCommandListener implements Listener {
    private final BedrockEssentialsService service;
    private BedrockHomeService homeService;
    private BedrockTpaService tpaService;

    public EssentialsCommandListener(BedrockEssentialsService service) {
        this.service = service;
    }

    public void setHomeService(BedrockHomeService homeService) {
        this.homeService = homeService;
    }

    public void setTpaService(BedrockTpaService tpaService) {
        this.tpaService = tpaService;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!service.shouldHandle(event.getPlayer())) {
            return;
        }

        String message = event.getMessage();
        if (message == null) return;
        String lower = message.toLowerCase();

        if (lower.equals("/warp") || lower.equals("/warps")) {
            event.setCancelled(true);
            service.openWarpMenu(event.getPlayer());
        } else if (lower.equals("/kit") || lower.equals("/kits")) {
            event.setCancelled(true);
            service.openKitMenu(event.getPlayer());
        } else if (lower.equals("/home") || lower.equals("/homes")) {
            if (homeService != null) {
                event.setCancelled(true);
                homeService.openHomeMenu(event.getPlayer(), 1);
            }
        } else if (lower.equals("/sethome")) {
            if (homeService != null) {
                event.setCancelled(true);
                homeService.showSetHomeForm(event.getPlayer());
            }
        } else if (lower.equals("/delhome")) {
            if (homeService != null) {
                event.setCancelled(true);
                homeService.showDeleteHomeForm(event.getPlayer());
            }
        } else if (lower.equals("/tpa") || lower.equals("/tpahere")
                || lower.equals("/tpaccept") || lower.equals("/tpdeny")
                || lower.equals("/tpacancel")) {
            if (tpaService != null) {
                event.setCancelled(true);
                tpaService.openTpaMenu(event.getPlayer());
            }
        }
    }
}
