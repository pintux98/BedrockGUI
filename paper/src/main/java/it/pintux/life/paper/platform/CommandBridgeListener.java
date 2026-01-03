package it.pintux.life.paper.platform;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class CommandBridgeListener implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!"bedrockgui:cmd".equalsIgnoreCase(channel)) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String uuidStr = in.readUTF();
            String cmd = in.readUTF();
            UUID uuid = UUID.fromString(uuidStr);
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && target.isOnline()) {
                Bukkit.dispatchCommand(target, cmd);
            }
        } catch (Exception ignored) {
        }
    }
}

