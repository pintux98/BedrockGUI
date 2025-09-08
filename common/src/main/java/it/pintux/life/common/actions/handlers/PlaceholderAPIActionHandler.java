package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.platform.PlatformPluginManager;
import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.PlaceholderProcessor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.ErrorHandlingUtil;

import java.util.List;
import java.util.Map;

/**
 * PlaceholderAPIActionHandler integrates with PlaceholderAPI to provide
 * dynamic placeholder management functionality.
 * 
 * Supported operations:
 * - set_placeholder: Set a custom placeholder value
 * - remove_placeholder: Remove a custom placeholder
 * - get_placeholder: Get placeholder value
 * - parse_placeholder: Parse placeholders in text
 * - register_expansion: Register a custom expansion
 * - unregister_expansion: Unregister an expansion
 * - reload_expansions: Reload all expansions
 * - list_expansions: List all registered expansions
 * 
 * Usage examples:
 * - action: placeholderapi
 *   operation: set_placeholder
 *   placeholder: "custom_points"
 *   value: "100"
 *   player: "%player%"
 * 
 * - action: placeholderapi
 *   operation: parse_placeholder
 *   text: "Hello %player_name%, you have %vault_eco_balance% coins!"
 *   player: "%player%"
 *   message: true
 * 
 * - action: placeholderapi
 *   operation: get_placeholder
 *   placeholder: "%player_health%"
 *   player: "%player%"
 */
public class PlaceholderAPIActionHandler extends BaseActionHandler {
    private final PlatformCommandExecutor commandExecutor;
    private final PlaceholderProcessor placeholderProcessor;
    private final PlatformPluginManager pluginManager;
    private final PlatformPlayerManager playerManager;
    
    private boolean placeholderAPIEnabled = false;
    private Object placeholderAPI;
    
    public PlaceholderAPIActionHandler(PlatformCommandExecutor commandExecutor, PlaceholderProcessor placeholderProcessor, 
                                     PlatformPluginManager pluginManager, PlatformPlayerManager playerManager) {
        this.commandExecutor = commandExecutor;
        this.placeholderProcessor = placeholderProcessor;
        this.pluginManager = pluginManager;
        this.playerManager = playerManager;
        initializePlaceholderAPI();
    }
    
    private void initializePlaceholderAPI() {
        try {
            if (!pluginManager.isPluginEnabled("PlaceholderAPI")) {
                logger.warn("PlaceholderAPI plugin not found. PlaceholderAPIActionHandler will use fallback commands.");
                return;
            }
            
            // Try to get PlaceholderAPI class
            if (pluginManager.hasClass("me.clip.placeholderapi.PlaceholderAPI")) {
                placeholderAPI = pluginManager.getClass("me.clip.placeholderapi.PlaceholderAPI");
                placeholderAPIEnabled = true;
                logger.info("PlaceholderAPI integration initialized successfully.");
            } else {
                logger.warn("PlaceholderAPI class not found.");
            }
        } catch (Exception e) {
            logger.warn("Failed to initialize PlaceholderAPI integration: " + e.getMessage());
            placeholderAPIEnabled = false;
        }
    }
    
    @Override
    public String getActionType() {
        return "placeholderapi";
    }
    
    @Override
    public String getDescription() {
        return "Handles PlaceholderAPI integration for parsing and setting placeholders";
    }

    @Override
    public boolean isValidAction(String action) {
        return action != null && (action.equals("parse") || action.equals("set"));
    }

