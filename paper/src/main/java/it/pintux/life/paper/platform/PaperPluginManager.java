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

    @Override
    public Object getPlugin(String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName);
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
        RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(serviceClass);
        return provider != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getServiceProvider(Class<T> serviceClass) {
        RegisteredServiceProvider<T> provider = Bukkit.getServicesManager().getRegistration(serviceClass);
        return provider != null ? provider.getProvider() : null;
    }
}
