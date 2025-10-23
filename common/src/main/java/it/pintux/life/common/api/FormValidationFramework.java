package it.pintux.life.common.api;

import it.pintux.life.common.api.BedrockGUIApi.*;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;


public class FormValidationFramework {
    
    private static final Logger logger = Logger.getLogger(FormValidationFramework.class);
    
    private final Map<String, FormValidator> validators = new ConcurrentHashMap<>();
    private final Map<String, ValidationRule> globalRules = new ConcurrentHashMap<>();
    
    public FormValidationFramework() {
        registerBuiltInValidators();
        registerBuiltInRules();
    }
    
    
    private void registerBuiltInValidators() {
        
        registerValidator("required", new RequiredValidator());
        registerValidator("minLength", new MinLengthValidator());
        registerValidator("maxLength", new MaxLengthValidator());
        registerValidator("pattern", new PatternValidator());
        registerValidator("email", new EmailValidator());
        registerValidator("numeric", new NumericValidator());
        registerValidator("range", new RangeValidator());
        registerValidator("playerName", new PlayerNameValidator());
        registerValidator("permission", new PermissionValidator());
        registerValidator("economy", new EconomyValidator());
        
        
        registerValidator("formStructure", new FormStructureValidator());
        registerValidator("buttonCount", new ButtonCountValidator());
        registerValidator("componentCount", new ComponentCountValidator());
        
        logger.info("Registered " + validators.size() + " built-in validators");
    }
    
    
    private void registerBuiltInRules() {
        
        registerRule("notEmpty", value -> !ValidationUtils.isNullOrEmpty(value.toString()));
        registerRule("positive", value -> {
            try {
                return Double.parseDouble(value.toString()) > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        registerRule("nonNegative", value -> {
            try {
                return Double.parseDouble(value.toString()) >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        registerRule("alphanumeric", value -> value.toString().matches("^[a-zA-Z0-9]+$"));
        registerRule("noSpaces", value -> !value.toString().contains(" "));
        
        logger.info("Registered " + globalRules.size() + " built-in validation rules");
    }
    
    
    public void registerValidator(String name, FormValidator validator) {
        validators.put(name.toLowerCase(), validator);
    }
    
    
    public void registerRule(String name, Predicate<Object> rule) {
        globalRules.put(name.toLowerCase(), new ValidationRule(name, rule));
    }
    
    
    public ValidationResult validate(String validatorName, Object value, Map<String, Object> parameters) {
        FormValidator validator = validators.get(validatorName.toLowerCase());
        if (validator == null) {
            return ValidationResult.failure("Unknown validator: " + validatorName);
        }
        
        
        if (validator instanceof RequiredValidator) {
            return ((RequiredValidator) validator).validate(value, parameters != null ? parameters : new HashMap<>());
        } else if (validator instanceof MinLengthValidator) {
            return ((MinLengthValidator) validator).validate(value, parameters != null ? parameters : new HashMap<>());
        } else if (validator instanceof MaxLengthValidator) {
            return ((MaxLengthValidator) validator).validate(value, parameters != null ? parameters : new HashMap<>());
        } else if (validator instanceof PatternValidator) {
            return ((PatternValidator) validator).validate(value, parameters != null ? parameters : new HashMap<>());
        } else if (validator instanceof EmailValidator) {
            return ((EmailValidator) validator).validate(value, parameters != null ? parameters : new HashMap<>());
        } else if (validator instanceof NumericValidator) {
            return ((NumericValidator) validator).validate(value, parameters != null ? parameters : new HashMap<>());
        } else if (validator instanceof RangeValidator) {
            return ((RangeValidator) validator).validate(value, parameters != null ? parameters : new HashMap<>());
        } else if (validator instanceof PlayerNameValidator) {
            return ((PlayerNameValidator) validator).validate(value, parameters != null ? parameters : new HashMap<>());
        } else if (validator instanceof PermissionValidator) {
            return ((PermissionValidator) validator).validate(value, parameters != null ? parameters : new HashMap<>());
        } else if (validator instanceof EconomyValidator) {
            return ((EconomyValidator) validator).validate(value, parameters != null ? parameters : new HashMap<>());
        }
        
        
        return validator.validate(new HashMap<>(), null);
    }
    
    
    public ValidationResult validateMultiple(Object value, List<ValidatorConfig> validatorConfigs) {
        for (ValidatorConfig config : validatorConfigs) {
            ValidationResult result = validate(config.validatorName, value, config.parameters);
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.success();
    }
    
    
    public ValidationResult validateForm(FormBuilder formBuilder) {
        if (formBuilder == null) {
            return ValidationResult.failure("Form builder cannot be null");
        }
        
        FormValidator structureValidator = validators.get("formstructure");
        if (structureValidator != null && structureValidator instanceof FormStructureValidator) {
            return ((FormStructureValidator) structureValidator).validate(formBuilder, new HashMap<>());
        }
        
        return ValidationResult.success();
    }
    
    
    public ValidationChain createValidationChain() {
        return new ValidationChain(this);
    }
    
    
    
    
    public static class RequiredValidator implements FormValidator {
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "required";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (value == null || ValidationUtils.isNullOrEmpty(value.toString())) {
                String fieldName = (String) parameters.getOrDefault("fieldName", "Field");
                return ValidationResult.failure(fieldName + " is required");
            }
            return ValidationResult.success();
        }
    }
    
    
    public static class MinLengthValidator implements FormValidator {
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "minLength";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (value == null) {
                return ValidationResult.failure("Value cannot be null");
            }
            
            int minLength = (Integer) parameters.getOrDefault("minLength", 1);
            String stringValue = value.toString();
            
            if (stringValue.length() < minLength) {
                return ValidationResult.failure("Minimum length is " + minLength + " characters");
            }
            
            return ValidationResult.success();
        }
    }
    
    
    public static class MaxLengthValidator implements FormValidator {
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "maxLength";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (value == null) {
                return ValidationResult.success(); 
            }
            
            int maxLength = (Integer) parameters.getOrDefault("maxLength", 255);
            String stringValue = value.toString();
            
            if (stringValue.length() > maxLength) {
                return ValidationResult.failure("Maximum length is " + maxLength + " characters");
            }
            
            return ValidationResult.success();
        }
    }
    
    
    public static class PatternValidator implements FormValidator {
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "pattern";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (value == null) {
                return ValidationResult.success(); 
            }
            
            String pattern = (String) parameters.get("pattern");
            if (pattern == null) {
                return ValidationResult.failure("Pattern parameter is required");
            }
            
            try {
                if (!Pattern.matches(pattern, value.toString())) {
                    String message = (String) parameters.getOrDefault("message", "Value does not match required pattern");
                    return ValidationResult.failure(message);
                }
            } catch (Exception e) {
                return ValidationResult.failure("Invalid pattern: " + e.getMessage());
            }
            
            return ValidationResult.success();
        }
    }
    
    
    public static class EmailValidator implements FormValidator {
        private static final String EMAIL_PATTERN = 
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "email";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (value == null || ValidationUtils.isNullOrEmpty(value.toString())) {
                return ValidationResult.success(); 
            }
            
            if (!Pattern.matches(EMAIL_PATTERN, value.toString())) {
                return ValidationResult.failure("Invalid email format");
            }
            
            return ValidationResult.success();
        }
    }
    
    
    public static class NumericValidator implements FormValidator {
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "numeric";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (value == null) {
                return ValidationResult.success();
            }
            
            try {
                Double.parseDouble(value.toString());
                return ValidationResult.success();
            } catch (NumberFormatException e) {
                return ValidationResult.failure("Value must be a valid number");
            }
        }
    }
    
    
    public static class RangeValidator implements FormValidator {
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "range";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (value == null) {
                return ValidationResult.success();
            }
            
            try {
                double numValue = Double.parseDouble(value.toString());
                Double min = (Double) parameters.get("min");
                Double max = (Double) parameters.get("max");
                
                if (min != null && numValue < min) {
                    return ValidationResult.failure("Value must be at least " + min);
                }
                
                if (max != null && numValue > max) {
                    return ValidationResult.failure("Value must be at most " + max);
                }
                
                return ValidationResult.success();
            } catch (NumberFormatException e) {
                return ValidationResult.failure("Value must be a valid number");
            }
        }
    }
    
    
    public static class PlayerNameValidator implements FormValidator {
        private static final String PLAYER_NAME_PATTERN = "^[a-zA-Z0-9_]{3,16}$";
        
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "playerName";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (value == null || ValidationUtils.isNullOrEmpty(value.toString())) {
                return ValidationResult.failure("Player name cannot be empty");
            }
            
