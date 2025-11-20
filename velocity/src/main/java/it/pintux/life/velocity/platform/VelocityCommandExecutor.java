package it.pintux.life.velocity.platform;

import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.velocity.utils.VelocityPlayer;

import java.util.List;

public class VelocityCommandExecutor implements PlatformCommandExecutor {

    @Override
    public boolean executeAsConsole(String command) {
        // Velocity doesn't have a direct console command execution like Paper
        // This would need to be implemented via plugin messages to backend servers
        return false;
    }

    @Override
    public boolean executeAsPlayer(String playerName, String command) {
        // This would need to be implemented by finding the player and sending command to their server
        return false;
    }

    @Override
    public boolean commandExists(String command) {
        // Velocity doesn't have a direct way to check if command exists
        // This would need to be implemented via plugin messages to backend servers
        return false;
    }
}