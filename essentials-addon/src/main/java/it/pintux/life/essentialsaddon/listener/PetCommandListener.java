package it.pintux.life.essentialsaddon.listener;

import it.pintux.life.essentialsaddon.config.EssentialsAddonConfiguration;
import it.pintux.life.essentialsaddon.service.BedrockPetService;
import it.pintux.life.essentialsaddon.util.CommandAliases;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class PetCommandListener implements Listener {

    private final BedrockPetService service;
    private final EssentialsAddonConfiguration configuration;

    public PetCommandListener(BedrockPetService service, EssentialsAddonConfiguration configuration) {
        this.service = service;
        this.configuration = configuration;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null) {
            return;
        }
        String root = CommandAliases.rootOf(message);
        if (root == null) {
            return;
        }
        String[] args = CommandAliases.argsOf(message);
        boolean bedrock = service.shouldHandle(event.getPlayer());

        // /pet (+ alias /pets) is handled here instead of via a registered command, so it keeps
        // working after a PlugMan-style reload (a registered PluginCommand would point at the old,
        // now-disabled plugin instance and throw "plugin is disabled"). Bedrock opens the form;
        // Java is forwarded to MyPet's own /petlist.
        if (configuration.commandPets().matches(root)) {
            event.setCancelled(true);
            if (bedrock) {
                service.openPetList(event.getPlayer());
            } else {
                event.getPlayer().performCommand(joinArgs("petlist", args));
            }
            return;
        }

        if (!bedrock) {
            return;
        }
        // MyPet's own commands, intercepted only for Bedrock players (Java passes through to MyPet).
        if (configuration.commandPetShop().matches(root)) {
            event.setCancelled(true);
            service.openPetShop(event.getPlayer());
        } else if (configuration.commandPetSkilltree().matches(root)) {
            event.setCancelled(true);
            service.openSkilltreeForm(event.getPlayer());
        }
    }

    private String joinArgs(String base, String[] args) {
        StringBuilder builder = new StringBuilder(base);
        for (String arg : args) {
            builder.append(' ').append(arg);
        }
        return builder.toString();
    }
}
