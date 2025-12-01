package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformCommandExecutor;

public class BungeeCommandExecutor implements PlatformCommandExecutor {
    @Override
    public boolean executeAsConsole(String command) { return false; }
    @Override
    public boolean executeAsPlayer(String playerName, String command) { return false; }
    @Override
    public boolean commandExists(String command) { return false; }
}