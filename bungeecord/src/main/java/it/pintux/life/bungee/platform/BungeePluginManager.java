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

    @Override
    public Object getPlugin(String pluginName) {
        return proxy.getPluginManager().getPlugin(pluginName);
    }

    @Override
    public boolean hasClass(String className) {
        try { Class.forName(className); return true; } catch (ClassNotFoundException e) { return false; }
    }

    @Override
    public Class<?> getClass(String className) throws ClassNotFoundException { return Class.forName(className); }

    @Override
    public boolean hasServiceProvider(Class<?> serviceClass) { return false; }

    @Override
    public <T> T getServiceProvider(Class<T> serviceClass) { return null; }
}