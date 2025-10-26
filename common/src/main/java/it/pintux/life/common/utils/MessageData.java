package it.pintux.life.common.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageData {


    private static String PREFIX = "prefix";


    public static String NO_PEX = "noPex";
    public static String NO_PERMISSION = "no_permission";


    public static String MENU_NOPEX = "menu.noPex";
    public static String MENU_NOJAVA = "menu.noJava";
    public static String MENU_ARGS = "menu.arguments";
    public static String MENU_NOT_FOUND = "menu.notFound";
    public static String MENU_NOT_FOUND_ALT = "menu.menu_not_found";


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


    public static String FORM_TIMEOUT = "form.timeout";
    public static String FORM_VALIDATION_FAILED = "form.validation_failed";


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


    public static String PLAYER_NOT_FOUND = "player.not_found";


    public static final String ACTION_LIST_SUCCESS = "action.list.success";
    public static final String ACTION_LIST_NO_DATA = "action.list.no_data";
    public static final String ACTION_LIST_INVALID_PAGE = "action.list.invalid_page";
    public static final String ACTION_LIST_GENERATED = "action.list.generated";


    public static final String SYSTEM_TEMPLATE_NOT_FOUND = "system.template_not_found";
    public static final String SYSTEM_FORM_DISPLAY_ERROR = "system.form_display_error";
    public static final String SYSTEM_FORM_SENT_SUCCESS = "system.form_sent_success";
    public static final String SYSTEM_FORM_SEND_FAILED = "system.form_send_failed";
    public static final String SYSTEM_UNEXPECTED_ERROR = "system.unexpected_error";
    public static final String SYSTEM_SERVICE_UNAVAILABLE = "system.service_unavailable";
    public static final String SYSTEM_SERVICE_STATUS_CHECK_FAILED = "system.service_status_check_failed";
    public static final String SYSTEM_COMMAND_EXECUTION_FAILED = "system.command_execution_failed";
    public static final String SYSTEM_OPERATION_INTERRUPTED = "system.operation_interrupted";


    public static final String VALIDATION_INVALID_MENU_NAME = "validation.invalid_menu_name";
    public static final String VALIDATION_INVALID_FORM_TYPE = "validation.invalid_form_type";
    public static final String VALIDATION_INVALID_IMAGE_SOURCE = "validation.invalid_image_source";
    public static final String VALIDATION_INVALID_ACTION_FORMAT = "validation.invalid_action_format";
    public static final String VALIDATION_INVALID_DELAY_FORMAT = "validation.invalid_delay_format";
    public static final String VALIDATION_INVALID_TARGET_MENU = "validation.invalid_target_menu";
    public static final String VALIDATION_INVALID_ECONOMY_ACTION = "validation.invalid_economy_action";
    public static final String VALIDATION_INVALID_ECONOMY_AMOUNT = "validation.invalid_economy_amount";
    public static final String VALIDATION_INVALID_COMMAND_FORMAT = "validation.invalid_command_format";
    public static final String VALIDATION_INVALID_PATTERN = "validation.invalid_pattern";
    public static final String VALIDATION_INVALID_EMAIL_FORMAT = "validation.invalid_email_format";
    public static final String VALIDATION_INVALID_PLAYER_NAME = "validation.invalid_player_name";
    public static final String VALIDATION_INVALID_PERMISSION_FORMAT = "validation.invalid_permission_format";
    public static final String VALIDATION_INVALID_MONETARY_AMOUNT = "validation.invalid_monetary_amount";
    public static final String VALIDATION_RULE_FAILED = "validation.rule_failed";
    public static final String VALIDATION_INVALID_CONDITION_FORMAT = "validation.invalid_condition_format";
    public static final String VALIDATION_INVALID_CONDITION_NOT_FORMAT = "validation.invalid_condition_not_format";
    public static final String VALIDATION_CONDITION_EVALUATION_ERROR = "validation.condition_evaluation_error";
    public static final String VALIDATION_PLACEHOLDER_CONDITION_ERROR = "validation.placeholder_condition_error";
    public static final String VALIDATION_INVALID_STEP_INDEX = "validation.invalid_step_index";


    public static final String ACTIONS_SOUND_NO_SOUND_SPECIFIED = "actions.sound.no_sound_specified";
    public static final String ACTIONS_SOUND_INVALID_VOLUME = "actions.sound.invalid_volume";
    public static final String ACTIONS_SOUND_INVALID_PITCH = "actions.sound.invalid_pitch";
    public static final String ACTIONS_SOUND_SOUND_PLAYED = "actions.sound.sound_played";
    public static final String ACTIONS_SOUND_PLAY_FAILED = "actions.sound.play_failed";
    public static final String ACTIONS_SOUND_PLAY_ERROR = "actions.sound.play_error";


    public static final String ACTIONS_TITLE_INVALID_PARAMETERS = "actions.title.invalid_parameters";
    public static final String ACTIONS_TITLE_SYSTEM_UNAVAILABLE = "actions.title.system_unavailable";
    public static final String ACTIONS_TITLE_TITLE_SENT = "actions.title.title_sent";
    public static final String ACTIONS_TITLE_SEND_FAILED = "actions.title.send_failed";
    public static final String ACTIONS_TITLE_SEND_ERROR = "actions.title.send_error";


    public static final String ACTIONS_SERVER_INVALID_PARAMETERS = "actions.server.invalid_parameters";
    public static final String ACTIONS_SERVER_NO_COMMAND_SPECIFIED = "actions.server.no_command_specified";
    public static final String ACTIONS_SERVER_COMMAND_EXECUTED = "actions.server.command_executed";
    public static final String ACTIONS_SERVER_EXECUTION_FAILED = "actions.server.execution_failed";
    public static final String ACTIONS_SERVER_EXECUTION_ERROR = "actions.server.execution_error";


    public static final String ACTIONS_POTION_INVALID_EFFECT = "actions.potion.invalid_effect";
    public static final String ACTIONS_POTION_DURATION_POSITIVE = "actions.potion.duration_positive";
    public static final String ACTIONS_POTION_INVALID_DURATION_FORMAT = "actions.potion.invalid_duration_format";
    public static final String ACTIONS_POTION_AMPLIFIER_RANGE = "actions.potion.amplifier_range";
    public static final String ACTIONS_POTION_INVALID_AMPLIFIER = "actions.potion.invalid_amplifier";
    public static final String ACTIONS_POTION_EFFECT_GIVEN = "actions.potion.effect_given";
    public static final String ACTIONS_POTION_GIVE_FAILED = "actions.potion.give_failed";
    public static final String ACTIONS_POTION_EFFECT_REMOVED = "actions.potion.effect_removed";
    public static final String ACTIONS_POTION_REMOVE_FAILED = "actions.potion.remove_failed";
    public static final String ACTIONS_POTION_EFFECTS_CLEARED = "actions.potion.effects_cleared";
    public static final String ACTIONS_POTION_CLEAR_FAILED = "actions.potion.clear_failed";
    public static final String ACTIONS_POTION_EFFECTS_LISTED = "actions.potion.effects_listed";
    public static final String ACTIONS_POTION_LIST_FAILED = "actions.potion.list_failed";


    public static final String ACTIONS_RANDOM_ACTION_EXECUTED = "actions.random.action_executed";
    public static final String ACTIONS_RANDOM_ACTION_FAILED = "actions.random.action_failed";
    public static final String ACTIONS_RANDOM_EXECUTION_INTERRUPTED = "actions.random.execution_interrupted";
    public static final String ACTIONS_RANDOM_EXECUTION_FAILED = "actions.random.execution_failed";


    public static final String FORMS_MODAL_DISPLAY_ERROR = "forms.modal_display_error";
    public static final String FORMS_MENU_DISPLAY_ERROR = "forms.menu_display_error";
    public static final String FORMS_CUSTOM_DISPLAY_ERROR = "forms.custom_display_error";
    public static final String FORMS_SYSTEM_UNAVAILABLE = "forms.system_unavailable";
    public static final String FORMS_INVALID_ACTION_FORMAT = "forms.invalid_action_format";
    public static final String FORMS_ACTION_FAILED = "forms.action_failed";
    public static final String FORMS_INVALID_ACTION_SEQUENCE = "forms.invalid_action_sequence";
    public static final String FORMS_SEQUENCE_PARTIAL_FAILURE = "forms.sequence_partial_failure";
    public static final String FORMS_SEQUENCE_ERROR = "forms.sequence_error";
    public static final String FORMS_FALLBACK_MESSAGE_SENT = "forms.fallback_message_sent";
    public static final String FORMS_FALLBACK_ERROR = "forms.fallback_error";


    public static final String INTEGRATIONS_PLACEHOLDERAPI_NOT_FOUND = "integrations.placeholderapi.not_found";
    public static final String INTEGRATIONS_PLACEHOLDERAPI_INITIALIZED = "integrations.placeholderapi.initialized";
    public static final String INTEGRATIONS_PLACEHOLDERAPI_CLASS_NOT_FOUND = "integrations.placeholderapi.class_not_found";
    public static final String INTEGRATIONS_PLACEHOLDERAPI_INIT_FAILED = "integrations.placeholderapi.init_failed";


    public static final String ERRORS_OPERATION_FAILED = "errors.operation_failed";
    public static final String ERRORS_OPERATION_FAILED_FALLBACK = "errors.operation_failed_fallback";
    public static final String ERRORS_FALLBACK_FAILED = "errors.fallback_failed";
    public static final String ERRORS_OPERATION_TIMEOUT = "errors.operation_timeout";
    public static final String ERRORS_FALLBACK_ERROR = "errors.fallback_error";
    public static final String ERRORS_START_FAILED = "errors.start_failed";
    public static final String ERRORS_ACTION_RESULT_ERROR = "errors.action_result_error";
    public static final String ERRORS_BASIC_FALLBACK = "errors.basic_fallback";


    public static final String EVENTS_HANDLER_ERROR = "events.handler_error";
    public static final String EVENTS_LISTENER_ERROR = "events.listener_error";
    public static final String EVENTS_VALIDATION_FAILED = "events.validation_failed";
    public static final String EVENTS_FORM_CLOSED = "events.form_closed";
    public static final String EVENTS_FORM_ERROR = "events.form_error";
    public static final String EVENTS_SEND_FAILED = "events.send_failed";


    public static final String VALIDATION_NO_FORMS = "validation.no_forms";
    public static final String VALIDATION_MENU_NULL = "validation.menu_null";
    public static final String VALIDATION_NO_TITLE = "validation.no_title";
    public static final String VALIDATION_NO_BUTTONS = "validation.no_buttons";
    public static final String VALIDATION_BUTTON_NULL = "validation.button_null";
    public static final String VALIDATION_BUTTON_NO_TEXT = "validation.button_no_text";
    public static final String VALIDATION_BUTTON_INVALID_IMAGE = "validation.button_invalid_image";
    public static final String VALIDATION_BUTTON_NO_ACTION = "validation.button_no_action";
    public static final String VALIDATION_UNKNOWN_ACTION_TYPE = "validation.unknown_action_type";
    public static final String VALIDATION_EMPTY_ACTION_VALUE = "validation.empty_action_value";
    public static final String VALIDATION_LEGACY_FORMAT_DETECTED = "validation.legacy_format_detected";
    public static final String VALIDATION_INVALID_SIZE = "validation.invalid_size";
    public static final String VALIDATION_INVALID_INTERVAL = "validation.invalid_interval";
    public static final String VALIDATION_DIRECTORY_NOT_FOUND = "validation.directory_not_found";
    public static final String VALIDATION_DIRECTORY_NOT_READABLE = "validation.directory_not_readable";

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

                String dollarPlaceholder = "$" + entry.getKey();
                String curlyPlaceholder = "{" + entry.getKey() + "}";
                String replacementValue = String.valueOf(entry.getValue());

                value = value.replace(dollarPlaceholder, replacementValue);
                value = value.replace(curlyPlaceholder, replacementValue);
            }
        }

        value = config.setPlaceholders(player, value);


        Matcher dollarMatcher = Pattern.compile("\\$(\\w+)").matcher(value);
        while (dollarMatcher.find()) {
            String placeholder = dollarMatcher.group(1);

        }

        Matcher curlyMatcher = Pattern.compile("\\{(\\w+)\\}").matcher(value);
        while (curlyMatcher.find()) {
            String placeholder = curlyMatcher.group(1);

        }

        return value;
    }

    public String applyColor(String message) {
        return config.applyColor(message);
    }
}


