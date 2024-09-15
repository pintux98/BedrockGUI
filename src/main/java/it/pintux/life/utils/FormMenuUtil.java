package it.pintux.life.utils;

import it.pintux.life.BedrockGUI;
import it.pintux.life.MessageData;
import it.pintux.life.form.FormButton;
import it.pintux.life.form.FormMenu;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.floodgate.api.FloodgateApi;
import org.bukkit.entity.Player;

import java.util.*;

public class FormMenuUtil {

    private final Map<String, FormMenu> formMenus;
    private final BedrockGUI plugin;

    public FormMenuUtil(BedrockGUI plugin) {
        this.plugin = plugin;
        formMenus = new HashMap<>();
        loadFormMenus();
    }

    private void loadFormMenus() {
        FileConfiguration config = plugin.getConfig();
        for (String key : config.getConfigurationSection("menu").getKeys(false)) {
            String command = config.getString("menu." + key + ".command");
            String permission = config.getString("menu." + key + ".permission");
            String type = config.getString("menu." + key + ".type", "SIMPLE");
            String title = config.getString("menu." + key + ".title", "Unknown");
            String description = config.getString("menu." + key + ".description");
            List<FormButton> buttons = new ArrayList<>();
            for (String button : config.getConfigurationSection("menu." + key + ".buttons").getKeys(false)) {
                String text = config.getString("menu." + key + ".buttons." + button + ".text");
                String image = config.getString("menu." + key + ".buttons." + button + ".image");
                String onClick = config.getString("menu." + key + ".buttons." + button + ".onClick");
                buttons.add(new FormButton(text, image, onClick));
            }
            if (type.equalsIgnoreCase("MODAL")) {
                if (buttons.size() != 2) {
                    plugin.getLogger().severe("Modal's must only have 2 buttons! Please modify menu." + key);
                    continue;
                }
            }
            FormMenu menu = new FormMenu(command, permission, title, description, type, buttons);
            formMenus.put(key.toLowerCase(), menu);
            plugin.getLogger().info("Loaded form menu: " + key + " type: " + type);
        }
    }

    public void openForm(Player player, String menuName, String[] args) {
        FormMenu menu = formMenus.get(menuName.toLowerCase());

        if (menu.getPermission() != null && !player.hasPermission(menu.getPermission())) {
            player.sendMessage(MessageData.getValue(MessageData.MENU_NOPEX, null, null));
            return;
        }

        if (menu.getFormCommand() != null && !validateCommandArguments(menu.getFormCommand(), args, player)) {
            return;
        }

        String type = menu.getFormType();

        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            placeholders.put(String.valueOf(i + 1), args[i]);
        }

        switch (type.toUpperCase()) {
            case "MODAL":
                openModalForm(player, menu, placeholders);
                break;
            case "SIMPLE":
                openSimpleForm(player, menu, placeholders);
                break;
        }
    }

    private void openModalForm(Player player, FormMenu formMenu, Map<String, String> placeholders) {
        String title = replacePlaceholders(formMenu.getFormTitle(), placeholders, player);

        List<FormButton> buttons = formMenu.getFormButtons();
        FormButton b1 = buttons.get(0);
        FormButton b2 = buttons.get(1);

        ModalForm.Builder formBuilder = ModalForm.builder()
                .title(title);

        String content = formMenu.getFormContent();
        if (content != null) {
            formBuilder.content(replacePlaceholders(content, placeholders, player));
        }

        formBuilder
                .button1(replacePlaceholders(b1.getText(), placeholders, player))
                .button2(replacePlaceholders(b2.getText(), placeholders, player))
                .validResultHandler((formResponse, modalResponse) -> {
                    if (modalResponse.clickedButtonId() == 0) {
                        if (b1.getOnClick() != null) {
                            handleOnClick(player, b1.getOnClick(), placeholders);
                        }
                    } else {
                        if (b2.getOnClick() != null) {
                            handleOnClick(player, b2.getOnClick(), placeholders);
                        }
                    }
                })
                .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), formBuilder.build());
    }

    private void openSimpleForm(Player player, FormMenu formMenu, Map<String, String> placeholders) {
        String title = replacePlaceholders(formMenu.getFormTitle(), placeholders, player);

        List<FormButton> buttons = formMenu.getFormButtons();
        SimpleForm.Builder formBuilder = SimpleForm.builder().title(title);

        String content = formMenu.getFormContent();
        if (content != null) {
            formBuilder.content(replacePlaceholders(content, placeholders, player));
        }

        List<String> onClickActions = new ArrayList<>();
        for (FormButton button : buttons) {
            String buttonText = replacePlaceholders(button.getText(), placeholders, player);
            if (button.getImage() != null) {
                formBuilder.button(buttonText, FormImage.Type.URL, button.getImage());
            } else {
                formBuilder.button(buttonText);
            }
            if (button.getOnClick() != null) {
                onClickActions.add(button.getOnClick());
            }
        }

        formBuilder.validResultHandler((form, response) -> {
            int clickedButtonId = response.clickedButtonId();
            String action = onClickActions.get(clickedButtonId);

            handleOnClick(player, action, placeholders);
        });

        SimpleForm form = formBuilder.build();
        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private void handleOnClick(Player player, String onClickAction, Map<String, String> placeholders) {
        onClickAction = replacePlaceholders(onClickAction.trim().replaceAll("\\s+", " "), placeholders, player);

        String[] parts = onClickAction.split(" ", 2);

        if (parts.length < 2) {
            player.sendMessage("Invalid onClick action: " + onClickAction);
            return;
        }

        String action = parts[0];
        String value = parts[1];

        if (action.equalsIgnoreCase("command")) {
            player.performCommand(value);
        } else if (action.equalsIgnoreCase("open")) {
            String[] newArgs = value.split(" ");
            String menuName = newArgs[0];
            String[] actualArgs = Arrays.copyOfRange(newArgs, 1, newArgs.length);
            openForm(player, menuName, actualArgs);
        }
    }

    private boolean validateCommandArguments(String command, String[] args, Player player) {
        if (command == null || command.isEmpty()) {
            return true;
        }
        int requiredArgs = countPlaceholders(command);
        if (args.length < requiredArgs) {
            player.sendMessage(MessageData.getValue(MessageData.MENU_ARGS, Map.of("args", requiredArgs), null));
            return false;
        }
        return true;
    }

    private int countPlaceholders(String command) {
        int count = 0;
        while (command.contains("$" + (count + 1))) {
            count++;
        }
        return count;
    }

    private String replacePlaceholders(String text, Map<String, String> placeholders, Player player) {
        if (text == null) return null;

        if (plugin.isPlaceholderAPI() && text.contains("%")) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("$" + entry.getKey(), entry.getValue());
        }

        return text;
    }

    public Map<String, FormMenu> getFormMenus() {
        return formMenus;
    }
}