package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformFormSender;
import it.pintux.life.common.utils.FormPlayer;
import org.geysermc.cumulus.form.Form;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

/**
 * Bungeecord implementation of PlatformFormSender using Floodgate API.
 */
public class BungeeFormSender implements PlatformFormSender {
    
    @Override
    public boolean sendForm(FormPlayer player, Form form) {
        try {
            if (isFormSystemAvailable() && isBedrockPlayer(player.getUniqueId())) {
                FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean isBedrockPlayer(UUID playerUuid) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(playerUuid);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean isFormSystemAvailable() {
        try {
            return FloodgateApi.getInstance() != null;
        } catch (Exception e) {
            return false;
        }
    }
}