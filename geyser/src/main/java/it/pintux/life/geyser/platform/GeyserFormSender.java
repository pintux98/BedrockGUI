package it.pintux.life.geyser.platform;

import it.pintux.life.common.platform.PlatformFormSender;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.geyser.utils.GeyserPlayer;
import org.geysermc.cumulus.form.Form;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.UUID;

/**
 * Geyser implementation of PlatformFormSender using Geyser API.
 */
public class GeyserFormSender implements PlatformFormSender {
    
    @Override
    public boolean sendForm(FormPlayer player, Form form) {
        try {
            if (!(player instanceof GeyserPlayer)) {
                return false;
            }
            
            GeyserPlayer geyserPlayer = (GeyserPlayer) player;
            // Connection should be valid if we got this far
            
            geyserPlayer.sendForm(form);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean isBedrockPlayer(UUID playerUuid) {
        try {
            GeyserConnection connection = GeyserApi.api().connectionByUuid(playerUuid);
            return connection != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean isFormSystemAvailable() {
        try {
            return GeyserApi.api() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets a GeyserConnection by UUID
     * @param uuid the player's UUID
     * @return the GeyserConnection or null if not found
     */
    public GeyserConnection getConnection(UUID uuid) {
        try {
            return GeyserApi.api().connectionByUuid(uuid);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Creates a GeyserPlayer from a UUID
     * @param uuid the player's UUID
     * @return the GeyserPlayer or null if not found
     */
    public GeyserPlayer getGeyserPlayer(UUID uuid) {
        GeyserConnection connection = getConnection(uuid);
        return connection != null ? new GeyserPlayer(connection) : null;
    }
}