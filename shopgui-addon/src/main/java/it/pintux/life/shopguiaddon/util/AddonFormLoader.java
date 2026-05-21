package it.pintux.life.shopguiaddon.util;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class AddonFormLoader {

    private final Logger logger;
    private final File formsDirectory;
    private final Map<String, AddonFormDefinition> registeredForms = new ConcurrentHashMap<>();

    public AddonFormLoader(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
        this.formsDirectory = new File(plugin.getDataFolder(), "forms");
    }

    public void loadAll() {
        if (!formsDirectory.exists() || !formsDirectory.isDirectory()) {
            logger.info("No addon forms directory found at " + formsDirectory.getAbsolutePath());
            return;
        }
        File[] ymlFiles = formsDirectory.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (ymlFiles == null || ymlFiles.length == 0) {
            logger.info("No addon form files found in " + formsDirectory.getAbsolutePath());
            return;
        }
        for (File file : ymlFiles) {
            loadForm(file);
        }
        logger.info("Loaded " + registeredForms.size() + " addon forms: " + registeredForms.keySet());
    }

    private void loadForm(File file) {
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String formName = file.getName().replace(".yml", "").replace(".yaml", "").toLowerCase();
            String type = cfg.getString("type", "SIMPLE");
            String title = cfg.getString("title", formName);
            String content = cfg.getString("content", "");

            List<ButtonDefinition> buttons = new ArrayList<>();
            if (cfg.isConfigurationSection("buttons")) {
                for (String buttonKey : cfg.getConfigurationSection("buttons").getKeys(false)) {
                    String text = cfg.getString("buttons." + buttonKey + ".text", buttonKey);
                    String image = cfg.getString("buttons." + buttonKey + ".image");
                    List<String> onClick = new ArrayList<>();
                    Object onClickObj = cfg.get("buttons." + buttonKey + ".onClick");
                    if (onClickObj instanceof List) {
                        for (Object item : (List<?>) onClickObj) {
                            if (item != null) onClick.add(item.toString().trim());
                        }
                    } else if (onClickObj instanceof String) {
                        onClick.add(((String) onClickObj).trim());
                    }
                    buttons.add(new ButtonDefinition(text, image, onClick));
                }
            }

            registeredForms.put(formName, new AddonFormDefinition(formName, title, content, type, buttons));
        } catch (Exception e) {
            logger.warning("Failed to load addon form from " + file.getName() + ": " + e.getMessage());
        }
    }

    public boolean hasForm(String formName) {
        return registeredForms.containsKey(formName.toLowerCase());
    }

    public void openForm(FormPlayer player, String formName) {
        AddonFormDefinition formDef = registeredForms.get(formName.toLowerCase());
        if (formDef == null) {
            player.sendMessage("&cAddon form not found: " + formName);
            return;
        }
        BedrockGUIApi api = BedrockGUIApi.getInstance();
        if (api == null) {
            player.sendMessage("&cBedrockGUI API is not available.");
            return;
        }
        formDef.send(api, player);
    }

    public Map<String, AddonFormDefinition> getRegisteredForms() {
        return registeredForms;
    }

    public record ButtonDefinition(String text, String image, List<String> onClick) {}

    public record AddonFormDefinition(String name, String title, String content, String type, List<ButtonDefinition> buttons) {
        public void send(BedrockGUIApi api, FormPlayer player) {
            BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(title);
            if (content != null && !content.isEmpty()) {
                form.content(content);
            }
            for (ButtonDefinition btn : buttons) {
                String actionString = btn.onClick().isEmpty() ? "" : String.join(", ", btn.onClick());
                if (btn.image() != null && !btn.image().isEmpty()) {
                    form.button(btn.text(), btn.image(), fp -> {
                        if (!actionString.isEmpty()) {
                            api.executeActionString(fp, actionString,
                                    api.createActionContext(Map.of(), Map.of(), Map.of("source", "addon-form:" + name), name, "simple"));
                        }
                    });
                } else {
                    form.button(btn.text(), fp -> {
                        if (!actionString.isEmpty()) {
                            api.executeActionString(fp, actionString,
                                    api.createActionContext(Map.of(), Map.of(), Map.of("source", "addon-form:" + name), name, "simple"));
                        }
                    });
                }
            }
            form.send(player);
        }
    }
}
