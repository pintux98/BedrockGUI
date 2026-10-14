package it.pintux.life.essentialsaddon.listener;

import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import it.pintux.life.essentialsaddon.util.CommandAliases;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class McPetsCommandListener implements Listener {

    private final BedrockPetService service;
    private final EssentialsAddonConfiguration configuration;

    public McPetsCommandListener(BedrockPetService service, EssentialsAddonConfiguration configuration) {
        this.service = service;
        this.configuration = configuration;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (!configuration.commandMcPets().matchesMessage(message)
                || CommandAliases.argsOf(message).length > 0
                || !service.shouldHandle(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        service.openPetList(event.getPlayer());
    }
}
