package it.pintux.life.common.utils;

import java.util.Map;

public class MessageData {

    public static String NO_PEX = "noPex";

    public static String MENU_NOPEX = "menu.no_permission";
    public static String MENU_NOJAVA = "menu.noJava";
    public static String MENU_ARGS = "menu.arguments";
    public static String MENU_NOT_FOUND = "menu.notFound";

    public static String ACTION_SUCCESS = "action.success";
    public static String ACTION_INVALID_PARAMETERS = "action.invalid_parameters";
    public static String ACTION_INVALID_FORMAT = "action.invalid_format";
    public static String EXECUTION_ERROR = "action.execution_error";
    public static String ACTION_COMMAND_SUCCESS = "action.command_success";
    public static String ACTION_COMMAND_FAILED = "action.command_failed";
    public static String ACTION_MESSAGE_SENT = "action.message_sent";
    public static String ACTION_ECONOMY_NOT_AVAILABLE = "action.economy.not_available";
    public static String PLAYER_NOT_FOUND = "player.not_found";

    public static String SYSTEM_SERVICE_UNAVAILABLE = "system.service_unavailable";
    public static String SYSTEM_SERVICE_STATUS_CHECK_FAILED = "system.service_status_check_failed";
    public static String SYSTEM_COMMAND_EXECUTION_FAILED = "system.command_execution_failed";
    public static String SYSTEM_OPERATION_INTERRUPTED = "system.operation_interrupted";

    public static String VALIDATION_INVALID_MENU_NAME = "validation.invalid_menu_name";
    public static String VALIDATION_INVALID_FORM_TYPE = "validation.invalid_form_type";
    public static String VALIDATION_INVALID_ACTION_FORMAT = "validation.invalid_action_format";

    public static String FORMS_SYSTEM_UNAVAILABLE = "forms.system_unavailable";
    public static String FORMS_INVALID_ACTION_FORMAT = "forms.invalid_action_format";
    public static String FORMS_ACTION_FAILED = "forms.action_failed";
    public static String FORMS_INVALID_ACTION_SEQUENCE = "forms.invalid_action_sequence";
    public static String FORMS_SEQUENCE_PARTIAL_FAILURE = "forms.sequence_partial_failure";
    public static String FORMS_SEQUENCE_ERROR = "forms.sequence_error";
    public static String FORMS_NO_VALID_ACTIONS = "forms.no_valid_actions";
    public static String FORMS_NO_INLINE_FORMS = "forms.no_inline_forms";
    public static String FORMS_CONVERSION_SUCCESS = "forms.conversion_success";
    public static String FORMS_CONVERSION_FAILED = "forms.conversion_failed";

    public static String COMMAND_NO_PERMISSION = "command.no_permission";
    public static String COMMAND_RELOAD_SUCCESS = "command.reload_success";
    public static String COMMAND_PLAYER_ONLY = "command.player_only";
    public static String COMMAND_USAGE_RELOAD = "command.usage.reload";
    public static String COMMAND_USAGE_OPEN = "command.usage.open";
    public static String COMMAND_USAGE_OPENFOR = "command.usage.openfor";
    public static String COMMAND_USAGE_CONVERT = "command.usage.convert";
    public static String COMMAND_OPENED_FOR = "command.opened_for";

    public static String VALIDATION_NO_FORMS = "validation.no_forms";
    public static String VALIDATION_MENU_NULL = "validation.menu_null";
    public static String VALIDATION_NO_TITLE = "validation.no_title";
    public static String VALIDATION_NO_BUTTONS = "validation.no_buttons";
    public static String VALIDATION_BUTTON_NULL = "validation.button_null";
    public static String VALIDATION_BUTTON_NO_TEXT = "validation.button_no_text";
    public static String VALIDATION_BUTTON_INVALID_IMAGE = "validation.button_invalid_image";
    public static String VALIDATION_UNKNOWN_ACTION_TYPE = "validation.unknown_action_type";
    public static String VALIDATION_LEGACY_FORMAT_DETECTED = "validation.legacy_format_detected";

    private final MessageConfig config;

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

                String dollarPlaceholder = "$" + entry.getKey();
                String curlyPlaceholder = "{" + entry.getKey() + "}";
                String replacementValue = String.valueOf(entry.getValue());

                value = value.replace(dollarPlaceholder, replacementValue);
                value = value.replace(curlyPlaceholder, replacementValue);
            }
        }

        value = config.setPlaceholders(player, value);
        if (player != null && value != null && value.contains("%")) {
            String name = player.getName();
            String uuid = player.getUniqueId().toString();
            value = value.replace("%player_name%", name)
                         .replace("%player%", name)
                         .replace("%player_uuid%", uuid);
        }
        return value;
    }

    public String applyColor(String message) {
        return config.applyColor(message);
    }
}


