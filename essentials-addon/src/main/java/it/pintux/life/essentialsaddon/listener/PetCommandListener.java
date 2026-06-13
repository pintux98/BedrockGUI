package it.pintux.life.essentialsaddon.listener;

import it.pintux.life.essentialsaddon.service.BedrockPetService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

public final class PetCommandListener implements Listener {

    private final BedrockPetService service;

    public PetCommandListener(BedrockPetService service) {
        this.service = service;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!service.shouldHandle(event.getPlayer())) {
            return;
        }
        String message = event.getMessage();
        if (message == null) {
            return;
        }
        String lower = message.toLowerCase(Locale.ROOT).trim();
        String first = lower.split("\\s+")[0];

        if (first.equals("/petshop")) {
            event.setCancelled(true);
            service.openPetShop(event.getPlayer());
        } else if (first.equals("/pet") || first.equals("/pets")) {
            event.setCancelled(true);
            service.openPetList(event.getPlayer());
        } else if (first.equals("/pcst")) {
            event.setCancelled(true);
            service.openSkilltreeForm(event.getPlayer());
        }
    }
}
