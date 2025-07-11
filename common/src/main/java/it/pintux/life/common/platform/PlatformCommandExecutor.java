package it.pintux.life.common.platform;

/**
 * Platform abstraction for executing commands.
 * This interface allows the common module to execute commands
 * without depending on platform-specific APIs.
 */
public interface PlatformCommandExecutor {
    
    /**
     * Execute a command as the server console.
     * 
     * @param command The command to execute (without leading slash)
     * @return true if the command was executed successfully, false otherwise
     */
    boolean executeAsConsole(String command);
    
    /**
     * Execute a command as a specific player.
     * 
     * @param playerName The name of the player to execute the command as
     * @param command The command to execute (without leading slash)
     * @return true if the command was executed successfully, false otherwise
     */
    boolean executeAsPlayer(String playerName, String command);
    
    /**
     * Check if a command exists on this platform.
     * 
     * @param command The command to check (without leading slash)
     * @return true if the command exists, false otherwise
     */
    boolean commandExists(String command);
}