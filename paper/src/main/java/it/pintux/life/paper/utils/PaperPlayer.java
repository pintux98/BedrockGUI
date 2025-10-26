package it.pintux.life.paper.utils;

import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PaperPlayer implements FormPlayer {
    private final Player player;

    public PaperPlayer(Player player) {
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


    public Player getBukkitPlayer() {
        return player;
    }
}

