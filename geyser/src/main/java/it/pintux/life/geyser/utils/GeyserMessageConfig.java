package it.pintux.life.geyser.utils;

import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.FormPlayer;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

public class GeyserMessageConfig implements MessageConfig {
    
    private final File configFile;
    private Map<String, Object> config;
    
    public GeyserMessageConfig(File dataFolder, String fileName) {
        this.configFile = new File(dataFolder, fileName);
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            if (!configFile.exists()) {
                createDefaultMessages();
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
            System.err.println("Failed to load messages config: " + e.getMessage());
        }
    }
    
    private void createDefaultMessages() {
        try {
            configFile.getParentFile().mkdirs();
            
            // Copy default messages from common module resources
            InputStream defaultMessagesStream = getClass().getClassLoader().getResourceAsStream("messages.yml");
            if (defaultMessagesStream != null) {
                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = defaultMessagesStream.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }
                defaultMessagesStream.close();
            } else {
                // Fallback: create minimal messages if resource not found
                Map<String, Object> defaultMessages = new HashMap<>();
                defaultMessages.put("prefix", "&x&E&3&C&1&F&BB&x&E&2&C&8&E&8e&x&E&0&C&F&D&5d&x&D&F&D&6&C&2r&x&D&E&D&D&A&Fo&x&D&C&E&3&9&Dc&x&D&B&E&A&8&Ak&x&D&A&F&1&7&7G&x&D&8&F&8&6&4U&x&D&7&F&F&5&1I");
                defaultMessages.put("noPex", "&4&lYou don't have permission to perform this command");
                
                Map<String, Object> menuMessages = new HashMap<>();
                menuMessages.put("noPex", "&4&lYou don't have permission to open this menu");
                menuMessages.put("noJava", "&4You are not a bedrock player. Please open this menu only from bedrock client");
                menuMessages.put("arguments", "&4This command requires at least {args} arguments.");
                menuMessages.put("notFound", "&cNo menu found with this name");
                defaultMessages.put("menu", menuMessages);
                
                Yaml yaml = new Yaml();
                try (FileWriter writer = new FileWriter(configFile)) {
                    yaml.dump(defaultMessages, writer);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Failed to create default messages: " + e.getMessage());
        }
    }
    
    @Override
    public String getString(String path) {
        Object value = config.get(path);
        return value != null ? value.toString() : path;
    }
    
    @Override
    public String setPlaceholders(FormPlayer player, String message) {
        if (player == null || message == null) {
            return message;
        }
        
        String result = message;
        result = result.replace("{player}", player.getName());
        result = result.replace("{uuid}", player.getUniqueId().toString());
        
        return result;
    }
    
    @Override
    public String applyColor(String message) {
        if (message == null) {
            return null;
        }
        
        // Convert & color codes to § color codes
        return message.replace('&', '§');
    }
    
    // Additional utility methods
    public String getMessage(String key) {
        return getString(key);
    }
    
    public String getMessage(String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    public List<String> getMessageList(String key) {
        Object value = config.get(key);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(item.toString());
            }
            return result;
        }
        return Arrays.asList(getMessage(key));
    }
    
    public boolean contains(String key) {
        return config.containsKey(key);
    }
    
    public Set<String> getKeys() {
        Set<String> keys = new HashSet<>();
        for (Object key : config.keySet()) {
            keys.add(key.toString());
        }
        return keys;
    }
    
    public void reload() {
        loadConfig();
    }
}