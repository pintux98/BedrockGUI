package it.pintux.life.geyser.utils;

import it.pintux.life.common.utils.FormConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

public class GeyserConfig implements FormConfig {
    
    private final File configFile;
    private Map<String, Object> config;
    
    public GeyserConfig(File configFile) {
        this.configFile = configFile;
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            if (!configFile.exists()) {
                // Create default config
                createDefaultConfig();
            }
            
            Yaml yaml = new Yaml();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                config = yaml.load(fis);
                if (config == null) {
                    config = new HashMap<>();
                }
            }
        } catch (IOException e) {
            config = new HashMap<>();
            System.err.println("Failed to load config: " + e.getMessage());
        }
    }
    
    private void createDefaultConfig() {
        try {
            configFile.getParentFile().mkdirs();
            
            // Copy default config from common module resources
            InputStream defaultConfigStream = getClass().getClassLoader().getResourceAsStream("config.yml");
            if (defaultConfigStream != null) {
                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = defaultConfigStream.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }
                defaultConfigStream.close();
            } else {
                // Fallback: create a minimal config if resource not found
                Map<String, Object> defaultConfig = new HashMap<>();
                defaultConfig.put("menu", new HashMap<>());
                
                Yaml yaml = new Yaml();
                try (FileWriter writer = new FileWriter(configFile)) {
                    yaml.dump(defaultConfig, writer);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Failed to create default config: " + e.getMessage());
        }
    }
    
    public Object get(String path) {
        String[] keys = path.split("\\.");
        Object current = config;
        
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    @Override
    public String getString(String path) {
        Object value = get(path);
        return value != null ? value.toString() : null;
    }
    
    @Override
    public String getString(String path, String defaultValue) {
        String value = getString(path);
        return value != null ? value : defaultValue;
    }
    
    @Override
    public List<String> getStringList(String path) {
        Object value = get(path);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item.toString());
            }
            return result;
        }
        return new ArrayList<>();
    }
    
    @Override
    public Set<String> getKeys(String path) {
        Object value = get(path);
        if (value instanceof Map) {
            Set<String> keys = new HashSet<>();
            for (Object key : ((Map<?, ?>) value).keySet()) {
                keys.add(key.toString());
            }
            return keys;
        }
        return new HashSet<>();
    }
    
    @Override
    public Map<String, Object> getValues(String path) {
        Object value = get(path);
        if (value instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
            return result;
        }
        return new HashMap<>();
    }
    
    // Additional utility methods
    public int getInt(String path) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    public int getInt(String path, int defaultValue) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String path) {
        Object value = get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }
    
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public boolean contains(String path) {
        return get(path) != null;
    }
    
    public void reload() {
        loadConfig();
    }
}