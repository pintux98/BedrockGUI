package it.pintux.life.common.api;

import it.pintux.life.common.api.BedrockGUIApi.*;
import it.pintux.life.common.api.FormValidationFramework.*;
import it.pintux.life.common.api.FormEventSystem.*;
import it.pintux.life.common.platform.PlatformFormSender;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.platform.PlatformSoundManager;
import it.pintux.life.common.platform.PlatformEconomyManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.FormConfig;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.actions.ActionExecutor;
import it.pintux.life.common.actions.ActionRegistry;

import java.util.*;
import java.util.function.Consumer;

/**
 * Comprehensive examples demonstrating the BedrockGUIApi capabilities
 * This class serves as both documentation and practical implementation guide
 */
public class BedrockGUIApiExamples {
    
    private final BedrockGUIApi api;
    private final FormTemplateManager templateManager;
    private final FormValidationFramework validationFramework;
    private final FormEventSystem eventSystem;
    private final FormBuilderFactory builderFactory;
    
    public BedrockGUIApiExamples(FormConfig formConfig, MessageData messageData, 
                                PlatformCommandExecutor commandExecutor, PlatformSoundManager soundManager,
                                PlatformEconomyManager economyManager, PlatformFormSender formSender) {
        // Initialize the complete API system
        this.api = new BedrockGUIApi(formConfig, messageData, commandExecutor, soundManager, economyManager, formSender);
        this.templateManager = new FormTemplateManager(api);
        this.validationFramework = new FormValidationFramework();
        this.eventSystem = new FormEventSystem();
        this.builderFactory = new FormBuilderFactory(api);
        
        setupEventListeners();
        setupValidationRules();
    }
    
    /**
     * Example 1: Basic Simple Form
     */
    public void createBasicSimpleForm(FormPlayer player) {
        SimpleFormBuilder form = api.createSimpleForm("Main Menu");
        form.content("Welcome to our server! Choose an option:");
        form.button("Player Stats", "textures/ui/icon_steve", p -> {
                // Show player statistics
                showPlayerStats(p);
            })
            .button("Server Shop", "textures/ui/icon_emerald", p -> {
                // Open server shop
                openServerShop(p);
            })
            .button("Settings", "textures/ui/settings_glyph_color_2x", p -> {
                // Open settings
                openSettings(p);
            })
            .send(player);
    }
    
    /**
     * Example 2: Advanced Custom Form with Validation
     */
    public void createPlayerRegistrationForm(FormPlayer player) {
        api.createCustomForm("Player Registration")
            .input("Username", "Enter your username", "")
            .input("Email", "Enter your email address", "")
            .slider("Age", 13, 100, 1, 18)
            .dropdown("Country", Arrays.asList("USA", "Canada", "UK", "Germany", "France", "Other"), 0)
            .toggle("Accept Terms", false)
            .toggle("Newsletter", true)
            .onSubmit((p, results) -> {
                // Validate inputs
                ValidationChain chain = validationFramework.createValidationChain()
                    .addValidator("required")
                    .addValidator("minLength", Map.of("minLength", 3))
                    .addValidator("maxLength", Map.of("maxLength", 16))
                    .addValidator("playerName");
                
                ValidationResult usernameResult = chain.validate(results.get(0));
                if (!usernameResult.isValid()) {
                    p.sendMessage("§cInvalid username: " + usernameResult.getMessage());
                    return;
                }
                
                ValidationResult emailResult = validationFramework.validate("email", results.get(1), new HashMap<>());
                if (!emailResult.isValid()) {
                    p.sendMessage("§cInvalid email: " + emailResult.getMessage());
                    return;
                }
                
                Boolean acceptTerms = (Boolean) results.get(4);
                if (!acceptTerms) {
                    p.sendMessage("§cYou must accept the terms to register!");
                    return;
                }
                
                // Registration successful
                registerPlayer(p, results);
                p.sendMessage("§aRegistration successful! Welcome to the server!");
            })
            .send(player);
    }
    
    /**
     * Example 3: Modal Form with Dynamic Content
     */
    public void createConfirmationDialog(FormPlayer player, String action, Runnable onConfirm) {
        String title = "Confirm Action";
        String content = String.format("Are you sure you want to %s?\n\n§cThis action cannot be undone!", action);
        
        api.createModalForm(title, content)
            .buttons("§aConfirm", "§cCancel")
            .onSubmit((p, confirmed) -> {
                if (confirmed) {
                    onConfirm.run();
                    p.sendMessage("§aAction completed successfully!");
                } else {
                    p.sendMessage("§7Action cancelled.");
                }
            })
            .send(player);
    }
    
