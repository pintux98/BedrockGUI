package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformFormSender;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.bungee.utils.BungeePlayer;
import org.geysermc.cumulus.form.Form;

import java.util.UUID;

public class BungeeFormSender implements PlatformFormSender {
    @Override
    public boolean sendForm(FormPlayer player, Form form) {
        if (!(player instanceof BungeePlayer)) return false;
        BungeePlayer p = (BungeePlayer) player;
        return p.isOnline();
    }

    @Override
    public boolean isBedrockPlayer(UUID playerUuid) {
        try {
            return org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(playerUuid);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isFormSystemAvailable() {
        try {
            return org.geysermc.floodgate.api.FloodgateApi.getInstance() != null;
        } catch (Exception e) {
            return false;
        }
    }
}