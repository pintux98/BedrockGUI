package it.pintux.life.bedwarsaddon.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public final class ConfigMigrator {

    private static final String VERSION_KEY = "config-version";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final String fileName;

    public ConfigMigrator(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.fileName = fileName;
    }

    public YamlConfiguration migrate() {
        File dataFile = new File(plugin.getDataFolder(), fileName);

        if (!dataFile.exists()) {
            plugin.saveResource(fileName, false);
            return YamlConfiguration.loadConfiguration(dataFile);
        }

        InputStream bundledStream = plugin.getResource(fileName);
        if (bundledStream == null) {
            logger.warning("Bundled " + fileName + " not found in resources. Skipping migration.");
            return YamlConfiguration.loadConfiguration(dataFile);
        }

        String bundledText = readStream(bundledStream);
        YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                new StringReader(bundledText));
        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(dataFile);

        int bundledVersion = bundled.getInt(VERSION_KEY, 1);
        int userVersion = userConfig.getInt(VERSION_KEY, 0);

        if (userVersion == bundledVersion) {
            return userConfig;
        }

        logger.info("Migrating " + fileName + " from version " + userVersion + " → " + bundledVersion);

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        mergeKeys(bundled, userConfig, "", added, removed);
        repairBooleanProviders(userConfig, userVersion);

        userConfig.set(VERSION_KEY, bundledVersion);

        mergeWithComments(bundledText, userConfig, dataFile);

        if (!added.isEmpty()) {
            logger.info("  Added " + added.size() + " new config key(s).");
        }
        if (!removed.isEmpty()) {
            logger.info("  Removed " + removed.size() + " obsolete config key(s).");
        }

        return YamlConfiguration.loadConfiguration(dataFile);
    }

    private void repairBooleanProviders(YamlConfiguration userConfig, int userVersion) {
        if (userVersion != 4) return;
        for (String key : List.of("warps", "kits", "homes", "tpa")) {
            String path = "providers." + key;
            if (userConfig.get(path) instanceof Boolean) {
                userConfig.set(path, "auto");
                logger.info("  Reset " + path + " to auto (config-version 4 wrote a module toggle there).");
            }
        }
    }

    private String readStream(InputStream in) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            logger.warning("Failed to read bundled " + fileName + ": " + e.getMessage());
            return "";
        }
    }

    private void mergeKeys(ConfigurationSection bundled, ConfigurationSection userConfig,
                           String path, List<String> added, List<String> removed) {
        for (String key : bundled.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (bundled.isConfigurationSection(key)) {
                if (!userConfig.isConfigurationSection(key)) {
                    userConfig.createSection(key);
                }
                mergeKeys(
                        bundled.getConfigurationSection(key),
                        userConfig.getConfigurationSection(key),
                        fullPath, added, removed
                );
            } else if (!userConfig.contains(key)) {
                userConfig.set(key, bundled.get(key));
                added.add(fullPath);
            }
        }
        for (String key : userConfig.getKeys(false)) {
            if (!bundled.contains(key)) {
                userConfig.set(key, null);
                String fullPath = path.isEmpty() ? key : path + "." + key;
                removed.add(fullPath);
            }
        }
    }

    private void mergeWithComments(String bundledText, YamlConfiguration userConfig, File dataFile) {
        try {
            List<String> templateLines = Arrays.asList(bundledText.split("\n"));
            List<String> output = ConfigLineMerger.merge(templateLines, new YamlValueSource(userConfig));
            Files.write(dataFile.toPath(), output, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warning("Failed to merge config with comments: " + e.getMessage());
        }
    }

    private static final class YamlValueSource implements ConfigLineMerger.ValueSource {
        private final YamlConfiguration configuration;

        private YamlValueSource(YamlConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        public boolean contains(String path) {
            return configuration.contains(path);
        }

        @Override
        public boolean isSection(String path) {
            return configuration.isConfigurationSection(path);
        }

        @Override
        public Object get(String path) {
            return configuration.get(path);
        }
    }
}
