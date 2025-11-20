package it.pintux.life.velocity.platform;

import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.platform.PlatformFormSender;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.velocity.utils.VelocityPlayer;
import org.geysermc.cumulus.form.Form;

import java.util.UUID;

public class VelocityFormSender implements PlatformFormSender {

    private final ProxyServer server;

    public VelocityFormSender(ProxyServer server) {
        this.server = server;
    }

    @Override
    public boolean sendForm(FormPlayer player, Form form) {
        if (!(player instanceof VelocityPlayer)) {
            return false;
        }
        
        VelocityPlayer velocityPlayer = (VelocityPlayer) player;
        if (!velocityPlayer.isOnline()) {
            return false;
        }
        
        // Forms need to be handled on the backend server, not proxy
        // Send form data to the backend server where the player is connected
        velocityPlayer.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
            // This would need proper form serialization
            // For now, just return false as forms are backend-specific
        });
        
        return false;
    }

    @Override
    public boolean isBedrockPlayer(UUID playerUuid) {
        // Check if player is a Bedrock player using Floodgate
        return server.getPlayer(playerUuid)
                .map(player -> {
                    try {
                        return org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(playerUuid);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .orElse(false);
    }

    @Override
    public boolean isFormSystemAvailable() {
        // Form system is available if Floodgate is available
        try {
            return org.geysermc.floodgate.api.FloodgateApi.getInstance() != null;
        } catch (Exception e) {
            return false;
        }
    }
}