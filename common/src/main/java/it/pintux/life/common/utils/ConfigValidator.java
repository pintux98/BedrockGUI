package it.pintux.life.common.utils;
import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.form.obj.FormMenu;
import it.pintux.life.common.form.obj.FormButton;
import it.pintux.life.common.form.obj.ConditionalButton;
import it.pintux.life.common.utils.ValidationUtils;
import it.pintux.life.common.actions.ActionRegistry;
import it.pintux.life.common.actions.ActionSystem.ActionDefinition;
import it.pintux.life.common.actions.ActionParser;

import java.util.*;
import java.util.regex.Pattern;


public class ConfigValidator {

    private static final Logger logger = Logger.getLogger(ConfigValidator.class);
    private final MessageData messageData;
    private final ActionRegistry actionRegistry;


    private static final Set<String> VALID_FORM_TYPES = Set.of("simple", "modal", "custom");


    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    private final List<String> validationErrors = new ArrayList<>();
    private final List<String> validationWarnings = new ArrayList<>();

    public ConfigValidator(MessageData messageData, ActionRegistry actionRegistry) {
        this.messageData = messageData;
        this.actionRegistry = actionRegistry;
    }


    public ValidationResult validateConfiguration(Map<String, FormMenu> formMenus) {
        validationErrors.clear();
        validationWarnings.clear();

        if (formMenus == null || formMenus.isEmpty()) {
            validationErrors.add(messageData.getValueNoPrefix(MessageData.VALIDATION_NO_FORMS, null, null));
            return new ValidationResult(validationErrors, validationWarnings);
        }


        for (Map.Entry<String, FormMenu> entry : formMenus.entrySet()) {
            validateFormMenu(entry.getKey(), entry.getValue());
        }


        checkCircularReferences(formMenus);

        return new ValidationResult(validationErrors, validationWarnings);
    }


    private void validateFormMenu(String menuName, FormMenu formMenu) {
        if (formMenu == null) {
            Map<String, Object> replacements = Map.of("menu", menuName);
            validationErrors.add(messageData.getValueNoPrefix(MessageData.VALIDATION_MENU_NULL, replacements, null));
            return;
        }


        if (!ValidationUtils.isValidMenuName(menuName)) {
            Map<String, Object> replacements = Map.of("name", menuName);
            validationErrors.add(messageData.getValueNoPrefix(MessageData.VALIDATION_INVALID_MENU_NAME, replacements, null));
        }


        String formType = formMenu.getFormType();
        if (formType == null || !VALID_FORM_TYPES.contains(formType.toLowerCase())) {
            Map<String, Object> replacements = Map.of("type", formType, "menu", menuName);
            validationErrors.add(messageData.getValueNoPrefix(MessageData.VALIDATION_INVALID_FORM_TYPE, replacements, null));
        }


        if (ValidationUtils.isNullOrEmpty(formMenu.getFormTitle())) {
            Map<String, Object> replacements = Map.of("menu", menuName);
            validationWarnings.add(messageData.getValueNoPrefix(MessageData.VALIDATION_NO_TITLE, replacements, null));
        }


        validateButtons(menuName, formMenu.getFormButtons());


        validateFormTypeSpecific(menuName, formMenu);


        if (formMenu.getFormCommand() != null) {
            validateCommand(menuName, formMenu.getFormCommand());
        }
    }


    private void validateButtons(String menuName, List<FormButton> buttons) {
        if (buttons == null || buttons.isEmpty()) {
            Map<String, Object> replacements = Map.of("menu", menuName);
            validationWarnings.add(messageData.getValueNoPrefix(MessageData.VALIDATION_NO_BUTTONS, replacements, null));
            return;
        }

        for (int i = 0; i < buttons.size(); i++) {
            FormButton button = buttons.get(i);
            if (button == null) {
                Map<String, Object> replacements = Map.of("button", i, "menu", menuName);
                validationErrors.add(messageData.getValueNoPrefix(MessageData.VALIDATION_BUTTON_NULL, replacements, null));
                continue;
            }


            if (ValidationUtils.isNullOrEmpty(button.getText())) {
                Map<String, Object> replacements = Map.of("button", i, "menu", menuName);
                validationWarnings.add(messageData.getValueNoPrefix(MessageData.VALIDATION_BUTTON_NO_TEXT, replacements, null));
            }


            validateButtonActions(menuName, i, button);


            if (button instanceof ConditionalButton) {
                validateConditionalButton(menuName, i, (ConditionalButton) button);
            }


            if (button.getImage() != null && !ValidationUtils.isValidImageSource(button.getImage())) {
                String buttonText = button.getText();
                Object buttonLabel = (ValidationUtils.isNullOrEmpty(buttonText)) ? i : buttonText;
                Map<String, Object> replacements = Map.of("button", buttonLabel, "menu", menuName);
                validationWarnings.add(messageData.getValueNoPrefix(MessageData.VALIDATION_BUTTON_INVALID_IMAGE, replacements, null));
            }
        }
    }


