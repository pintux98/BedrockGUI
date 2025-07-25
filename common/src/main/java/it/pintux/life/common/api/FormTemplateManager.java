package it.pintux.life.common.api;

import it.pintux.life.common.api.BedrockGUIApi.*;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Advanced template management system for creating reusable form structures
 */
public class FormTemplateManager {
    
    private static final Logger logger = Logger.getLogger(FormTemplateManager.class);
    
    private final BedrockGUIApi api;
    private final Map<String, FormTemplate> templates = new ConcurrentHashMap<>();
    private final Map<String, TemplateValidator> validators = new ConcurrentHashMap<>();
    
    public FormTemplateManager(BedrockGUIApi api) {
        this.api = api;
        registerBuiltInTemplates();
    }
    
    /**
     * Registers built-in templates
     */
    private void registerBuiltInTemplates() {
        // Player selection template
        registerTemplate("player_selector", new PlayerSelectorTemplate());
        
        // Item selection template
        registerTemplate("item_selector", new ItemSelectorTemplate());
        
        // Permission management template
        registerTemplate("permission_manager", new PermissionManagerTemplate());
        
        // Economy transaction template
        registerTemplate("economy_transaction", new EconomyTransactionTemplate());
        
        // Configuration editor template
        registerTemplate("config_editor", new ConfigEditorTemplate());
        
        logger.info("Registered " + templates.size() + " built-in form templates");
    }
    
    /**
     * Registers a custom template
     */
    public void registerTemplate(String name, FormTemplate template) {
        if (ValidationUtils.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Template name cannot be null or empty");
        }
        
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        
        templates.put(name.toLowerCase(), template);
        logger.info("Registered form template: " + name);
    }
    
    /**
     * Gets a template by name
     */
    public FormTemplate getTemplate(String name) {
        if (ValidationUtils.isNullOrEmpty(name)) {
            return null;
        }
        return templates.get(name.toLowerCase());
    }
    
