package it.pintux.life.bungee.platform;

import it.pintux.life.common.platform.PlatformPluginManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Optional;

public class BungeePluginManager implements PlatformPluginManager {
    private final ProxyServer proxy;
    public BungeePluginManager(ProxyServer proxy) { this.proxy = proxy; }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return proxy.getPluginManager().getPlugin(pluginName) != null;
    }
}