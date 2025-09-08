package it.pintux.life.common.platform;

/**
 * Platform abstraction for plugin management operations.
 * This interface allows the common module to interact with plugins
 * without depending on platform-specific APIs.
 */
public interface PlatformPluginManager {
    
    /**
     * Check if a plugin is enabled on this platform.
     * 
     * @param pluginName The name of the plugin to check
     * @return true if the plugin is enabled, false otherwise
     */
    boolean isPluginEnabled(String pluginName);
    
    /**
     * Get a plugin instance by name.
     * 
     * @param pluginName The name of the plugin
     * @return The plugin instance, or null if not found
     */
    Object getPlugin(String pluginName);
    
    /**
     * Check if a specific class exists (for plugin API detection).
     * 
     * @param className The fully qualified class name
     * @return true if the class exists, false otherwise
     */
    boolean hasClass(String className);
    
    /**
     * Get a class by name using reflection.
     * 
     * @param className The fully qualified class name
     * @return The class instance, or null if not found
     * @throws ClassNotFoundException if the class cannot be found
     */
    Class<?> getClass(String className) throws ClassNotFoundException;
    
    /**
     * Check if a service provider is available.
     * 
     * @param serviceClass The service class to check
     * @return true if a service provider is available, false otherwise
     */
    boolean hasServiceProvider(Class<?> serviceClass);
    
    /**
     * Get a service provider instance.
     * 
     * @param serviceClass The service class
     * @param <T> The service type
     * @return The service provider instance, or null if not available
     */
    <T> T getServiceProvider(Class<T> serviceClass);
}