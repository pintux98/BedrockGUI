package it.pintux.life.bungee.utils;

import it.pintux.life.common.utils.FormConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class BungeeConfig implements FormConfig {
    private final File dataFolder;
    private Map<String, Object> config;
    private final Yaml yaml;

    public BungeeConfig(File dataFolder) {
        this.dataFolder = dataFolder;
        this.yaml = new Yaml();
        loadConfig();
    }

    private BungeeConfig(File dataFolder, Map<String, Object> loaded) {
        this.dataFolder = dataFolder;
        this.yaml = new Yaml();
        this.config = loaded != null ? loaded : new HashMap<>();
    }

    private void loadConfig() {
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) { createDefaultConfig(configFile); }
        try (InputStream inputStream = new FileInputStream(configFile)) {
            config = yaml.load(inputStream);
            if (config == null) { config = new HashMap<>(); }
        } catch (IOException e) { throw new RuntimeException("Failed to load config.yml", e); }
    }

    private void createDefaultConfig(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            InputStream defaultConfig = getClass().getResourceAsStream("/config.yml");
            if (defaultConfig != null) { Files.copy(defaultConfig, configFile.toPath()); }
            else {
                String defaultContent = """
                    debug: false
                    forms:
                      main_hub:
                        type: "SIMPLE"
                        title: "Main Hub"
                        content: "Welcome!"
                        buttons: {}
                    """;
                Files.write(configFile.toPath(), defaultContent.getBytes());
            }
        } catch (IOException e) { throw new RuntimeException("Failed to create default config", e); }
    }

    @Override
    public String getString(String path, String defaultValue) {
        Object value = getValue(path);
        return value != null ? value.toString() : defaultValue;
    }

    @Override
    public String getString(String path) { return getString(path, ""); }

    @Override
    public List<String> getStringList(String path) {
        Object value = getValue(path);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> res = new ArrayList<>();
            for (Object item : list) res.add(item.toString());
            return res;
        }
        return new ArrayList<>();
    }

    @Override
    public Set<String> getKeys(String path) {
        Object value = getValue(path);
        if (value instanceof Map) { return ((Map<String, Object>) value).keySet(); }
        return new HashSet<>();
    }

    @Override
    public Map<String, Object> getValues(String path) {
        Object value = getValue(path);
        if (value instanceof Map) { return new HashMap<>((Map<String, Object>) value); }
        return new HashMap<>();
    }

    @Override
    public FormConfig loadFormFile(String relativePath) {
        File formsDir = new File(dataFolder, "forms");
        File file = new File(formsDir, relativePath);
        try (InputStream in = new FileInputStream(file)) {
            Object loaded = yaml.load(in);
            Map<String, Object> root = loaded instanceof Map ? (Map<String, Object>) loaded : new HashMap<>();
            return new BungeeConfig(dataFolder, root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load form file: " + relativePath, e);
        }
    }

    private Object getValue(String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = config;
        for (int i = 0; i < parts.length - 1; i++) {
            Object value = current.get(parts[i]);
            if (value instanceof Map) { current = (Map<String, Object>) value; }
            else { return null; }
        }
        return current.get(parts[parts.length - 1]);
    }

    public void reload() { loadConfig(); }
}
