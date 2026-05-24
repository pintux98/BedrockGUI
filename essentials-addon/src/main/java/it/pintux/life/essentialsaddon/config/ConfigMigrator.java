package it.pintux.life.essentialsaddon.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Migrates an existing datafolder config to match the bundled config structure.
 *
 * Strategy:
 * 1. Read bundled config (resource) to get the current schema + version.
 * 2. Read datafolder config (user file).
 * 3. If versions match → no migration needed.
 * 4. If versions differ → copy every key from bundled into user config,
 *    preserving user values for keys that already exist.
 *    Keys present in user but removed from bundled are deleted.
 * 5. Write updated version to user config and save.
 */
public final class ConfigMigrator {

    private static final String VERSION_KEY = "config-version";
    private static final String VERSION_COMMENT = "# DO NOT CHANGE THIS VALUE — managed by the plugin.";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final String fileName;

    public ConfigMigrator(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.fileName = fileName;
    }

    /**
     * Runs migration if needed. Returns the YamlConfiguration ready for use.
     */
    public YamlConfiguration migrate() {
        File dataFile = new File(plugin.getDataFolder(), fileName);

        // Ensure the datafolder file exists (first-run: just copy bundled)
        if (!dataFile.exists()) {
            plugin.saveResource(fileName, false);
            return YamlConfiguration.loadConfiguration(dataFile);
        }

        // Load bundled config from resources
        YamlConfiguration bundled = loadBundledConfig();
        if (bundled == null) {
            logger.warning("Bundled " + fileName + " not found in resources. Skipping migration.");
            return YamlConfiguration.loadConfiguration(dataFile);
        }

        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(dataFile);

        int bundledVersion = bundled.getInt(VERSION_KEY, 1);
        int userVersion = userConfig.getInt(VERSION_KEY, 0);

        if (userVersion == bundledVersion) {
            // Up to date
            return userConfig;
        }

        logger.info("Migrating " + fileName + " from version " + userVersion + " → " + bundledVersion);

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();

        // Add missing keys from bundled → user
        mergeSection(bundled, userConfig, "", added);

        // Remove keys from user that no longer exist in bundled
        removeObsoleteKeys(bundled, userConfig, "", removed);

        // Update version
        userConfig.set(VERSION_KEY, bundledVersion);

        // Save and rewrite with comments
        saveWithComments(dataFile, userConfig, bundled);

        if (!added.isEmpty()) {
            logger.info("  Added " + added.size() + " new config key(s).");
        }
        if (!removed.isEmpty()) {
            logger.info("  Removed " + removed.size() + " obsolete config key(s).");
        }

        return YamlConfiguration.loadConfiguration(dataFile);
    }

    private YamlConfiguration loadBundledConfig() {
        try (InputStream in = plugin.getResource(fileName)) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.warning("Failed to read bundled " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Recursively copy keys from source into target if they don't already exist.
     */
    private void mergeSection(ConfigurationSection source, ConfigurationSection target, String path, List<String> added) {
        for (String key : source.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (source.isConfigurationSection(key)) {
                ConfigurationSection childSource = source.getConfigurationSection(key);
                ConfigurationSection childTarget = target.isConfigurationSection(key)
                        ? target.getConfigurationSection(key)
                        : target.createSection(key);
                mergeSection(childSource, childTarget, fullPath, added);
            } else if (!target.contains(key)) {
                // New key — copy default value
                target.set(key, source.get(key));
                added.add(fullPath);
            }
            // If target already has the key → preserve user value, do nothing
        }
    }

    /**
     * Recursively remove keys from target that no longer exist in source.
     */
    private void removeObsoleteKeys(ConfigurationSection source, ConfigurationSection target, String path, List<String> removed) {
        List<String> obsoleteKeys = new ArrayList<>();
        for (String key : target.getKeys(false)) {
            if (!source.contains(key)) {
                obsoleteKeys.add(key);
            } else if (source.isConfigurationSection(key) && target.isConfigurationSection(key)) {
                // Recurse into sub-sections
                removeObsoleteKeys(
                        source.getConfigurationSection(key),
                        target.getConfigurationSection(key),
                        path.isEmpty() ? key : path + "." + key,
                        removed
                );
            }
        }
        for (String key : obsoleteKeys) {
            target.set(key, null);
            String fullPath = path.isEmpty() ? key : path + "." + key;
            removed.add(fullPath);
        }
    }

    /**
     * Saves the config while preserving the "DO NOT CHANGE" comment on config-version.
     * Bukkit's YamlConfiguration doesn't support comments natively, so we
     * post-process the file to inject the comment.
     */
    private void saveWithComments(File dataFile, YamlConfiguration userConfig, YamlConfiguration bundled) {
        try {
            // Save normally first
            userConfig.save(dataFile);

            // Read lines and inject comment before config-version
            List<String> lines = Files.readAllLines(dataFile.toPath(), StandardCharsets.UTF_8);
            List<String> output = new ArrayList<>();
            boolean versionCommented = false;

            for (String line : lines) {
                if (!versionCommented && line.startsWith("config-version:")) {
                    output.add(VERSION_COMMENT);
                    versionCommented = true;
                }
                output.add(line);
            }

            Files.write(dataFile.toPath(), output, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warning("Failed to save migrated " + fileName + ": " + e.getMessage());
        }
    }
}
