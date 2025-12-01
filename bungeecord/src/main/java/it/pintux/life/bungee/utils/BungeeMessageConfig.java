package it.pintux.life.bungee.utils;

import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.FormPlayer;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class BungeeMessageConfig implements MessageConfig {
    private final File dataFolder;
    private final String fileName;
    private Map<String, Object> messages;
    private final Yaml yaml;

    public BungeeMessageConfig(File dataFolder, String fileName) {
        this.dataFolder = dataFolder;
        this.fileName = fileName;
        this.yaml = new Yaml();
        loadMessages();
    }

    private void loadMessages() {
        File messageFile = new File(dataFolder, fileName);
        if (!messageFile.exists()) { createDefaultMessages(messageFile); }
        try (InputStream inputStream = new FileInputStream(messageFile)) {
            messages = yaml.load(inputStream);
            if (messages == null) { messages = new HashMap<>(); }
        } catch (IOException e) { throw new RuntimeException("Failed to load " + fileName, e); }
    }

    private void createDefaultMessages(File messageFile) {
        try {
            messageFile.getParentFile().mkdirs();
            InputStream defaultMessages = getClass().getResourceAsStream("/" + fileName);
            if (defaultMessages != null) { Files.copy(defaultMessages, messageFile.toPath()); }
            else {
                String defaultContent = """
                    prefix: "&8[&6BedrockGUI&8] &f"
                    no-permission: "{prefix}&cYou don't have permission to use this command."
                    menu-not-found: "{prefix}&cMenu not found: {menu}"
                    player-not-found: "{prefix}&cPlayer not found: {player}"
                    menu-opened: "{prefix}&aMenu opened successfully!"
                    reload-success: "{prefix}&aConfiguration reloaded successfully!"
                    """;
                Files.write(messageFile.toPath(), defaultContent.getBytes());
            }
        } catch (IOException e) { throw new RuntimeException("Failed to create default messages", e); }
    }

    @Override
    public String getString(String path) {
        Object value = getValue(path);
        return value != null ? value.toString() : "";
    }

    @Override
    public String setPlaceholders(FormPlayer player, String message) {
        if (message == null) return "";
        String result = message;
        if (player != null) { result = result.replace("{player}", player.getName()); }
        return result;
    }

    @Override
    public String applyColor(String message) { return message.replace("&", "§"); }

    public void reload() { loadMessages(); }

    private Object getValue(String path) {
        if (path == null || path.isEmpty()) return null;
        String[] parts = path.split("\\.");
        Map<String, Object> current = messages;
        for (int i = 0; i < parts.length - 1; i++) {
            Object v = current.get(parts[i]);
            if (v instanceof Map) { current = (Map<String, Object>) v; }
            else { return null; }
        }
        return current.get(parts[parts.length - 1]);
    }
}