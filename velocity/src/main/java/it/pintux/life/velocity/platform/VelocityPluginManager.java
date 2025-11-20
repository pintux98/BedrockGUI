package it.pintux.life.velocity.platform;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.platform.PlatformPluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VelocityPluginManager implements PlatformPluginManager {

    private final ProxyServer server;

    public VelocityPluginManager(ProxyServer server) {
        this.server = server;
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return server.getPluginManager().isLoaded(pluginName);
    }

    @Override
    public Object getPlugin(String pluginName) {
        Optional<PluginContainer> plugin = server.getPluginManager().getPlugin(pluginName);
        return plugin.orElse(null);
    }

    @Override
    public boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public Class<?> getClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }

    @Override
    public boolean hasServiceProvider(Class<?> serviceClass) {
        return server.getPluginManager().getPlugin(serviceClass.getSimpleName()).isPresent();
    }

    @Override
    public <T> T getServiceProvider(Class<T> serviceClass) {
        // Velocity doesn't have a service provider system like Bukkit
        // This would need to be implemented based on specific service requirements
        return null;
    }
}