package it.pintux.life.paper.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.logging.Logger;

public class DependencyValidator {
    
    private static final Logger logger = Logger.getLogger(DependencyValidator.class.getName());
    
    // Minimum required versions for soft dependencies
    private static final String MIN_VAULT_VERSION = "1.7.0";
    private static final String MIN_PLACEHOLDERAPI_VERSION = "2.10.0";
    
    /**
     * Validates all soft dependencies and their versions
     * @return true if all dependencies are compatible, false otherwise
     */
    public static boolean validateDependencies() {
        boolean allValid = true;
        
        // Check Vault
        Plugin vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
        if (vaultPlugin != null) {
            if (!isVersionCompatible(vaultPlugin, MIN_VAULT_VERSION)) {
                logger.warning("Vault version " + vaultPlugin.getDescription().getVersion() + 
                             " is below minimum required version " + MIN_VAULT_VERSION + 
                             ". Economy features may not work properly.");
                allValid = false;
            } else {
                logger.info("Vault version " + vaultPlugin.getDescription().getVersion() + " is compatible");
            }
        }
        
        // Check PlaceholderAPI
        Plugin placeholderPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderPlugin != null) {
            if (!isVersionCompatible(placeholderPlugin, MIN_PLACEHOLDERAPI_VERSION)) {
                logger.warning("PlaceholderAPI version " + placeholderPlugin.getDescription().getVersion() + 
                             " is below minimum required version " + MIN_PLACEHOLDERAPI_VERSION + 
                             ". Placeholder features may not work properly.");
                allValid = false;
            } else {
                logger.info("PlaceholderAPI version " + placeholderPlugin.getDescription().getVersion() + " is compatible");
            }
        }
        
        return allValid;
    }
    
    /**
     * Checks if a plugin version is compatible with the minimum required version
     * @param plugin The plugin to check
     * @param minVersion The minimum required version
     * @return true if compatible, false otherwise
     */
    private static boolean isVersionCompatible(Plugin plugin, String minVersion) {
        try {
            PluginDescriptionFile description = plugin.getDescription();
            String currentVersion = description.getVersion();
            
            return compareVersions(currentVersion, minVersion) >= 0;
        } catch (Exception e) {
            logger.warning("Failed to check version compatibility for " + plugin.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Compares two version strings
     * @param version1 First version
     * @param version2 Second version
     * @return negative if version1 < version2, 0 if equal, positive if version1 > version2
     */
    private static int compareVersions(String version1, String version2) {
        // Remove any non-numeric suffixes (like -SNAPSHOT, -beta, etc.)
        String cleanVersion1 = version1.replaceAll("[^0-9.]", "");
        String cleanVersion2 = version2.replaceAll("[^0-9.]", "");
        
        String[] parts1 = cleanVersion1.split("\\.");
        String[] parts2 = cleanVersion2.split("\\.");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int part1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int part2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            
            if (part1 != part2) {
                return Integer.compare(part1, part2);
            }
        }
        
        return 0;
    }
    
    /**
     * Safely parses a version part to integer
     * @param part The version part string
     * @return The parsed integer, or 0 if parsing fails
     */
    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Checks if a specific plugin is present and compatible
     * @param pluginName The name of the plugin
     * @param minVersion The minimum required version
     * @return true if present and compatible, false otherwise
     */
    public static boolean isPluginCompatible(String pluginName, String minVersion) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && isVersionCompatible(plugin, minVersion);
    }
}