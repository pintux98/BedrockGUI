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
        String message = event.getMessage();
        if (message == null) {
            return;
        }
        String[] parts = message.trim().split("\\s+");
        if (parts.length == 0) {
            return;
        }
        String first = parts[0].toLowerCase(Locale.ROOT);
        boolean bedrock = service.shouldHandle(event.getPlayer());

        // /pet (+ alias /pets) is handled here instead of via a registered command, so it keeps
        // working after a PlugMan-style reload (a registered PluginCommand would point at the old,
        // now-disabled plugin instance and throw "plugin is disabled"). Bedrock opens the form;
        // Java is forwarded to MyPet's own /petlist.
        if (first.equals("/pet") || first.equals("/pets")) {
            event.setCancelled(true);
            if (bedrock) {
                service.openPetList(event.getPlayer());
            } else {
                event.getPlayer().performCommand(joinArgs("petlist", parts));
            }
            return;
        }

        if (!bedrock) {
            return;
        }
        // MyPet's own commands, intercepted only for Bedrock players (Java passes through to MyPet).
        if (first.equals("/petshop")) {
            event.setCancelled(true);
            service.openPetShop(event.getPlayer());
        } else if (first.equals("/pcst")) {
            event.setCancelled(true);
            service.openSkilltreeForm(event.getPlayer());
        }
    }

    private String joinArgs(String base, String[] parts) {
        StringBuilder builder = new StringBuilder(base);
        for (int i = 1; i < parts.length; i++) {
            builder.append(' ').append(parts[i]);
        }
        return builder.toString();
    }
}
