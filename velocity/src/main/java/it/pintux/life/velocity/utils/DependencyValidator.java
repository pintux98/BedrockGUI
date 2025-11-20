package it.pintux.life.velocity.utils;

import java.util.HashMap;
import java.util.Map;

public class DependencyValidator {

    private static final Map<String, String> pluginVersions = new HashMap<>();

    static {
        // Initialize with available plugins on Velocity
        // Note: Velocity doesn't have Vault or PlaceholderAPI like Paper
        // These would need Velocity-specific alternatives
    }

    public static boolean isPluginCompatible(String pluginName, String minVersion) {
        String currentVersion = pluginVersions.get(pluginName.toLowerCase());
        if (currentVersion == null) {
            return false;
        }
        
        return compareVersions(currentVersion, minVersion) >= 0;
    }

    public static boolean validateDependencies() {
        boolean allValid = true;
        
        // Check for required dependencies
        // Velocity doesn't have Vault or PlaceholderAPI by default
        // These checks would be for Velocity-specific plugins if needed
        
        return allValid;
    }

    private static int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int length = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            
            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }
        
        return 0;
    }

    private static int parseVersionPart(String part) {
        // Remove any non-numeric suffixes (like -SNAPSHOT, -BETA, etc.)
        String numeric = part.replaceAll("[^0-9]", "");
        try {
            return numeric.isEmpty() ? 0 : Integer.parseInt(numeric);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void registerPluginVersion(String pluginName, String version) {
        pluginVersions.put(pluginName.toLowerCase(), version);
    }
}