            String playerName = value.toString();
            
            if (!Pattern.matches(PLAYER_NAME_PATTERN, playerName)) {
                return ValidationResult.failure("Invalid player name format (3-16 characters, alphanumeric and underscore only)");
            }
            
            return ValidationResult.success();
        }
    }
    
    
    public static class PermissionValidator implements FormValidator {
        private static final String PERMISSION_PATTERN = "^[a-zA-Z0-9._-]+$";
        
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "permission";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (value == null || ValidationUtils.isNullOrEmpty(value.toString())) {
                return ValidationResult.failure("Permission cannot be empty");
            }
            
            String permission = value.toString();
            
            if (!Pattern.matches(PERMISSION_PATTERN, permission)) {
                return ValidationResult.failure("Invalid permission format");
            }
            
            return ValidationResult.success();
        }
    }
    
    
    public static class EconomyValidator implements FormValidator {
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "economy";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (value == null) {
                return ValidationResult.success();
            }
            
            try {
                double amount = Double.parseDouble(value.toString());
                
                Boolean allowNegative = (Boolean) parameters.getOrDefault("allowNegative", false);
                if (!allowNegative && amount < 0) {
                    return ValidationResult.failure("Amount cannot be negative");
                }
                
                Double maxAmount = (Double) parameters.get("maxAmount");
                if (maxAmount != null && amount > maxAmount) {
                    return ValidationResult.failure("Amount cannot exceed " + maxAmount);
                }
                
                
                Integer maxDecimals = (Integer) parameters.getOrDefault("maxDecimals", 2);
                String[] parts = value.toString().split("\\.");
                if (parts.length > 1 && parts[1].length() > maxDecimals) {
                    return ValidationResult.failure("Maximum " + maxDecimals + " decimal places allowed");
                }
                
                return ValidationResult.success();
            } catch (NumberFormatException e) {
                return ValidationResult.failure("Invalid monetary amount");
            }
        }
    }
    
    
    public static class FormStructureValidator implements FormValidator {
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "formStructure";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (!(value instanceof FormBuilder)) {
                return ValidationResult.failure("Value must be a FormBuilder instance");
            }
            
            FormBuilder builder = (FormBuilder) value;
            
            
            if (ValidationUtils.isNullOrEmpty(builder.getTitle())) {
                return ValidationResult.failure("Form title cannot be empty");
            }
            
            
            
            return ValidationResult.success();
        }
    }
    
    
    public static class ButtonCountValidator implements FormValidator {
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "buttonCount";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (!(value instanceof SimpleFormBuilder)) {
                return ValidationResult.success(); 
            }
            
            SimpleFormBuilder builder = (SimpleFormBuilder) value;
            int buttonCount;
            try {
                
                try {
                    java.lang.reflect.Method m = builder.getClass().getMethod("getButtonCount");
                    Object result = m.invoke(builder);
                    buttonCount = (result instanceof Number) ? ((Number) result).intValue() : 0;
                } catch (NoSuchMethodException nsme) {
                    
                    java.lang.reflect.Field field = builder.getClass().getDeclaredField("buttons");
                    field.setAccessible(true);
                    Object val = field.get(builder);
                    if (val instanceof java.util.Collection) {
                        buttonCount = ((java.util.Collection<?>) val).size();
                    } else {
                        buttonCount = 0;
                    }
                }
            } catch (Exception ex) {
                buttonCount = 0;
            }
            
            Integer minButtons = (Integer) parameters.get("minButtons");
            Integer maxButtons = (Integer) parameters.get("maxButtons");
            
            if (minButtons != null && buttonCount < minButtons) {
                return ValidationResult.failure("Form must have at least " + minButtons + " buttons");
            }
            
            if (maxButtons != null && buttonCount > maxButtons) {
                return ValidationResult.failure("Form cannot have more than " + maxButtons + " buttons");
            }
            
            return ValidationResult.success();
        }
    }
    
    
    public static class ComponentCountValidator implements FormValidator {
        @Override
        public ValidationResult validate(Map<String, Object> formData, FormPlayer player) {
            
            return ValidationResult.success();
        }
        
        @Override
        public String getValidatorName() {
            return "componentCount";
        }
        
        public ValidationResult validate(Object value, Map<String, Object> parameters) {
            if (!(value instanceof CustomFormBuilder)) {
                return ValidationResult.success(); 
            }
            
            CustomFormBuilder builder = (CustomFormBuilder) value;
            int componentCount;
            try {
                
                try {
                    java.lang.reflect.Method m = builder.getClass().getMethod("getComponentCount");
                    Object result = m.invoke(builder);
                    componentCount = (result instanceof Number) ? ((Number) result).intValue() : 0;
                } catch (NoSuchMethodException nsme) {
                    
                    java.lang.reflect.Field field = builder.getClass().getDeclaredField("components");
                    field.setAccessible(true);
                    Object val = field.get(builder);
                    if (val instanceof java.util.Collection) {
                        componentCount = ((java.util.Collection<?>) val).size();
                    } else {
                        componentCount = 0;
                    }
                }
            } catch (Exception ex) {
                componentCount = 0;
            }
            
            Integer minComponents = (Integer) parameters.get("minComponents");
            Integer maxComponents = (Integer) parameters.get("maxComponents");
            
            if (minComponents != null && componentCount < minComponents) {
                return ValidationResult.failure("Form must have at least " + minComponents + " components");
            }
            
            if (maxComponents != null && componentCount > maxComponents) {
                return ValidationResult.failure("Form cannot have more than " + maxComponents + " components");
            }
            
            return ValidationResult.success();
        }
    }
    
    
    
    
    public static class ValidationChain {
        private final FormValidationFramework framework;
        private final List<ValidationStep> steps = new ArrayList<>();
        
        public ValidationChain(FormValidationFramework framework) {
            this.framework = framework;
        }
        
        public ValidationChain addValidator(String validatorName, Map<String, Object> parameters) {
            steps.add(new ValidationStep(validatorName, parameters));
            return this;
        }
        
        public ValidationChain addValidator(String validatorName) {
            return addValidator(validatorName, new HashMap<>());
        }
        
        public ValidationChain addRule(String ruleName) {
            steps.add(new ValidationStep(ruleName, new HashMap<>(), true));
            return this;
        }
        
        public ValidationChain addCustom(Function<Object, ValidationResult> customValidator) {
            steps.add(new ValidationStep(customValidator));
            return this;
        }
        
        public ValidationResult validate(Object value) {
            for (ValidationStep step : steps) {
                ValidationResult result = step.execute(framework, value);
                if (!result.isValid()) {
                    return result;
                }
            }
            return ValidationResult.success();
        }
        
        private static class ValidationStep {
            private final String validatorName;
            private final Map<String, Object> parameters;
            private final boolean isRule;
            private final Function<Object, ValidationResult> customValidator;
            
            public ValidationStep(String validatorName, Map<String, Object> parameters) {
                this(validatorName, parameters, false);
            }
            
            public ValidationStep(String validatorName, Map<String, Object> parameters, boolean isRule) {
                this.validatorName = validatorName;
                this.parameters = parameters;
                this.isRule = isRule;
                this.customValidator = null;
            }
            
            public ValidationStep(Function<Object, ValidationResult> customValidator) {
                this.validatorName = null;
                this.parameters = null;
                this.isRule = false;
                this.customValidator = customValidator;
            }
            
            public ValidationResult execute(FormValidationFramework framework, Object value) {
                if (customValidator != null) {
                    return customValidator.apply(value);
                }
                
                if (isRule) {
                    ValidationRule rule = framework.globalRules.get(validatorName.toLowerCase());
                    if (rule != null) {
                        return rule.test(value) ? ValidationResult.success() : 
                               ValidationResult.failure("Rule '" + validatorName + "' failed");
                    }
                    return ValidationResult.failure("Unknown rule: " + validatorName);
                }
                
                return framework.validate(validatorName, value, parameters);
            }
        }
    }
    
    
    
    
    public static class ValidatorConfig {
        public final String validatorName;
        public final Map<String, Object> parameters;
        
        public ValidatorConfig(String validatorName, Map<String, Object> parameters) {
            this.validatorName = validatorName;
            this.parameters = parameters;
        }
        
        public ValidatorConfig(String validatorName) {
            this(validatorName, new HashMap<>());
        }
    }
    
    
    public static class ValidationRule {
        private final String name;
        private final Predicate<Object> rule;
        
        public ValidationRule(String name, Predicate<Object> rule) {
            this.name = name;
            this.rule = rule;
        }
        
        public boolean test(Object value) {
            return rule.test(value);
        }
        
        public String getName() {
            return name;
        }
    }
    
    
    
    
    public static ValidatorConfig required() {
        return new ValidatorConfig("required");
    }
    
    public static ValidatorConfig minLength(int length) {
        Map<String, Object> params = new HashMap<>();
        params.put("minLength", length);
        return new ValidatorConfig("minLength", params);
    }
    
    public static ValidatorConfig maxLength(int length) {
        Map<String, Object> params = new HashMap<>();
        params.put("maxLength", length);
        return new ValidatorConfig("maxLength", params);
    }
    
    public static ValidatorConfig pattern(String regex, String message) {
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", regex);
        params.put("message", message);
        return new ValidatorConfig("pattern", params);
    }
    
    public static ValidatorConfig range(double min, double max) {
        Map<String, Object> params = new HashMap<>();
        params.put("min", min);
        params.put("max", max);
        return new ValidatorConfig("range", params);
    }
    
    public static ValidatorConfig economy(boolean allowNegative, Double maxAmount) {
        Map<String, Object> params = new HashMap<>();
        params.put("allowNegative", allowNegative);
        if (maxAmount != null) {
            params.put("maxAmount", maxAmount);
        }
        return new ValidatorConfig("economy", params);
    }
}
