package it.pintux.life.essentialsaddon.listener;

import it.pintux.life.essentialsaddon.api.BedrockPlayerDetector;
import it.pintux.life.essentialsaddon.service.BedrockEssentialsService;
import it.pintux.life.essentialsaddon.service.BedrockHomeService;
import it.pintux.life.essentialsaddon.service.BedrockTpaService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class EssentialsCommandListener implements Listener {
    private final BedrockPlayerDetector bedrockPlayerDetector;
    private BedrockEssentialsService service;
    private BedrockHomeService homeService;
    private BedrockTpaService tpaService;

    /**
     * The detector is taken directly rather than through {@link BedrockEssentialsService} so this
     * listener still works on a server that enables only homes or only TPA — that service is
     * built by the warps/kits module and is null otherwise.
     */
    public EssentialsCommandListener(BedrockPlayerDetector bedrockPlayerDetector) {
        this.bedrockPlayerDetector = bedrockPlayerDetector;
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
        if (message == null) return;
        String lower = message.toLowerCase();

        if (lower.equals("/warp") || lower.equals("/warps")) {
            if (service != null) {
                event.setCancelled(true);
                service.openWarpMenu(event.getPlayer());
            }
        } else if (lower.equals("/kit") || lower.equals("/kits")) {
            if (service != null) {
                event.setCancelled(true);
                service.openKitMenu(event.getPlayer());
            }
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
