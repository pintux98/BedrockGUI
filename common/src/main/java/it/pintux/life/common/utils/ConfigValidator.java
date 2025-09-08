package it.pintux.life.common.utils;

import it.pintux.life.common.form.obj.FormMenu;
import it.pintux.life.common.form.obj.FormButton;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Comprehensive configuration validator for BedrockGUI
 */
public class ConfigValidator {
    
    private static final Logger logger = Logger.getLogger(ConfigValidator.class);
    
    // Valid form types
    private static final Set<String> VALID_FORM_TYPES = Set.of("simple", "modal", "custom");
    
    // Valid action types
    private static final Set<String> VALID_ACTION_TYPES = Set.of(
        "command", "server", "message", "broadcast", "close", "delay", "sound", 
        "economy", "title", "actionbar", "permission", "potion", "gamemode", 
        "health", "teleport", "inventory", "conditional", "random", "openform", "openurl"
    );
    
    // Pattern for valid identifiers
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");
    
    private final List<String> validationErrors = new ArrayList<>();
    private final List<String> validationWarnings = new ArrayList<>();
    
    /**
     * Validates all form configurations
     * @param formMenus Map of form menus to validate
     * @return ValidationResult containing errors and warnings
     */
    public ValidationResult validateConfiguration(Map<String, FormMenu> formMenus) {
        validationErrors.clear();
        validationWarnings.clear();
        
        if (formMenus == null || formMenus.isEmpty()) {
            validationErrors.add("No form menus configured");
            return new ValidationResult(validationErrors, validationWarnings);
        }
        
        // Validate each form menu
        for (Map.Entry<String, FormMenu> entry : formMenus.entrySet()) {
            validateFormMenu(entry.getKey(), entry.getValue());
        }
        
        // Check for circular references
        checkCircularReferences(formMenus);
        
        return new ValidationResult(validationErrors, validationWarnings);
    }
    
    /**
     * Validates a single form menu
     */
    private void validateFormMenu(String menuName, FormMenu formMenu) {
        if (formMenu == null) {
            validationErrors.add("Form menu '" + menuName + "' is null");
            return;
        }
        
        // Validate menu name
        if (!ValidationUtils.isValidMenuName(menuName)) {
            validationErrors.add("Invalid menu name: '" + menuName + "'");
        }
        
        // Validate form type
        String formType = formMenu.getFormType();
        if (formType == null || !VALID_FORM_TYPES.contains(formType.toLowerCase())) {
            validationErrors.add("Invalid form type '" + formType + "' in menu '" + menuName + "'");
        }
        
        // Validate title
        if (ValidationUtils.isNullOrEmpty(formMenu.getFormTitle())) {
            validationWarnings.add("Form menu '" + menuName + "' has no title");
        }
        
        // Validate buttons
        validateButtons(menuName, formMenu.getFormButtons());
        
        // Validate form-specific requirements
        validateFormTypeSpecific(menuName, formMenu);
        
        // Validate command if present
        if (formMenu.getFormCommand() != null) {
            validateCommand(menuName, formMenu.getFormCommand());
        }
    }
    
    /**
     * Validates form buttons
     */
    private void validateButtons(String menuName, List<FormButton> buttons) {
        if (buttons == null || buttons.isEmpty()) {
            validationWarnings.add("Form menu '" + menuName + "' has no buttons");
            return;
        }
        
        for (int i = 0; i < buttons.size(); i++) {
            FormButton button = buttons.get(i);
            if (button == null) {
                validationErrors.add("Button " + i + " in menu '" + menuName + "' is null");
                continue;
            }
            
            // Validate button text
            if (ValidationUtils.isNullOrEmpty(button.getText())) {
                validationWarnings.add("Button " + i + " in menu '" + menuName + "' has no text");
            }
            
            // Validate button actions
            validateButtonActions(menuName, i, button.getOnClick());
            
            // Validate image if present
            if (button.getImage() != null && !ValidationUtils.isValidImageSource(button.getImage())) {
                validationWarnings.add("Button " + i + " in menu '" + menuName + "' has invalid image source");
            }
        }
    }
    