    /**
     * Creates a form from a template
     */
    public FormBuilder createFromTemplate(String templateName, Map<String, Object> parameters) {
        FormTemplate template = getTemplate(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        
        // Validate required parameters
        List<String> requiredParams = template.getRequiredParameters();
        for (String param : requiredParams) {
            if (!parameters.containsKey(param)) {
                throw new IllegalArgumentException("Missing required parameter: " + param);
            }
        }
        
        // Validate parameters if validator exists
        TemplateValidator validator = validators.get(templateName.toLowerCase());
        if (validator != null) {
            ValidationResult result = validator.validateParameters(parameters);
            if (!result.isValid()) {
                throw new IllegalArgumentException("Invalid parameters: " + result.getMessage());
            }
        }
        
        return template.createForm(parameters);
    }
    
    /**
     * Registers a template validator
     */
    public void registerValidator(String templateName, TemplateValidator validator) {
        validators.put(templateName.toLowerCase(), validator);
    }
    
    /**
     * Gets all available template names
     */
    public Set<String> getAvailableTemplates() {
        return new HashSet<>(templates.keySet());
    }
    
    // ==================== BUILT-IN TEMPLATES ====================
    
    /**
     * Player selector template
     */
    public class PlayerSelectorTemplate implements FormTemplate {
        @Override
        public FormBuilder createForm(Map<String, Object> parameters) {
            String title = (String) parameters.getOrDefault("title", "Select Player");
            @SuppressWarnings("unchecked")
            List<String> players = (List<String>) parameters.get("players");
            @SuppressWarnings("unchecked")
            Function<String, Runnable> onSelect = (Function<String, Runnable>) parameters.get("onSelect");
            
            BedrockGUIApi.SimpleFormBuilder builder = api.createSimpleForm(title);
            
            if (players != null) {
                for (String player : players) {
                    builder.button(player, formPlayer -> {
                        if (onSelect != null) {
                            Runnable action = onSelect.apply(player);
                            if (action != null) action.run();
                        }
                    });
                }
            }
            
            return builder;
        }
        
        @Override
        public String getTemplateName() {
            return "player_selector";
        }
        
        @Override
        public List<String> getRequiredParameters() {
            return Arrays.asList("players");
        }
    }
    
    /**
     * Item selector template
     */
    public class ItemSelectorTemplate implements FormTemplate {
        @Override
        public FormBuilder createForm(Map<String, Object> parameters) {
            String title = (String) parameters.getOrDefault("title", "Select Item");
            @SuppressWarnings("unchecked")
            Map<String, String> items = (Map<String, String>) parameters.get("items"); // name -> image
            @SuppressWarnings("unchecked")
            Function<String, Runnable> onSelect = (Function<String, Runnable>) parameters.get("onSelect");
            
            BedrockGUIApi.SimpleFormBuilder builder = api.createSimpleForm(title);
            
            if (items != null) {
                for (Map.Entry<String, String> entry : items.entrySet()) {
                    String itemName = entry.getKey();
                    String itemImage = entry.getValue();
                    
                    builder.button(itemName, itemImage, player -> {
                        if (onSelect != null) {
                            Runnable action = onSelect.apply(itemName);
                            if (action != null) action.run();
                        }
                    });
                }
            }
            
            return builder;
        }
        
        @Override
        public String getTemplateName() {
            return "item_selector";
        }
        
        @Override
        public List<String> getRequiredParameters() {
            return Arrays.asList("items");
        }
    }
    
    /**
     * Permission manager template
     */
    public class PermissionManagerTemplate implements FormTemplate {
        @Override
        public FormBuilder createForm(Map<String, Object> parameters) {
            String title = (String) parameters.getOrDefault("title", "Manage Permissions");
            String targetPlayer = (String) parameters.get("targetPlayer");
            @SuppressWarnings("unchecked")
            List<String> availablePermissions = (List<String>) parameters.get("permissions");
            @SuppressWarnings("unchecked")
            List<String> currentPermissions = (List<String>) parameters.getOrDefault("currentPermissions", new ArrayList<>());
            
            BedrockGUIApi.CustomFormBuilder builder = api.createCustomForm(title)
                .input("Target Player", "Enter player name", targetPlayer != null ? targetPlayer : "");
            
            if (availablePermissions != null) {
                for (String permission : availablePermissions) {
                    boolean hasPermission = currentPermissions.contains(permission);
                    builder.toggle(permission, hasPermission);
                }
            }
            
            return builder;
        }
        
        @Override
        public String getTemplateName() {
            return "permission_manager";
        }
        
        @Override
        public List<String> getRequiredParameters() {
            return Arrays.asList("permissions");
        }
    }
    
    /**
     * Economy transaction template
     */
    public class EconomyTransactionTemplate implements FormTemplate {
        @Override
        public FormBuilder createForm(Map<String, Object> parameters) {
            String title = (String) parameters.getOrDefault("title", "Economy Transaction");
            String transactionType = (String) parameters.getOrDefault("type", "transfer");
            Double maxAmount = (Double) parameters.get("maxAmount");
            Double currentBalance = (Double) parameters.get("currentBalance");
            
            BedrockGUIApi.CustomFormBuilder builder = api.createCustomForm(title);
            
            // Transaction type dropdown
            List<String> types = Arrays.asList("Give Money", "Take Money", "Transfer Money", "Set Balance");
            int defaultType = Math.max(0, types.indexOf(transactionType));
            builder.dropdown("Transaction Type", types, defaultType);
            
            // Amount input
            builder.input("Amount", "Enter amount", "0");
            
            // Target player (for transfers)
            if ("transfer".equals(transactionType)) {
                builder.input("Target Player", "Enter player name", "");
            }
            
            // Current balance display
            if (currentBalance != null) {
                builder.input("Current Balance", "Read-only", String.format("%.2f", currentBalance));
            }
            
            // Confirmation toggle
            builder.toggle("Confirm Transaction", false);
            
            return builder;
        }
        
        @Override
        public String getTemplateName() {
            return "economy_transaction";
        }
        
        @Override
        public List<String> getRequiredParameters() {
            return Arrays.asList(); // No required parameters
        }
    }
    
    /**
     * Configuration editor template
     */
    public class ConfigEditorTemplate implements FormTemplate {
        @Override
        public FormBuilder createForm(Map<String, Object> parameters) {
            String title = (String) parameters.getOrDefault("title", "Configuration Editor");
            @SuppressWarnings("unchecked")
            Map<String, Object> configValues = (Map<String, Object>) parameters.get("config");
            @SuppressWarnings("unchecked")
            Map<String, String> configTypes = (Map<String, String>) parameters.get("types");
            
            BedrockGUIApi.CustomFormBuilder builder = api.createCustomForm(title);
            
            if (configValues != null) {
                for (Map.Entry<String, Object> entry : configValues.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    String type = configTypes != null ? configTypes.get(key) : "string";
                    
                    switch (type.toLowerCase()) {
                        case "boolean":
                            builder.toggle(key, value instanceof Boolean ? (Boolean) value : false);
                            break;
                        case "integer":
                            int intValue = value instanceof Number ? ((Number) value).intValue() : 0;
                            builder.slider(key, 0, 100, 1, intValue);
                            break;
                        case "string":
                        default:
                            builder.input(key, "Enter " + key, value != null ? value.toString() : "");
                            break;
                    }
                }
            }
            
            return builder;
        }
        
        @Override
        public String getTemplateName() {
            return "config_editor";
        }
        
        @Override
        public List<String> getRequiredParameters() {
            return Arrays.asList("config");
        }
    }
    
    // ==================== TEMPLATE VALIDATION ====================
    
    /**
     * Interface for template parameter validation
     */
    public interface TemplateValidator {
        ValidationResult validateParameters(Map<String, Object> parameters);
    }
    
    /**
     * Built-in validators for common templates
     */
    public static class CommonValidators {
        
        public static TemplateValidator playerListValidator() {
            return parameters -> {
                Object players = parameters.get("players");
                if (!(players instanceof List)) {
                    return ValidationResult.failure("'players' parameter must be a List");
                }
                
                @SuppressWarnings("unchecked")
                List<String> playerList = (List<String>) players;
                if (playerList.isEmpty()) {
                    return ValidationResult.failure("Player list cannot be empty");
                }
                
                return ValidationResult.success();
            };
        }
        
        public static TemplateValidator itemMapValidator() {
            return parameters -> {
                Object items = parameters.get("items");
                if (!(items instanceof Map)) {
                    return ValidationResult.failure("'items' parameter must be a Map");
                }
                
                @SuppressWarnings("unchecked")
                Map<String, String> itemMap = (Map<String, String>) items;
                if (itemMap.isEmpty()) {
                    return ValidationResult.failure("Item map cannot be empty");
                }
                
                return ValidationResult.success();
            };
        }
        
        public static TemplateValidator economyValidator() {
            return parameters -> {
                Object maxAmount = parameters.get("maxAmount");
                if (maxAmount != null && !(maxAmount instanceof Number)) {
                    return ValidationResult.failure("'maxAmount' must be a number");
                }
                
                Object currentBalance = parameters.get("currentBalance");
                if (currentBalance != null && !(currentBalance instanceof Number)) {
                    return ValidationResult.failure("'currentBalance' must be a number");
                }
                
                return ValidationResult.success();
            };
        }
    }
    
    // ==================== ADVANCED TEMPLATE FEATURES ====================
    
    /**
     * Template with conditional logic
     */
    public abstract class ConditionalTemplate implements FormTemplate {
        protected final Map<String, Predicate<Map<String, Object>>> conditions = new HashMap<>();
        
        public ConditionalTemplate addCondition(String name, Predicate<Map<String, Object>> condition) {
            conditions.put(name, condition);
            return this;
        }
        
        protected boolean evaluateCondition(String name, Map<String, Object> parameters) {
            Predicate<Map<String, Object>> condition = conditions.get(name);
            return condition != null && condition.test(parameters);
        }
    }
    
    /**
     * Template with dynamic component generation
     */
    public abstract class DynamicTemplate implements FormTemplate {
        protected final Map<String, Function<Map<String, Object>, List<ComponentDefinition>>> componentGenerators = new HashMap<>();
        
        public DynamicTemplate addComponentGenerator(String name, Function<Map<String, Object>, List<ComponentDefinition>> generator) {
            componentGenerators.put(name, generator);
            return this;
        }
        
        protected List<ComponentDefinition> generateComponents(String generatorName, Map<String, Object> parameters) {
            Function<Map<String, Object>, List<ComponentDefinition>> generator = componentGenerators.get(generatorName);
            return generator != null ? generator.apply(parameters) : new ArrayList<>();
        }
        
        public static class ComponentDefinition {
            public final String type;
            public final String name;
            public final Map<String, Object> properties;
            
            public ComponentDefinition(String type, String name, Map<String, Object> properties) {
                this.type = type;
                this.name = name;
                this.properties = properties;
            }
        }
    }
    
    /**
     * Template with localization support
     */
    public abstract class LocalizedTemplate implements FormTemplate {
        protected final Map<String, Map<String, String>> localizations = new HashMap<>();
        
        public LocalizedTemplate addLocalization(String locale, Map<String, String> translations) {
            localizations.put(locale, translations);
            return this;
        }
        
        protected String getLocalizedText(String key, String locale, String defaultText) {
            Map<String, String> translations = localizations.get(locale);
            if (translations != null && translations.containsKey(key)) {
                return translations.get(key);
            }
            return defaultText;
        }
    }
}