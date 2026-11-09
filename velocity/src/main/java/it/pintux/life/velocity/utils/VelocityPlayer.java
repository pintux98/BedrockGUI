package it.pintux.life.velocity.utils;

import com.velocitypowered.api.proxy.Player;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.LegacyColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

public class VelocityPlayer implements FormPlayer {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final Player player;

    public VelocityPlayer(Player player) {
        this.player = player;
    }

    public static Component text(String legacy) {
        return LEGACY.deserialize(legacy == null ? "" : LegacyColors.translate(legacy));
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getUsername();
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(text(message));
    }

    @Override
    public boolean executeAction(String action) {
        String cmd = action == null ? "" : action.trim();
        if (cmd.isEmpty() || player.getCurrentServer().isEmpty()) {
            return false;
        }
        player.spoofChatInput(cmd.startsWith("/") ? cmd : "/" + cmd);
        return true;
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    public boolean isOnline() {
        return player.isActive();
    }

    public Player getPlayer() {
        return player;
    }

}