    /**
     * Validates button actions
     */
    private void validateButtonActions(String menuName, int buttonIndex, String onClick) {
        if (ValidationUtils.isNullOrEmpty(onClick)) {
            validationWarnings.add("Button " + buttonIndex + " in menu '" + menuName + "' has no onClick action");
            return;
        }
        
        // Handle multiple actions separated by ;
        String[] actions = onClick.split(";");
        for (String action : actions) {
            validateSingleAction(menuName, buttonIndex, action.trim());
        }
    }
    
    /**
     * Validates a single action
     */
    private void validateSingleAction(String menuName, int buttonIndex, String action) {
        if (ValidationUtils.isNullOrEmpty(action)) {
            return;
        }
        
        String[] parts = action.split(":", 2);
        if (parts.length != 2) {
            validationErrors.add("Invalid action format in menu '" + menuName + "' button " + buttonIndex + ": '" + action + "'");
            return;
        }
        
        String actionType = parts[0].trim().toLowerCase();
        String actionValue = parts[1].trim();
        
        // Validate action type
        if (!VALID_ACTION_TYPES.contains(actionType)) {
            validationErrors.add("Unknown action type '" + actionType + "' in menu '" + menuName + "' button " + buttonIndex);
        }
        
        // Validate action value
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            validationErrors.add("Empty action value for '" + actionType + "' in menu '" + menuName + "' button " + buttonIndex);
        }
        
        // Validate specific action types
        validateActionTypeSpecific(menuName, buttonIndex, actionType, actionValue);
    }
    
    /**
     * Validates specific action type requirements
     */
    private void validateActionTypeSpecific(String menuName, int buttonIndex, String actionType, String actionValue) {
        switch (actionType) {
            case "delay":
                try {
                    int delay = Integer.parseInt(actionValue.split(":")[0]);
                    if (delay < 0 || delay > 300000) { // Max 5 minutes
                        validationWarnings.add("Delay value " + delay + "ms may be too large in menu '" + menuName + "' button " + buttonIndex);
                    }
                } catch (NumberFormatException e) {
                    validationErrors.add("Invalid delay format in menu '" + menuName + "' button " + buttonIndex + ": '" + actionValue + "'");
                }
                break;
                
            case "openform":
                String targetMenu = actionValue.split(":")[0];
                if (!IDENTIFIER_PATTERN.matcher(targetMenu).matches()) {
                    validationErrors.add("Invalid target menu name '" + targetMenu + "' in menu '" + menuName + "' button " + buttonIndex);
                }
                break;
                
            case "economy":
                String[] economyParts = actionValue.split(":");
                if (economyParts.length < 2) {
                    validationErrors.add("Invalid economy action format in menu '" + menuName + "' button " + buttonIndex);
                } else {
                    try {
                        double amount = Double.parseDouble(economyParts[1]);
                        if (amount < 0) {
                            validationWarnings.add("Negative economy amount in menu '" + menuName + "' button " + buttonIndex);
                        }
                    } catch (NumberFormatException e) {
                        validationErrors.add("Invalid economy amount in menu '" + menuName + "' button " + buttonIndex + ": '" + economyParts[1] + "'");
                    }
                }
                break;
        }
    }
    
    /**
     * Validates form type specific requirements
     */
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
                // Custom forms should have input validation
                if (formMenu.getFormButtons().isEmpty()) {
                    validationWarnings.add("Custom form '" + menuName + "' has no input elements");
                }
                break;
        }
    }
    
    /**
     * Validates command format
     */
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
    
    /**
     * Checks for circular references in form navigation
     */
    private void checkCircularReferences(Map<String, FormMenu> formMenus) {
        for (String menuName : formMenus.keySet()) {
            Set<String> visited = new HashSet<>();
            if (hasCircularReference(menuName, formMenus, visited)) {
                validationWarnings.add("Potential circular reference detected starting from menu '" + menuName + "'");
            }
        }
    }
    
    /**
     * Recursively checks for circular references
     */
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
            if (button.getOnClick() != null) {
                String[] actions = button.getOnClick().split(";");
                for (String action : actions) {
                    if (action.trim().startsWith("openform:")) {
                        String targetMenu = action.trim().substring(9).split(":")[0];
                        if (hasCircularReference(targetMenu, formMenus, new HashSet<>(visited))) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Result of configuration validation
     */
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