package it.pintux.life.common.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageData {

    // Message path constants for messages.yml
    private static String PREFIX = "prefix";
    
    // Permission and access messages
    public static String NO_PEX = "noPex";
    public static String NO_PERMISSION = "no_permission";
    
    // Menu-related messages
    public static String MENU_NOPEX = "menu.noPex";
    public static String MENU_NOJAVA = "menu.noJava";
    public static String MENU_ARGS = "menu.arguments";
    public static String MENU_NOT_FOUND = "menu.notFound";
    public static String MENU_NOT_FOUND_ALT = "menu.menu_not_found";
    
    // Action result messages
    public static String ACTION_SUCCESS = "action.success";
    public static String ACTION_FAILED = "action.failed";
    public static String ACTION_INVALID_PARAMETERS = "action.invalid_parameters";
    public static String ACTION_INVALID_FORMAT = "action.invalid_format";
    public static String ACTION_EXECUTION_ERROR = "action.execution_error";
    public static String ACTION_COMMAND_SUCCESS = "action.command_success";
    public static String ACTION_COMMAND_FAILED = "action.command_failed";
    public static String ACTION_TELEPORT_SUCCESS = "action.teleport_success";
    public static String ACTION_TELEPORT_FAILED = "action.teleport_failed";
    public static String ACTION_MESSAGE_SENT = "action.message_sent";
    public static String ACTION_MESSAGE_FAILED = "action.message_failed";
    public static String ACTION_SOUND_FAILED = "action.sound_failed";
    public static String ACTION_BROADCAST_SUCCESS = "action.broadcast_success";
    public static String ACTION_BROADCAST_FAILED = "action.broadcast_failed";
    public static String ACTION_FORM_OPENED = "action.form_opened";
    public static String ACTION_FORM_NOT_FOUND = "action.form_not_found";
    public static String ACTION_PARTICLE_SUCCESS = "action.particle_success";
    public static String ACTION_PARTICLE_FAILED = "action.particle_failed";
    public static final String ACTION_TELEPORT_OUT_OF_BOUNDS = "action.teleport.out_of_bounds";
    public static final String ACTION_TELEPORT_INVALID_Y = "action.teleport.invalid_y";
    public static final String ACTION_ECONOMY_NOT_AVAILABLE = "action.economy.not_available";
    public static String ACTION_ECONOMY_INVALID_AMOUNT = "action.economy_invalid_amount";
    public static String ACTION_ECONOMY_ADD_FAILED = "action.economy_add_failed";
    public static String ACTION_ECONOMY_REMOVE_FAILED = "action.economy_remove_failed";
    public static String ACTION_ECONOMY_SET_FAILED = "action.economy_set_failed";
    public static String ACTION_ECONOMY_INSUFFICIENT = "action.economy_insufficient";
    
    // Form-related messages
    public static String FORM_TIMEOUT = "form.timeout";
    public static String FORM_VALIDATION_FAILED = "form.validation_failed";
    
    // Economy messages
    public static String ECONOMY_INSUFFICIENT = "economy.insufficient";
    public static String ECONOMY_ADD_SUCCESS = "economy.add_success";
    public static String ECONOMY_ADD_FAILED = "economy.add_failed";
    public static String ECONOMY_ADD_INVALID_FORMAT = "economy.add_invalid_format";
    public static String ECONOMY_REMOVE_SUCCESS = "economy.remove_success";
    public static String ECONOMY_REMOVE_FAILED = "economy.remove_failed";
    public static String ECONOMY_REMOVE_INVALID_FORMAT = "economy.remove_invalid_format";
    public static String ECONOMY_SET_SUCCESS = "economy.set_success";
    public static String ECONOMY_SET_FAILED = "economy.set_failed";
    public static String ECONOMY_SET_INVALID_FORMAT = "economy.set_invalid_format";
    public static String ECONOMY_CHECK_SUCCESS = "economy.check_success";
    public static String ECONOMY_CHECK_INVALID_FORMAT = "economy.check_invalid_format";
    public static String ECONOMY_PAY_SUCCESS = "economy.pay_success";
    public static String ECONOMY_PAY_FAILED = "economy.pay_failed";
    public static String ECONOMY_PAY_INVALID_FORMAT = "economy.pay_invalid_format";
    public static String ECONOMY_INSUFFICIENT_FUNDS = "economy.insufficient_funds";
    public static String ECONOMY_AMOUNT_POSITIVE = "economy.amount_positive";
    public static String ECONOMY_AMOUNT_NEGATIVE = "economy.amount_negative";
    
    // Player-related messages
    public static String PLAYER_NOT_FOUND = "player.not_found";
    

    
    // List action messages
    public static final String ACTION_LIST_SUCCESS = "action.list.success";
    public static final String ACTION_LIST_NO_DATA = "action.list.no_data";
    public static final String ACTION_LIST_INVALID_PAGE = "action.list.invalid_page";
    public static final String ACTION_LIST_GENERATED = "action.list.generated";

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
            // Debug logging removed for security - placeholder not found
        }

        return value;
    }

    public String applyColor(String message) {
        return config.applyColor(message);
    }
}