    public String[] getUsageExamples() {
        return new String[]{
            "placeholderapi_parse %player_name%",
            "placeholderapi_set player_health 20",
            "placeholderapi_parse %player_world%"
        };
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionValue, it.pintux.life.common.actions.ActionContext context) {
        ActionResult validationResult = validateBasicParameters(player, actionValue);
        if (validationResult != null) {
            return validationResult;
        }
        
        // Parse parameters from actionValue
        Map<String, Object> parameters = parseActionValue(actionValue);
        try {
            String operation = (String) parameters.get("operation");
            if (operation == null) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Operation parameter is required"), player);
            }
            
            operation = operation.toLowerCase();
            
            switch (operation) {
                case "set_placeholder":
                    return handleSetPlaceholder(parameters, player);
                case "remove_placeholder":
                    return handleRemovePlaceholder(parameters, player);
                case "get_placeholder":
                    return handleGetPlaceholder(parameters, player);
                case "parse_placeholder":
                    return handleParsePlaceholder(parameters, player);
                case "register_expansion":
                    return handleRegisterExpansion(parameters, player);
                case "unregister_expansion":
                    return handleUnregisterExpansion(parameters, player);
                case "reload_expansions":
                    return handleReloadExpansions(parameters, player);
                case "list_expansions":
                    return handleListExpansions(parameters, player);
                default:
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Unknown operation: " + operation), player);
            }
        } catch (Exception e) {
            logger.error("Error executing placeholderapi action: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to execute placeholderapi action: " + e.getMessage()), player);
        }
    }
    
    private ActionResult handleSetPlaceholder(Map<String, Object> parameters, FormPlayer player) {
        String placeholder = placeholderProcessor.processPlaceholders(
            (String) parameters.get("placeholder"), player
        );
        
        String value = placeholderProcessor.processPlaceholders(
            (String) parameters.get("value"), player
        );
        
        String targetPlayer = placeholderProcessor.processPlaceholders(
            (String) parameters.getOrDefault("player", player.getName()), player
        );
        
        if (placeholder == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Placeholder parameter is required"), player);
        }
        
        if (value == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Value parameter is required"), player);
        }
        
        // Note: PlaceholderAPI doesn't have a direct "set placeholder" method
        // This would typically require a custom expansion or external storage
        // For now, we'll use a command-based approach
        
        String command = "papi set " + targetPlayer + " " + placeholder + " " + value;
        commandExecutor.executeAsConsole(command);
        
        String message = placeholderProcessor.processPlaceholders(
            (String) parameters.getOrDefault("success_message", "Placeholder set successfully"), player
        );
        
        if (parameters.containsKey("success_message")) {
            playerManager.sendMessage(player, message.replace("{placeholder}", placeholder).replace("{value}", value));
        }
        
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Set placeholder " + placeholder + " to " + value + " for " + targetPlayer), player);
    }
    
    private ActionResult handleRemovePlaceholder(Map<String, Object> parameters, FormPlayer player) {
        String placeholder = placeholderProcessor.processPlaceholders(
            (String) parameters.get("placeholder"), player
        );
        
        String targetPlayer = placeholderProcessor.processPlaceholders(
            (String) parameters.getOrDefault("player", player.getName()), player
        );
        
        if (placeholder == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Placeholder parameter is required"), player);
        }
        
        String command = "papi remove " + targetPlayer + " " + placeholder;
        commandExecutor.executeAsConsole(command);
        
        String message = placeholderProcessor.processPlaceholders(
            (String) parameters.getOrDefault("success_message", "Placeholder removed successfully"), player
        );
        
        if (parameters.containsKey("success_message")) {
            playerManager.sendMessage(player, message.replace("{placeholder}", placeholder));
        }
        
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Removed placeholder " + placeholder + " for " + targetPlayer), player);
    }
    
    private ActionResult handleGetPlaceholder(Map<String, Object> parameters, FormPlayer player) {
        String placeholder = placeholderProcessor.processPlaceholders(
            (String) parameters.get("placeholder"), player
        );
        
        String targetPlayerName = placeholderProcessor.processPlaceholders(
            (String) parameters.getOrDefault("player", player.getName()), player
        );
        
        if (placeholder == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Placeholder parameter is required"), player);
        }
        
        if (placeholderAPIEnabled && placeholderAPI != null) {
            try {
                Object targetPlayer = playerManager.getPlayer(targetPlayerName);
                if (targetPlayer == null) {
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Target player not found: " + targetPlayerName), player);
                }
                
                String result = (String) placeholderAPI.getClass()
                    .getMethod("setPlaceholders", Object.class, String.class)
                    .invoke(null, targetPlayer, placeholder);
                
                String message = placeholderProcessor.processPlaceholders(
                    (String) parameters.getOrDefault("message", "Placeholder value: " + result), player
                );
                
                if (parameters.containsKey("message")) {
                    playerManager.sendMessage(player, message.replace("{placeholder}", placeholder).replace("{value}", result));
                }
                
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Placeholder " + placeholder + " = " + result), player);
            } catch (Exception e) {
                logger.warn("Failed to get placeholder via PlaceholderAPI: " + e.getMessage());
            }
        }
        
        // Fallback to command with enhanced error handling
        String command = "papi parse " + targetPlayerName + " " + placeholder;
        boolean success = ErrorHandlingUtil.executeCommandWithFallback(
            () -> commandExecutor.executeAsConsole(command),
            "PlaceholderAPI parse command",
            player
        );
        
        if (success) {
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Get placeholder command executed"), player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to execute placeholder command"), player);
        }
    }
    
    private ActionResult handleParsePlaceholder(Map<String, Object> parameters, FormPlayer player) {
        String text = placeholderProcessor.processPlaceholders(
            (String) parameters.get("text"), player
        );
        
        String targetPlayerName = placeholderProcessor.processPlaceholders(
            (String) parameters.getOrDefault("player", player.getName()), player
        );
        
        boolean sendMessage = Boolean.parseBoolean(
            parameters.getOrDefault("message", "false").toString()
        );
        
        if (text == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Text parameter is required"), player);
        }
        
        if (placeholderAPIEnabled && placeholderAPI != null) {
            try {
                Object targetPlayer = playerManager.getPlayer(targetPlayerName);
                if (targetPlayer == null) {
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Target player not found: " + targetPlayerName), player);
                }
                
                String parsedText = (String) placeholderAPI.getClass()
                    .getMethod("setPlaceholders", Object.class, String.class)
                    .invoke(null, targetPlayer, text);
                
                if (sendMessage) {
                    playerManager.sendMessage(player, parsedText);
                }
                
                return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Parsed text: " + parsedText), player);
            } catch (Exception e) {
                logger.warn("Failed to parse placeholders via PlaceholderAPI: " + e.getMessage());
            }
        }
        
        // Fallback - just process with our own placeholder processor
        String parsedText = placeholderProcessor.processPlaceholders(text, player);
        
        if (sendMessage) {
            playerManager.sendMessage(player, parsedText);
        }
        
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Parsed text: " + parsedText), player);
    }
    
    private ActionResult handleRegisterExpansion(Map<String, Object> parameters, FormPlayer player) {
        String expansionName = placeholderProcessor.processPlaceholders(
            (String) parameters.get("expansion"), player
        );
        
        if (expansionName == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Expansion parameter is required"), player);
        }
        
        String command = "papi register " + expansionName;
        commandExecutor.executeAsConsole(command);
        
        String message = placeholderProcessor.processPlaceholders(
            (String) parameters.getOrDefault("success_message", "Expansion registered successfully"), player
        );
        
        if (parameters.containsKey("success_message")) {
            playerManager.sendMessage(player, message.replace("{expansion}", expansionName));
        }
        
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Registered expansion: " + expansionName), player);
    }
    
    private ActionResult handleUnregisterExpansion(Map<String, Object> parameters, FormPlayer player) {
        String expansionName = placeholderProcessor.processPlaceholders(
            (String) parameters.get("expansion"), player
        );
        
        if (expansionName == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Expansion parameter is required"), player);
        }
        
        String command = "papi unregister " + expansionName;
        commandExecutor.executeAsConsole(command);
        
        String message = placeholderProcessor.processPlaceholders(
            (String) parameters.getOrDefault("success_message", "Expansion unregistered successfully"), player
        );
        
        if (parameters.containsKey("success_message")) {
            playerManager.sendMessage(player, message.replace("{expansion}", expansionName));
        }
        
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Unregistered expansion: " + expansionName), player);
    }
    
    private ActionResult handleReloadExpansions(Map<String, Object> parameters, FormPlayer player) {
        String command = "papi reload";
        commandExecutor.executeAsConsole(command);
        
        String message = placeholderProcessor.processPlaceholders(
            (String) parameters.getOrDefault("success_message", "Expansions reloaded successfully"), player
        );
        
        if (parameters.containsKey("success_message")) {
            playerManager.sendMessage(player, message);
        }
        
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Reloaded PlaceholderAPI expansions"), player);
    }
    
    private ActionResult handleListExpansions(Map<String, Object> parameters, FormPlayer player) {
        if (placeholderAPIEnabled && placeholderAPI != null) {
            try {
                // Get expansion manager
                Object expansionManager = placeholderAPI.getClass()
                    .getMethod("getExpansionManager")
                    .invoke(null);
                
                if (expansionManager != null) {
                    // Get registered expansions
                    Object expansions = expansionManager.getClass()
                        .getMethod("getRegisteredExpansions")
                        .invoke(expansionManager);
                    
                    String expansionList = expansions.toString();
                    
                    String message = placeholderProcessor.processPlaceholders(
                        (String) parameters.getOrDefault("message", "Registered expansions: " + expansionList), player
                    );
                    
                    if (parameters.containsKey("message")) {
                        playerManager.sendMessage(player, message.replace("{expansions}", expansionList));
                    }
                    
                    return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Listed expansions: " + expansionList), player);
                }
            } catch (Exception e) {
                logger.warn("Failed to list expansions via PlaceholderAPI: " + e.getMessage());
            }
        }
        
        // Fallback to command
        String command = "papi list";
        commandExecutor.executeAsConsole(command);
        return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "List expansions command executed"), player);
    }
    
    // Add parseActionValue method
    private Map<String, Object> parseActionValue(String actionValue) {
        Map<String, Object> parameters = new java.util.HashMap<>();
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return parameters;
        }
        
        // Simple parsing - expecting format like "operation:set_placeholder;placeholder:test;value:hello"
        String[] pairs = actionValue.split(";");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                parameters.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return parameters;
    }
}