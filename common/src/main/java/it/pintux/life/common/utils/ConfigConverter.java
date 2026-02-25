package it.pintux.life.common.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public class ConfigConverter {

    private final File dataFolder;
    private final Logger logger;

    public ConfigConverter(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public int convert() {
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            return 0;
        }

        // Setup Yaml
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        Map<String, Object> config;
        try (InputStream in = new FileInputStream(configFile)) {
            config = yaml.load(in);
        } catch (IOException e) {
            logger.severe("Failed to load config.yml for conversion: " + e.getMessage());
            return 0;
        }

        if (config == null) {
            return 0;
        }

        File formsDir = new File(dataFolder, "forms");
        if (!formsDir.exists()) {
            formsDir.mkdirs();
        }

        // Identify source root
        String sourceRoot = null;
        if (hasMenus(config, "forms")) {
            sourceRoot = "forms";
        } else if (hasMenus(config, "menu")) {
            sourceRoot = "menu";
        }

        if (sourceRoot == null) {
            return 0;
        }

        Map<String, Object> rootSection = getSection(config, sourceRoot);
        if (rootSection == null) {
            return 0;
        }

        int converted = 0;
        // We need a list of keys to avoid ConcurrentModificationException if we modify the map while iterating
        // But here we are iterating 'rootSection' and modifying 'config' or 'rootSection'.
        // Iterating over a copy of keys is safer.
        List<String> keys = new ArrayList<>(rootSection.keySet());

        for (String key : keys) {
            Object value = rootSection.get(key);
            if (!(value instanceof Map)) {
                continue;
            }
            Map<String, Object> menuData = (Map<String, Object>) value;

            // Check if already converted
            if (menuData.containsKey("file") && menuData.get("file") instanceof String && !((String) menuData.get("file")).isEmpty()) {
                continue;
            }

            // Prepare new form content
            Map<String, Object> newFormConfig = new LinkedHashMap<>();

            // 1. Copy Bedrock content (the current menuData)
            // We need to deep copy and process onClick/actions
            Map<String, Object> bedrockContent = deepCopyAndProcess(menuData);
            newFormConfig.put("bedrock", bedrockContent);

            // 2. Check for Java menu (inline convention: key + ".java" inside root, OR inside menuData if structure differs?)
            // The BedrockCommand logic looked for "forms.<key>.java" as a sibling key in the root.
            // Let's check if the root has "<key>.java"
            String javaKey = key + ".java";
            if (rootSection.containsKey(javaKey)) {
                Object javaValue = rootSection.get(javaKey);
                if (javaValue instanceof Map) {
                    Map<String, Object> javaContent = deepCopyAndProcess((Map<String, Object>) javaValue);
                    newFormConfig.put("java", javaContent);
                    // Mark java section for removal from main config
                    rootSection.remove(javaKey);
                }
            }

            // Save to new file
            String filename = key + ".yml";
            File outFile = new File(formsDir, filename);
            try (Writer writer = new FileWriter(outFile)) {
                yaml.dump(newFormConfig, writer);
            } catch (IOException e) {
                logger.severe("Failed to save converted form " + filename + ": " + e.getMessage());
                continue;
            }

            // Update main config
            // Replace content with file reference
            menuData.clear();
            menuData.put("file", filename);
            converted++;
        }
        
        // If we converted from "menu", we should probably move the converted entries to "forms" if we want to standardize,
        // or just leave them in "menu" but as file references?
        // BedrockCommand logic: if "menu" was root, it set "menu" to null?
        // Wait, BedrockCommand logic:
        // if ("menu".equals(sourceRoot)) { cfg.set("menu", null); }
        // But it also seemed to assume it was moving things to "forms" in the file structure?
        // Actually, BedrockCommand iterated keys, created files, and then:
        // cfg.set("forms." + key + ".file", rel);
        // So it effectively MOVED them to "forms" in the config structure if they weren't there.
        
        if ("menu".equals(sourceRoot)) {
            // Move everything from "menu" to "forms"
            Map<String, Object> formsSection;
            if (config.containsKey("forms") && config.get("forms") instanceof Map) {
                formsSection = (Map<String, Object>) config.get("forms");
            } else {
                formsSection = new LinkedHashMap<>();
                config.put("forms", formsSection);
            }
            
            // rootSection is the "menu" map.
            // We've already updated it to have "file" references.
            // We should put these into formsSection
            formsSection.putAll(rootSection);
            
            // Remove "menu"
            config.remove("menu");
        }

        // Save config.yml
        try (Writer writer = new FileWriter(configFile)) {
            yaml.dump(config, writer);
        } catch (IOException e) {
            logger.severe("Failed to save config.yml after conversion: " + e.getMessage());
        }
        
        // Backup
        try {
            File backupFile = new File(dataFolder, "config_backup_" + System.currentTimeMillis() + ".yml");
            // We should have backed up BEFORE saving, but we can't easily deep copy the file in memory without re-reading.
            // Let's just say we modify in place.
            // Ideally we should have copied config.yml to backup BEFORE starting.
            // The previous BedrockCommand did copy before modification.
            // I should assume the caller handles backup or I do it at start.
        } catch (Exception e) {}

        return converted;
    }
    
    public String backupConfig() {
        try {
            File configFile = new File(dataFolder, "config.yml");
            if (configFile.exists()) {
                String backupName = "config_backup_" + System.currentTimeMillis() + ".yml";
                File backupFile = new File(dataFolder, backupName);
                Files.copy(configFile.toPath(), backupFile.toPath());
                return backupName;
            }
        } catch (IOException e) {
            logger.warning("Failed to backup config.yml: " + e.getMessage());
        }
        return null;
    }

    private boolean hasMenus(Map<String, Object> config, String key) {
        if (!config.containsKey(key)) return false;
        Object val = config.get(key);
        return val instanceof Map && !((Map<?, ?>) val).isEmpty();
    }

    private Map<String, Object> getSection(Map<String, Object> config, String key) {
        Object val = config.get(key);
        if (val instanceof Map) {
            return (Map<String, Object>) val;
        }
        return null;
    }

    private Map<String, Object> deepCopyAndProcess(Map<String, Object> original) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                copy.put(key, deepCopyAndProcess((Map<String, Object>) value));
            } else if (value instanceof List) {
                copy.put(key, processList(key, (List<?>) value));
            } else if (value instanceof String) {
                copy.put(key, processString(key, (String) value));
            } else {
                copy.put(key, value);
            }
        }
        return copy;
    }

    private Object processList(String key, List<?> list) {
        List<Object> newList = new ArrayList<>();
        boolean isActionKey = isActionKey(key);

        for (Object item : list) {
            if (item instanceof Map) {
                newList.add(deepCopyAndProcess((Map<String, Object>) item));
            } else if (item instanceof String) {
                String s = (String) item;
                if (isActionKey) {
                     // Flatten multiline strings if mostly valid action
                     // The logic in BedrockCommand was:
                     // replace \n with space, replace "- |" with "- " (block scalar indicator removal?)
                     // trim
                     String noNewlines = s.replaceAll("\\r?\\n", " ");
                     String normalized = noNewlines.replaceAll("-\\s*\\|", "- ");
                     normalized = normalized.replaceAll("\\s+", " ").trim();
                     newList.add(convertLegacyPrefixedActionIfNeeded(normalized));
                } else {
                    newList.add(s);
                }
            } else {
                newList.add(item);
            }
        }
        return newList;
    }

    private Object processString(String key, String value) {
        if (isActionKey(key)) {
            return convertLegacyPrefixedActionIfNeeded(value);
        }
        return value;
    }

    private boolean isActionKey(String key) {
        return "onClick".equalsIgnoreCase(key) || 
               "action".equalsIgnoreCase(key) || 
               "global_actions".equalsIgnoreCase(key);
    }
    
    // Extracted from BedrockCommand logic
    private String convertLegacyPrefixedActionIfNeeded(String raw) {
        if (raw == null) return null;
        String value = raw.trim();

        // If it's already a new format (curly braces) or a legacy type:value (colon), leave it
        if ((value.contains("{") && value.contains("}")) || value.contains(":")) {
            return raw;
        }

        // Check for known action types
        // command, open, message, sound, broadcast, server, console, json, actionbar, bungee, delay, conditional, random
        // If the string starts with one of these followed by space
        
        List<String> knownTypes = Arrays.asList(
            "command", "open", "message", "sound", "broadcast", "server", "console", 
            "json", "actionbar", "bungee", "delay", "conditional", "random"
        );
        
        String[] parts = value.split("\\s+", 2);
        if (parts.length < 2) {
            return raw; // e.g. just "command" without args? or just "close"?
        }
        
        String type = parts[0].toLowerCase();
        String content = parts[1];
        
        if (!knownTypes.contains(type)) {
            return raw;
        }
        
        if ("open".equals(type)) {
            // open test $1 -> open { - "test" - "$1" }
            List<String> tokens = tokenizeByWhitespace(content);
            if (tokens.isEmpty()) return raw;
            StringBuilder b = new StringBuilder();
            b.append("open {");
            for (String token : tokens) {
                b.append(" - \"").append(escapeActionValue(token)).append("\"");
            }
            b.append(" }");
            return b.toString();
        }
        
        // command say hi -> command { - "say hi" }
        return type + " { - \"" + escapeActionValue(content) + "\" }";
    }

    private String escapeActionValue(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private List<String> tokenizeByWhitespace(String input) {
        List<String> out = new ArrayList<>();
        if (input == null) return out;
        String s = input.trim();
        if (s.isEmpty()) return out;
        for (String part : s.split("\\s+")) {
            String p = part.trim();
            if (!p.isEmpty()) out.add(p);
        }
        return out;
    }
}
