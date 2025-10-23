package it.pintux.life.common.platform;


public interface PlatformCommandExecutor {
    
    
    boolean executeAsConsole(String command);
    
    
    boolean executeAsPlayer(String playerName, String command);
    
    
    boolean commandExists(String command);
}
