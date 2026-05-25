package it.pintux.life.essentialsaddon.config;

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
            List<String> output = new ArrayList<>();
            mergeLines(templateLines, userConfig, output, "", 0);
            Files.write(dataFile.toPath(), output, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warning("Failed to merge config with comments: " + e.getMessage());
        }
    }

    private int mergeLines(List<String> templateLines, YamlConfiguration userConfig,
                           List<String> output, String currentPath, int startIdx) {
        int i = startIdx;
        while (i < templateLines.size()) {
            String line = templateLines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                output.add(line);
                i++;
                continue;
            }

            String key = extractKey(trimmed);
            if (key == null) {
                output.add(line);
                i++;
                continue;
            }

            String fullPath = currentPath.isEmpty() ? key : currentPath + "." + key;
            int indent = getIndent(line);

            if (userConfig.contains(fullPath)) {
                Object value = userConfig.get(fullPath);
                if (userConfig.isConfigurationSection(fullPath)) {
                    output.add(line);
                    i = mergeLines(templateLines, userConfig, output, fullPath, i + 1);
                } else {
                    output.add(spaces(indent) + key + ": " + formatValue(value));
                    i++;
                }
            } else {
                output.add(line);
                i++;
            }
        }
        return i;
    }

    private String extractKey(String line) {
        int colon = line.indexOf(':');
        if (colon <= 0) return null;
        String candidate = line.substring(0, colon).trim();
        if (candidate.isEmpty() || candidate.contains(" ") || candidate.contains("\t")) return null;
        return candidate;
    }

    private int getIndent(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') count++;
            else if (line.charAt(i) == '\t') count += 4;
            else break;
        }
        return count;
    }

    private String spaces(int n) {
        return " ".repeat(n);
    }

    private String formatValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String) {
            String s = (String) value;
            if (s.contains("'") || s.contains("\n") || s.contains("#") ||
                    s.startsWith(":") || s.isEmpty()) {
                return "'" + s.replace("'", "''") + "'";
            }
            return s;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(list.get(i)));
            }
            return "[" + sb + "]";
        }
        return value.toString();
    }
}
