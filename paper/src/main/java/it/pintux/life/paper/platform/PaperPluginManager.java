package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformPluginManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;


public class PaperPluginManager implements PlatformPluginManager {

    @Override
    public boolean isPluginEnabled(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }
}
