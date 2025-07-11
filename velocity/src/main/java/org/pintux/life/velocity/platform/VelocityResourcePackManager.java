package org.pintux.life.velocity.platform;

import it.pintux.life.common.platform.PlatformResourcePackManager;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocity implementation of PlatformResourcePackManager.
 * Note: Velocity doesn't have native resource pack support, so this is a basic implementation.
 */
public class VelocityResourcePackManager implements PlatformResourcePackManager {
    
    private final Set<String> loadedPacks = ConcurrentHashMap.newKeySet();
    
    @Override
    public boolean isResourcePackSupported() {
        // Velocity doesn't have direct resource pack capabilities
        // Resource packs would need to be handled by the backend servers
        return false;
    }
    
    @Override
    public boolean sendResourcePack(UUID playerUuid, String packName) {
        // Velocity doesn't have direct resource pack sending capabilities
        // This would need to be implemented through plugin messaging to backend servers
        return false;
    }
    
    @Override
    public boolean hasResourcePack(UUID playerUuid, String packName) {
        // Velocity doesn't have direct resource pack capabilities
        return false;
    }
    
    @Override
    public Set<String> getLoadedPacks() {
        return Set.copyOf(loadedPacks);
    }
    
    @Override
    public String getMenuResourcePack(String menuName) {
        // No menu-specific resource packs in Velocity
        return null;
    }
    
    @Override
    public boolean sendMenuResourcePack(UUID playerUuid, String menuName) {
        // Velocity doesn't have direct resource pack sending capabilities
        return false;
    }
    
    @Override
    public boolean isEnabled() {
        // Resource pack management is not enabled for Velocity
        return false;
    }
}