    private void validateButtonActions(String menuName, int buttonIndex, FormButton button) {

        ActionSystem.ActionDefinition actionDef = null;

        if (button.hasActions()) {

            actionDef = button.getPrimaryAction();
        } else if (button instanceof ConditionalButton) {

            ConditionalButton conditionalButton = (ConditionalButton) button;
            actionDef = conditionalButton.getActionDefinition();
        }

        if (actionDef == null) {
            Map<String, Object> replacements = Map.of("button", buttonIndex, "menu", menuName);
            validationWarnings.add(messageData.getValueNoPrefix(MessageData.VALIDATION_BUTTON_NO_ACTION, replacements, null));
            return;
        }


        validateActionDefinition(menuName, buttonIndex, actionDef);


        if (button.hasActions()) {
            for (ActionSystem.ActionDefinition action : button.getAllActions()) {
                validateActionDefinition(menuName, buttonIndex, action);
            }
        }
    }


    private void validateActionDefinition(String menuName, int buttonIndex, ActionSystem.ActionDefinition actionDef) {
        if (actionDef == null) {
            return;
        }


        if (!ActionParser.isValid(actionDef)) {
            Map<String, Object> replacements = Map.of("menu", menuName, "button", buttonIndex);
            validationErrors.add(messageData.getValueNoPrefix(MessageData.VALIDATION_INVALID_ACTION_FORMAT, replacements, null));
            return;
        }


        if (actionDef.isConditional()) {
            return;
        }


        if (!actionDef.isEmpty()) {
            for (String actionType : actionDef.getActionTypes()) {
                Object actionValue = actionDef.getAction(actionType);
                String actionValueStr = actionValue != null ? actionValue.toString() : "";

                checkFormatWarnings(menuName, buttonIndex, actionType, actionValueStr);


                if (actionValueStr.contains(":") && !actionValueStr.contains("{") && !actionValueStr.contains("}")) {
                    Map<String, Object> replacements = Map.of("menu", menuName, "index", buttonIndex, "action", actionType);
                    validationErrors.add(messageData.getValueNoPrefix(MessageData.VALIDATION_LEGACY_FORMAT_DETECTED, replacements, null));
                    continue;
                }


                if (!actionRegistry.getRegisteredActionTypes().contains(actionType.toLowerCase())) {
                    Map<String, Object> replacements = Map.of("type", actionType, "menu", menuName, "index", buttonIndex);
                    validationErrors.add(messageData.getValueNoPrefix(MessageData.VALIDATION_UNKNOWN_ACTION_TYPE, replacements, null));
                }


                validateActionTypeSpecific(menuName, buttonIndex, actionType, actionValue);
            }
        }


        if (actionDef.hasAction("delay")) {
            Object delayValue = actionDef.getAction("delay");
            String delayStr = delayValue != null ? delayValue.toString() : "";
            ActionSystem.ActionHandler handler = actionRegistry.getHandler("delay");
            if (handler != null) {
                if (!handler.isValidAction(delayStr)) {
                    validationErrors.add("Invalid delay value '" + delayStr + "' in menu '" + menuName + "' button " + buttonIndex);
                }
            }
        }
    }

    private void checkFormatWarnings(String menuName, int buttonIndex, String actionType, String block) {
        if (ValidationUtils.isNullOrEmpty(block)) {
            return;
        }

        String trimmed = block.trim();
        int opens = 0, closes = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{') opens++;
            else if (c == '}') closes++;
        }
        if (opens != closes) {
            if (opens > closes) {
                validationWarnings.add("Action '" + actionType + "' in menu '" + menuName + "' button " + buttonIndex + " may be missing closing '}'");
            } else {
                validationWarnings.add("Action '" + actionType + "' in menu '" + menuName + "' button " + buttonIndex + " may be missing opening '{'");
            }
        }
    }


    private void validateConditionalButton(String menuName, int buttonIndex, ConditionalButton button) {

        if (button.hasShowCondition()) {
            validateActionCondition(menuName, buttonIndex, button.getShowCondition());
        }


        if (button.getPriorityCondition() != null && !button.getPriorityCondition().trim().isEmpty()) {
            validateActionCondition(menuName, buttonIndex, button.getPriorityCondition());
        }


        if (button.getAlternativeActionDefinition() != null) {
            validateActionDefinition(menuName, buttonIndex, button.getAlternativeActionDefinition());
        }


        for (Map.Entry<String, ActionSystem.ActionDefinition> entry : button.getConditionalActions().entrySet()) {
            validateActionCondition(menuName, buttonIndex, entry.getKey());
            validateActionDefinition(menuName, buttonIndex, entry.getValue());
        }


        int priority = button.getPriority();
        if (priority < -1000 || priority > 1000) {
            validationWarnings.add("Priority value " + priority + " may be out of reasonable range in menu '" + menuName + "' button " + buttonIndex);
        }
    }


