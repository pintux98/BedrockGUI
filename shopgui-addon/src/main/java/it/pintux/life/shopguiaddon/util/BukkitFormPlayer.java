package it.pintux.life.shopguiaddon.util;

import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class BukkitFormPlayer implements FormPlayer {
    private final Player player;

    public BukkitFormPlayer(Player player) {
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(message);
    }

    @Override
    public boolean executeAction(String action) {
        player.chat(action);
        return true;
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    public Player getPlayer() {
        return player;
    }
}