    /**
     * Example 4: Using Form Templates
     */
    public void createPlayerSelectorFromTemplate(FormPlayer player, List<String> availablePlayers) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", "Select Player to Teleport");
        parameters.put("players", availablePlayers);
        parameters.put("onSelect", (java.util.function.Function<String, Runnable>) targetPlayer -> 
            () -> {
                // Teleport logic here
                player.sendMessage("§aTeleporting to " + targetPlayer + "...");
                // Execute teleport command
                player.executeAction("teleport " + player.getName() + " " + targetPlayer);
            }
        );
        
        FormBuilder form = templateManager.createFromTemplate("player_selector", parameters);
        form.send(player);
    }
    
    /**
     * Example 5: Economy Transaction Form
     */
    public void createEconomyTransactionForm(FormPlayer player, double currentBalance) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", "Economy Manager");
        parameters.put("currentBalance", currentBalance);
        parameters.put("maxAmount", 10000.0);
        
        FormBuilder form = templateManager.createFromTemplate("economy_transaction", parameters);
        
        if (form instanceof CustomFormBuilder) {
            ((CustomFormBuilder) form).onSubmit((p, results) -> {
                String transactionType = ((List<String>) Arrays.asList("Give Money", "Take Money", "Transfer Money", "Set Balance")).get((Integer) results.get(0));
                String amountStr = (String) results.get(1);
                Boolean confirmed = (Boolean) results.get(results.size() - 1);
                
                if (!confirmed) {
                    p.sendMessage("§cTransaction cancelled - confirmation required!");
                    return;
                }
                
                // Validate amount
                ValidationResult amountResult = validationFramework.validate("economy", amountStr, 
                    Map.of("allowNegative", false, "maxAmount", 10000.0));
                
                if (!amountResult.isValid()) {
                    p.sendMessage("§cInvalid amount: " + amountResult.getMessage());
                    return;
                }
                
                double amount = Double.parseDouble(amountStr);
                processEconomyTransaction(p, transactionType, amount, results);
            });
        }
        
        form.send(player);
    }
    
    /**
     * Example 6: Multi-Step Wizard Form
     */
    public void createServerSetupWizard(FormPlayer player) {
        List<FormBuilder> steps = Arrays.asList(
            // Step 1: Basic Information
            api.createCustomForm("Server Setup - Step 1/3")
                .input("Server Name", "Enter server name", "")
                .input("MOTD", "Enter message of the day", "")
                .slider("Max Players", 1, 100, 1, 20),
            
            // Step 2: Game Settings
            api.createCustomForm("Server Setup - Step 2/3")
                .dropdown("Difficulty", Arrays.asList("Peaceful", "Easy", "Normal", "Hard"), 2)
                .toggle("PvP Enabled", true)
                .toggle("Flight Allowed", false)
                .slider("Spawn Protection", 0, 50, 1, 16),
            
            // Step 3: Economy Settings
            api.createCustomForm("Server Setup - Step 3/3")
                .input("Starting Money", "Enter starting money amount", "1000")
                .dropdown("Currency Symbol", Arrays.asList("$", "€", "£", "¥", "Coins"), 0)
                .toggle("Enable Shop", true)
                .toggle("Enable Auctions", false)
        );
        
        FormBuilderFactory.WizardFormBuilder wizard = builderFactory.createWizard("Server Setup Wizard", steps)
            .onComplete((p, allResults) -> {
                // Process all wizard results
                processServerSetup(p, allResults);
                p.sendMessage("§aServer setup completed successfully!");
            })
            .onCancel(p -> p.sendMessage("§7Server setup cancelled."));
        
        wizard.send(player);
    }
    
    /**
     * Example 7: Dynamic Form with Conditional Logic
     */
    public void createDynamicPermissionForm(FormPlayer player, String targetPlayer) {
        // Create a dynamic form that changes based on player's current permissions
        DynamicFormBuilder dynamicForm = api.createDynamicForm("Manage Permissions - " + targetPlayer)
            .addCondition("isAdmin", context -> player.hasPermission("admin.permissions"))
            .addCondition("isModerator", context -> player.hasPermission("moderator.permissions"))
            .addCondition("canManageEconomy", context -> player.hasPermission("economy.manage"));
        
        // Add components based on conditions
        dynamicForm.addComponent("always", api.createComponent("input", "Target Player", targetPlayer))
                  .addComponent("isAdmin", api.createComponent("toggle", "Admin Permissions", false))
                  .addComponent("isModerator", api.createComponent("toggle", "Moderator Permissions", false))
                  .addComponent("canManageEconomy", api.createComponent("toggle", "Economy Permissions", false))
                  .addComponent("always", api.createComponent("dropdown", "Permission Level", 
                      Arrays.asList("Guest", "Member", "VIP", "Moderator", "Admin")));
        
        dynamicForm.onSubmit((p, results) -> {
            // Process permission changes
            applyPermissionChanges(p, targetPlayer, results);
        });
        
        dynamicForm.send(player);
    }
    
    /**
     * Example 8: Form with Advanced Validation Chain
     */
    public void createAdvancedValidationForm(FormPlayer player) {
        api.createCustomForm("Advanced Validation Example")
            .input("Username", "3-16 characters, alphanumeric", "")
            .input("Password", "Minimum 8 characters", "")
            .input("Confirm Password", "Must match password", "")
            .input("Email", "Valid email address", "")
            .input("Age", "Must be 13 or older", "")
            .onSubmit((p, results) -> {
                // Create complex validation chain
                Map<String, ValidationChain> validators = new HashMap<>();
                
                // Username validation
                validators.put("username", validationFramework.createValidationChain()
                    .addValidator("required")
                    .addValidator("minLength", Map.of("minLength", 3))
                    .addValidator("maxLength", Map.of("maxLength", 16))
                    .addValidator("pattern", Map.of("pattern", "^[a-zA-Z0-9_]+$", "message", "Only letters, numbers, and underscores allowed"))
                );
                
                // Password validation
                validators.put("password", validationFramework.createValidationChain()
                    .addValidator("required")
                    .addValidator("minLength", Map.of("minLength", 8))
                    .addCustom(value -> {
                        String pwd = value.toString();
                        if (!pwd.matches(".*[A-Z].*")) {
                            return ValidationResult.failure("Password must contain at least one uppercase letter");
                        }
                        if (!pwd.matches(".*[a-z].*")) {
                            return ValidationResult.failure("Password must contain at least one lowercase letter");
                        }
                        if (!pwd.matches(".*[0-9].*")) {
                            return ValidationResult.failure("Password must contain at least one number");
                        }
                        return ValidationResult.success();
                    })
                );
                
                // Email validation
                validators.put("email", validationFramework.createValidationChain()
                    .addValidator("required")
                    .addValidator("email")
                );
                
                // Age validation
                validators.put("age", validationFramework.createValidationChain()
                    .addValidator("required")
                    .addValidator("numeric")
                    .addValidator("range", Map.of("min", 13.0, "max", 120.0))
                );
                
                // Validate all fields
                String[] fieldNames = {"username", "password", "email", "age"};
                for (int i = 0; i < fieldNames.length; i++) {
                    if (i < results.size()) {
                        ValidationResult result = validators.get(fieldNames[i]).validate(results.get(i));
                        if (!result.isValid()) {
                            p.sendMessage("§c" + fieldNames[i] + ": " + result.getMessage());
                            return;
                        }
                    }
                }
                
                // Check password confirmation
                if (results.size() >= 3) {
                    String password = (String) results.get(1);
                    String confirmPassword = (String) results.get(2);
                    if (!password.equals(confirmPassword)) {
                        p.sendMessage("§cPasswords do not match!");
                        return;
                    }
                }
                
                p.sendMessage("§aAll validations passed! Account created successfully.");
            })
            .send(player);
    }
    
    /**
     * Example 9: Form with Event Handling
     */
    public void createEventDrivenForm(FormPlayer player) {
        // Add event listeners for this specific form
        eventSystem.addEventListener(FormEvent.FORM_OPENED, eventData -> {
            if ("Event-Driven Form".equals(eventData.getForm().getTitle())) {
                eventData.getPlayer().sendMessage("§aWelcome! This form has advanced event handling.");
            }
        });
        
        eventSystem.addEventListener(FormEvent.VALIDATION_FAILED, eventData -> {
            if ("Event-Driven Form".equals(eventData.getForm().getTitle())) {
                eventData.getPlayer().sendMessage("§cValidation failed: " + eventData.getValidationResult().getMessage());
            }
        });
        
        api.createCustomForm("Event-Driven Form")
            .input("Test Input", "Enter something", "")
            .onSubmit((p, results) -> {
                // Fire custom events
                FormEventData submitEvent = eventSystem.createFormEvent(FormEvent.FORM_SUBMITTED, p, null);
                submitEvent.setMetadata("inputValue", results.get(0));
                eventSystem.fireEvent(submitEvent);
                
                p.sendMessage("§aForm submitted with events!");
            })
            .send(player);
    }
    
    /**
     * Example 10: Paginated List Form
     */
    public void createPaginatedPlayerList(FormPlayer player, List<String> allPlayers) {
        FormBuilderFactory.PaginatedFormBuilder paginatedForm = builderFactory.createPaginatedList(
            "Online Players",
            allPlayers,
            5, // Items per page
            (playerName, pageBuilder) -> {
                pageBuilder.button(playerName, "textures/ui/icon_steve", p -> {
                    // Show player info or perform action
                    showPlayerInfo(p, playerName);
                });
            }
        );
        
        // Build and send the form
        org.geysermc.cumulus.form.Form form = paginatedForm.build();
        api.openForm(player, form);
    }
    
    // ==================== SETUP METHODS ====================
    
    private void setupEventListeners() {
        // Add logging interceptor
        eventSystem.addInterceptor(new LoggingInterceptor());
        
        // Add rate limiting
        eventSystem.addInterceptor(new RateLimitInterceptor(1000)); // 1 second cooldown
        
        // Add permission checking for sensitive operations
        PermissionInterceptor permissionInterceptor = new PermissionInterceptor()
            .addPermissionCheck("admin.form.opened", "admin.forms")
            .addPermissionCheck("economy.form.submitted", "economy.manage");
        eventSystem.addInterceptor(permissionInterceptor);
        
        // Global error handler
        eventSystem.addEventListener(FormEvent.FORM_ERROR, eventData -> {
            FormPlayer player = eventData.getPlayer();
            if (player != null) {
                player.sendMessage("§cAn error occurred while processing the form. Please try again.");
            }
        });
    }
    
    private void setupValidationRules() {
        // Register custom validation rules
        validationFramework.registerRule("strongPassword", value -> {
            String pwd = value.toString();
            return pwd.length() >= 8 && 
                   pwd.matches(".*[A-Z].*") && 
                   pwd.matches(".*[a-z].*") && 
                   pwd.matches(".*[0-9].*");
        });
        
        validationFramework.registerRule("validMinecraftName", value -> {
            String name = value.toString();
            return name.length() >= 3 && name.length() <= 16 && name.matches("^[a-zA-Z0-9_]+$");
        });
    }
    
    // ==================== HELPER METHODS ====================
    
    private void showPlayerStats(FormPlayer player) {
        // Implementation for showing player statistics
        player.sendMessage("§aShowing player statistics...");
    }
    
    private void openServerShop(FormPlayer player) {
        // Implementation for opening server shop
        player.sendMessage("§aOpening server shop...");
    }
    
    private void openSettings(FormPlayer player) {
        // Implementation for opening settings
        player.sendMessage("§aOpening settings...");
    }
    
    private void registerPlayer(FormPlayer player, Map<String, Object> results) {
        // Implementation for player registration
        String username = (String) results.getOrDefault("username", "");
        String email = (String) results.getOrDefault("email", "");
        player.sendMessage("§aRegistered with username: " + username + " and email: " + email);
    }
    
    private void processEconomyTransaction(FormPlayer player, String type, double amount, Map<String, Object> results) {
        // Implementation for processing economy transactions
        player.sendMessage("§aProcessing " + type + " for amount: $" + amount);
    }
    
    private void processServerSetup(FormPlayer player, Map<String, Object> allResults) {
        // Implementation for processing server setup
        player.sendMessage("§aServer setup completed successfully");
    }
    
    private void applyPermissionChanges(FormPlayer player, String targetPlayer, Map<String, Object> results) {
        // Implementation for applying permission changes
        player.sendMessage("§aApplied permission changes for " + targetPlayer);
    }
    
    private void showPlayerInfo(FormPlayer player, String targetPlayer) {
        // Implementation for showing player information
        player.sendMessage("§aShowing info for player: " + targetPlayer);
    }
    
    // ==================== USAGE DOCUMENTATION ====================
    
    /**
     * USAGE GUIDE:
     * 
     * 1. BASIC FORMS:
     *    - Use api.createSimpleForm() for button-based menus
     *    - Use api.createModalForm() for yes/no confirmations
     *    - Use api.createCustomForm() for input forms
     * 
     * 2. VALIDATION:
     *    - Use validationFramework.validate() for single field validation
     *    - Use validationFramework.createValidationChain() for complex validation
     *    - Register custom validators with validationFramework.registerValidator()
     * 
     * 3. TEMPLATES:
     *    - Use templateManager.createFromTemplate() for reusable form structures
     *    - Register custom templates with templateManager.registerTemplate()
     * 
     * 4. EVENTS:
     *    - Use eventSystem.addEventListener() to handle form events
     *    - Use eventSystem.addInterceptor() for cross-cutting concerns
     *    - Fire custom events with eventSystem.fireEvent()
     * 
     * 5. ADVANCED FEATURES:
     *    - Use builderFactory for specialized forms (wizards, pagination, etc.)
     *    - Use api.createDynamicForm() for conditional forms
     *    - Use form validation with onSubmit callbacks
     * 
     * 6. BEST PRACTICES:
     *    - Always validate user input
     *    - Use appropriate form types for different use cases
     *    - Handle errors gracefully with try-catch blocks
     *    - Use event system for logging and monitoring
     *    - Implement rate limiting for form submissions
     *    - Use templates for commonly used form patterns
     */
}