package it.pintux.life.geyser.utils;

import it.pintux.life.common.utils.FormPlayer;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.cumulus.form.Form;

import java.util.UUID;

public class GeyserPlayer implements FormPlayer {
    
    private final GeyserConnection connection;
    
    public GeyserPlayer(GeyserConnection connection) {
        this.connection = connection;
    }
    
    @Override
    public UUID getUniqueId() {
        return connection.javaUuid();
    }
    
    @Override
    public String getName() {
        return connection.javaUsername();
    }
    
    @Override
    public void sendMessage(String message) {
        // Send message to the player
        connection.sendMessage(message);
    }
    
    public void sendForm(Object form) {
        if (form instanceof Form) {
            connection.sendForm((Form) form);
        } else {
            throw new IllegalArgumentException("Form must be a Cumulus Form instance");
        }
    }
    
    @Override
    public boolean hasPermission(String permission) {
        // Geyser connections don't have built-in permission system
        // This would need to be integrated with the backend server's permission system
        return true; // Default to true for now
    }
    
    @Override
    public boolean executeAction(String action) {
        sendMessage("[Action] " + action);
        return true;
    }
    
    public void executeCommand(String command) {
        // Execute command as the player
        // Note: GeyserConnection doesn't support direct command execution
        // This would need to be handled by the backend server
        sendMessage("[Command] " + command);
    }
    
    public boolean isOnline() {
        return connection != null;
    }
    
    public String getDisplayName() {
        return connection.bedrockUsername();
    }
    
    public void teleport(double x, double y, double z) {
        // Teleportation would need to be handled by the backend server
        // This is a placeholder implementation
        executeCommand(String.format("tp %s %.2f %.2f %.2f", getName(), x, y, z));
    }
    
    public void teleport(double x, double y, double z, float yaw, float pitch) {
        // Teleportation with rotation would need to be handled by the backend server
        executeCommand(String.format("tp %s %.2f %.2f %.2f %.2f %.2f", getName(), x, y, z, yaw, pitch));
    }
    
    public void playSound(String sound, float volume, float pitch) {
        // Sound playing would need to be handled by the backend server
        executeCommand(String.format("playsound %s @s ~ ~ ~ %.2f %.2f", sound, volume, pitch));
    }
    
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        // Title sending would need to be handled by the backend server
        // For now, send as regular messages
        if (title != null && !title.isEmpty()) {
            sendMessage("[Title] " + title);
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            sendMessage("[Subtitle] " + subtitle);
        }
    }
    
    public void sendActionBar(String message) {
        // Action bar would need to be handled by the backend server
        // For now, send as regular message
        sendMessage("[ActionBar] " + message);
    }
    
    public Object getLocation() {
        // Location information is not directly available from GeyserConnection
        // This would need to be tracked separately or obtained from the backend server
        return null;
    }
    
    public String getWorld() {
        // World information is not directly available from GeyserConnection
        // This would need to be obtained from the backend server
        return "world";
    }
    
    public void closeInventory() {
        // Inventory closing would need to be handled by the backend server
        executeCommand("clear @s");
    }
    
    public void updateInventory() {
        // Inventory updates would need to be handled by the backend server
        // This is a no-op for Geyser connections
    }
    
    /**
     * Gets the underlying Geyser connection
     * @return the GeyserConnection instance
     */
    public GeyserConnection getConnection() {
        return connection;
    }
    
    /**
     * Gets the Bedrock username
     * @return the Bedrock username
     */
    public String getBedrockUsername() {
        return connection.bedrockUsername();
    }
    
    /**
     * Gets the Java username
     * @return the Java username
     */
    public String getJavaUsername() {
        return connection.javaUsername();
    }
    
    /**
     * Gets the Bedrock UUID
     * @return the Bedrock UUID
     */
    public UUID getBedrockUuid() {
        return connection.xuid() != null ? UUID.fromString(connection.xuid()) : getUniqueId();
    }
}