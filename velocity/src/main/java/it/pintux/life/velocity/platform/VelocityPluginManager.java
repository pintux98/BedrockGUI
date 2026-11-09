package it.pintux.life.velocity.platform;

import com.velocitypowered.api.proxy.ProxyServer;
import it.pintux.life.common.platform.PlatformPluginManager;


public class VelocityPluginManager implements PlatformPluginManager {

    private final ProxyServer server;

    public VelocityPluginManager(ProxyServer server) {
        this.server = server;
    }

    @Override
    public boolean isPluginEnabled(String pluginName) {
        return server.getPluginManager().getPlugins().stream()
                .map(container -> container.getDescription())
                .anyMatch(d -> d.getId().equalsIgnoreCase(pluginName)
                        || d.getName().map(name -> name.equalsIgnoreCase(pluginName)).orElse(false));
    }
}