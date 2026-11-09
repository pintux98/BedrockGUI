package it.pintux.life.bungee.utils;

import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.LegacyColors;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class BungeePlayer implements FormPlayer {
    private final ProxiedPlayer player;
    public BungeePlayer(ProxiedPlayer player) { this.player = player; }
    public static BaseComponent text(String legacy) { return TextComponent.fromLegacy(legacy == null ? "" : LegacyColors.translate(legacy)); }
    @Override
    public UUID getUniqueId() { return player.getUniqueId(); }
    @Override
    public String getName() { return player.getName(); }
    @Override
    public void sendMessage(String message) { player.sendMessage(text(message)); }
    @Override
    public boolean executeAction(String action) { return false; }
    @Override
    public boolean hasPermission(String permission) { return player.hasPermission(permission); }
    public boolean isOnline() { return player.isConnected(); }
    public ProxiedPlayer getPlayer() { return player; }
}
