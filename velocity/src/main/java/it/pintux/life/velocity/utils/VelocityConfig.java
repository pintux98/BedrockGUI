package it.pintux.life.velocity.utils;

import it.pintux.life.common.utils.FormConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class VelocityConfig implements FormConfig {

    private final File dataFolder;
    private Map<String, Object> config;
    private final Yaml yaml;

    public VelocityConfig(File dataFolder) {
        this.dataFolder = dataFolder;
        this.yaml = new Yaml();
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(dataFolder, "config.yml");
        
        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }

        try (InputStream inputStream = new FileInputStream(configFile)) {
            config = yaml.load(inputStream);
            if (config == null) {
                config = new HashMap<>();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.yml", e);
        }
    }

    private void createDefaultConfig(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            
            // Copy default config from common module
            InputStream defaultConfig = getClass().getResourceAsStream("/config.yml");
            if (defaultConfig != null) {
                Files.copy(defaultConfig, configFile.toPath());
            } else {
                // Create minimal default config
                String defaultContent = """
                    # BedrockGUI Configuration
                    debug: false
                    
                    # Menu settings
                    menus:
                      example:
                        title: "Example Menu"
                        type: "simple"
                        content: "Welcome to the example menu!"
                    """;
                Files.write(configFile.toPath(), defaultContent.getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default config", e);
        }
    }

    @Override
    public String getString(String path, String defaultValue) {
        Object value = getValue(path);
        return value != null ? value.toString() : defaultValue;
    }

    @Override
    public String getString(String path) {
        return getString(path, "");
    }

    @Override
    public List<String> getStringList(String path) {
        Object value = getValue(path);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> stringList = new ArrayList<>();
            for (Object item : list) {
                stringList.add(item.toString());
            }
            return stringList;
        }
        return new ArrayList<>();
    }

    @Override
    public Set<String> getKeys(String path) {
        Object value = getValue(path);
        if (value instanceof Map) {
            return ((Map<String, Object>) value).keySet();
        }
        return new HashSet<>();
    }

    @Override
    public Map<String, Object> getValues(String path) {
        Object value = getValue(path);
        if (value instanceof Map) {
            return new HashMap<>((Map<String, Object>) value);
        }
        return new HashMap<>();
    }

    private Object getValue(String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = config;
        
        for (int i = 0; i < parts.length - 1; i++) {
            Object value = current.get(parts[i]);
            if (value instanceof Map) {
                current = (Map<String, Object>) value;
            } else {
                return null;
            }
        }
        
        return current.get(parts[parts.length - 1]);
    }

    public void reload() {
        loadConfig();
    }
}