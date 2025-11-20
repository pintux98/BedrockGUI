package it.pintux.life.velocity.utils;

import it.pintux.life.common.utils.MessageConfig;
import it.pintux.life.common.utils.FormPlayer;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class VelocityMessageConfig implements MessageConfig {

    private final File dataFolder;
    private final String fileName;
    private Map<String, Object> messages;
    private final Yaml yaml;

    public VelocityMessageConfig(File dataFolder, String fileName) {
        this.dataFolder = dataFolder;
        this.fileName = fileName;
        this.yaml = new Yaml();
        loadMessages();
    }

    private void loadMessages() {
        File messageFile = new File(dataFolder, fileName);
        
        // Create default messages file if it doesn't exist
        if (!messageFile.exists()) {
            createDefaultMessages(messageFile);
        }

        try (InputStream inputStream = new FileInputStream(messageFile)) {
            messages = yaml.load(inputStream);
            if (messages == null) {
                messages = new HashMap<>();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + fileName, e);
        }
    }

    private void createDefaultMessages(File messageFile) {
        try {
            messageFile.getParentFile().mkdirs();
            
            // Copy default messages from common module
            InputStream defaultMessages = getClass().getResourceAsStream("/" + fileName);
            if (defaultMessages != null) {
                Files.copy(defaultMessages, messageFile.toPath());
            } else {
                // Create minimal default messages
                String defaultContent = """
                    # BedrockGUI Messages
                    prefix: "&8[&6BedrockGUI&8] &f"
                    no-permission: "{prefix}&cYou don't have permission to use this command."
                    menu-not-found: "{prefix}&cMenu not found: {menu}"
                    player-not-found: "{prefix}&cPlayer not found: {player}"
                    menu-opened: "{prefix}&aMenu opened successfully!"
                    reload-success: "{prefix}&aConfiguration reloaded successfully!"
                    """;
                Files.write(messageFile.toPath(), defaultContent.getBytes());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default messages", e);
        }
    }

    @Override
    public String getString(String path) {
        Object value = messages.get(path);
        return value != null ? value.toString() : "";
    }

    @Override
    public String setPlaceholders(FormPlayer player, String message) {
        // Simple placeholder replacement - can be enhanced later
        return message.replace("{player}", player.getName());
    }

    @Override
    public String applyColor(String message) {
        // Simple color code conversion - can be enhanced later
        return message.replace("&", "§");
    }

    public void reload() {
        loadMessages();
    }
}