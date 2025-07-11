package it.pintux.life.common.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageData {

    private static String PREFIX = "prefix";
    public static String NO_PEX = "noPex";
    public static String MENU_NOPEX = "menu.noPex";
    public static String MENU_NOJAVA = "menu.noJava";
    public static String MENU_ARGS = "menu.arguments";
    public static String MENU_NOT_FOUND = "menu.notFound";

    private static final Pattern hexPattern = Pattern.compile("<#([A-Fa-f0-9]){6}>");

    private static MessageConfig config;

    public MessageData(MessageConfig config) {
        this.config = config;
    }

    public String getValueNoPrefix(String key, Map<String, Object> replacements, FormPlayer player) {
        String value = getValueFrom(key);
        value = replaceVariables(value, replacements, player);
        return value;
    }

    public String getValue(String key, Map<String, Object> replacements, FormPlayer player) {
        String prefix = getValueFrom("prefix");
        String value = getValueNoPrefix(key, replacements, player);
        return prefix.concat(" ").concat(value);
    }

    private String getValueFrom(String key) {
        return applyColor(getValueFromConfig(key));
    }

    private String getValueFromConfig(String path) {
        String currentElement = config.getString(path);
        return currentElement == null || currentElement.isBlank()
                ? "&aValue not found in &6messages.yml &afor &6" + path + " &a - please add it manually."
                : currentElement;
    }

    public String replaceVariables(String value, Map<String, Object> replacements, FormPlayer player) {
        if (replacements != null) {
            for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                String placeholder = "$" + entry.getKey();
                value = value.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        value = config.setPlaceholders(player, value);

        Matcher matcher = Pattern.compile("\\$(\\w+)").matcher(value);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            System.out.println("Missing replacement value for placeholder: $" + placeholder);
        }

        return value;
    }

    public String applyColor(String message) {
        return config.applyColor(message);
    }
}
