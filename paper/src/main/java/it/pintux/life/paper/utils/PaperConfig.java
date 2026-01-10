package it.pintux.life.paper.utils;

import it.pintux.life.common.utils.FormConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PaperConfig implements FormConfig {
    private final FileConfiguration config;
    private final java.io.File dataFolder;

    public PaperConfig(java.io.File dataFolder, FileConfiguration config) {
        this.dataFolder = dataFolder;
        this.config = config;
    }

    @Override
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    @Override
    public String getString(String path) {
        return config.getString(path);
    }

    @Override
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    @Override
    public Set<String> getKeys(String path) {
        var section = config.getConfigurationSection(path);
        if (section == null) {
            return new java.util.HashSet<>();
        }
        return section.getKeys(false);
    }

    @Override
    public Map<String, Object> getValues(String path) {
        var section = config.getConfigurationSection(path);
        if (section == null) {
            return new java.util.HashMap<>();
        }
        return section.getValues(false);
    }

    @Override
    public FormConfig loadFormFile(String relativePath) {
        java.io.File formsDir = new java.io.File(dataFolder, "forms");
        java.io.File file = new java.io.File(formsDir, relativePath);
        org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        return new PaperConfig(dataFolder, yaml);
    }
}
