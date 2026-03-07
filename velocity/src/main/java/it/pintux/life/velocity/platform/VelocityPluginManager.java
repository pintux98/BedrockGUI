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
}