    private void validateActionCondition(String menuName, int buttonIndex, String condition) {
        if (ValidationUtils.isNullOrEmpty(condition)) {
            return;
        }


        if (condition.contains(":")) {
            String[] parts = condition.split(":", 2);
            String conditionType = parts[0].trim();
            String conditionValue = parts[1].trim();


            switch (conditionType.toLowerCase()) {
                case "permission":
                    if (ValidationUtils.isNullOrEmpty(conditionValue)) {
                        validationErrors.add("Empty permission condition in menu '" + menuName + "' button " + buttonIndex);
                    }
                    break;
                case "placeholder":
                    if (!conditionValue.contains("=")) {
                        validationWarnings.add("Placeholder condition may be missing comparison operator in menu '" + menuName + "' button " + buttonIndex);
                    }
                    break;
                case "economy":

                    break;
                default:
                    validationWarnings.add("Unknown condition type '" + conditionType + "' in menu '" + menuName + "' button " + buttonIndex);
                    break;
            }
        }
    }


    private void validateActionTypeSpecific(String menuName, int buttonIndex, String actionType, Object actionValue) {

        String actualActionType = actionType;
        String actualActionValue = actionValue != null ? actionValue.toString() : null;


        if (actualActionValue != null && actualActionValue.contains("{") && actualActionValue.contains("}")) {

            return;
        }


        if (actualActionValue != null && !actualActionValue.contains("{")) {

            return;
        }


    }


    private void validateFormTypeSpecific(String menuName, FormMenu formMenu) {
        String formType = formMenu.getFormType();
        if (formType == null) return;

        switch (formType.toLowerCase()) {
            case "modal":
                if (formMenu.getFormButtons().size() != 2) {
                    validationErrors.add("Modal form '" + menuName + "' must have exactly 2 buttons");
                }
                break;

            case "custom":

                if (formMenu.getFormButtons().isEmpty()) {
                    validationWarnings.add("Custom form '" + menuName + "' has no input elements");
                }
                break;
        }
    }


    private void validateCommand(String menuName, String command) {
        if (ValidationUtils.isNullOrEmpty(command)) {
            return;
        }

        if (command.startsWith("/")) {
            validationWarnings.add("Command in menu '" + menuName + "' should not start with '/'");
        }

        if (!IDENTIFIER_PATTERN.matcher(command.split(" ")[0]).matches()) {
            validationErrors.add("Invalid command format in menu '" + menuName + "': '" + command + "'");
        }
    }


    private void checkCircularReferences(Map<String, FormMenu> formMenus) {
        for (String menuName : formMenus.keySet()) {
            Set<String> visited = new HashSet<>();
            if (hasCircularReference(menuName, formMenus, visited)) {
                validationWarnings.add("Potential circular reference detected starting from menu '" + menuName + "'");
            }
        }
    }


    private boolean hasCircularReference(String menuName, Map<String, FormMenu> formMenus, Set<String> visited) {
        if (visited.contains(menuName)) {
            return true;
        }

        visited.add(menuName);
        FormMenu menu = formMenus.get(menuName);
        if (menu == null) {
            return false;
        }

        for (FormButton button : menu.getFormButtons()) {


            if (button instanceof ConditionalButton) {
                ConditionalButton conditionalButton = (ConditionalButton) button;


                if (checkActionForCircularReference(conditionalButton.getActionDefinition(), formMenus, visited)) {
                    return true;
                }


                if (checkActionForCircularReference(conditionalButton.getAlternativeActionDefinition(), formMenus, visited)) {
                    return true;
                }


                for (ActionSystem.ActionDefinition conditionalAction : conditionalButton.getConditionalActions().values()) {
                    if (checkActionForCircularReference(conditionalAction, formMenus, visited)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    private boolean checkActionForCircularReference(ActionSystem.ActionDefinition actionDef, Map<String, FormMenu> formMenus, Set<String> visited) {
        if (actionDef == null) {
            return false;
        }


        for (String actionType : actionDef.getActionTypes()) {
            if (actionType.equalsIgnoreCase("openform") || actionType.equalsIgnoreCase("open_form")) {
                Object targetMenuObj = actionDef.getAction(actionType);
                if (targetMenuObj != null) {
                    String targetMenu = targetMenuObj.toString();
                    if (hasCircularReference(targetMenu, formMenus, new HashSet<>(visited))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    public static class ValidationResult {
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(List<String> errors, List<String> warnings) {
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public boolean isValid() {
            return !hasErrors();
        }
    }
}

