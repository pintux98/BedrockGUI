package it.pintux.life.common.platform;


public interface PlatformPluginManager {
    
    
    boolean isPluginEnabled(String pluginName);
    
    
    Object getPlugin(String pluginName);
    
    
    boolean hasClass(String className);
    
    
    Class<?> getClass(String className) throws ClassNotFoundException;
    
    
    boolean hasServiceProvider(Class<?> serviceClass);
    
    
    <T> T getServiceProvider(Class<T> serviceClass);
}
