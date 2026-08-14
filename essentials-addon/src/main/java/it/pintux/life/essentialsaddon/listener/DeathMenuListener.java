package it.pintux.life.essentialsaddon.listener;

import it.pintux.life.essentialsaddon.service.BedrockDeathService;
import it.pintux.life.essentialsaddon.util.MainThread;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Sends the respawn menu once the vanilla Bedrock death screen is gone. Forms pushed during
 * the death screen itself get swallowed by the client, so this waits for the respawn.
 */
public final class DeathMenuListener implements Listener {
    private final BedrockDeathService deathService;

    public DeathMenuListener(BedrockDeathService deathService) {
        this.deathService = deathService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!deathService.shouldHandle(player)) {
            return;
        }
        MainThread.runLater(() -> deathService.openDeathMenu(player), deathService.formDelayTicks());
    }
}
