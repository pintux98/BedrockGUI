package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.platform.PlatformPluginManager;
import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.ErrorHandlingUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlaceholderAPIActionHandler extends BaseActionHandler {
    private final PlatformCommandExecutor commandExecutor;
    private final PlatformPluginManager pluginManager;
    private final PlatformPlayerManager playerManager;

    private boolean placeholderAPIEnabled = false;
    private Object placeholderAPI;

    public PlaceholderAPIActionHandler(PlatformCommandExecutor commandExecutor,
                                       PlatformPluginManager pluginManager, PlatformPlayerManager playerManager) {
        this.commandExecutor = commandExecutor;
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
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {

        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }

        try {
            List<String> operations = parseActionData(actionData, context, player);

            if (operations.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid PlaceholderAPI operations found");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }


            if (operations.size() == 1) {
                return executeSinglePlaceholderOperation(operations.get(0), player);
            }


            return executeMultiplePlaceholderOperations(operations, player);

        } catch (Exception e) {
            logError("PlaceholderAPI operation", actionData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing PlaceholderAPI operation: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }

    private ActionSystem.ActionResult executeSinglePlaceholderOperation(String operationData, FormPlayer player) {
        try {
            logger.info("Executing PlaceholderAPI operation: " + operationData + " for player " + player.getName());

            Map<String, Object> parameters = parseOperationData(operationData);
            String operation = (String) parameters.get("operation");

            if (operation == null) {
                Map<String, Object> errorReplacements = createReplacements("error", "Operation parameter is required");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }


            boolean success = executeWithErrorHandling(
                    () -> {
                        ActionSystem.ActionResult result = executeOperation(operation.toLowerCase(), parameters, player);
                        return result != null && result.isSuccess();
                    },
                    "PlaceholderAPI operation: " + operation,
                    player
            );

            if (success) {
                logSuccess("PlaceholderAPI operation", operation, player);
                ActionSystem.ActionResult result = executeOperation(operation.toLowerCase(), parameters, player);
                return result;
            } else {
                Map<String, Object> errorReplacements = createReplacements("error", "Failed to execute operation: " + operation);
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }

        } catch (Exception e) {
            logError("PlaceholderAPI operation", operationData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing operation: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }

    private ActionSystem.ActionResult executeMultiplePlaceholderOperations(List<String> operations, FormPlayer player) {
        int successCount = 0;
        int totalCount = operations.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < operations.size(); i++) {
            String operationData = operations.get(i);

            try {
                logger.info("Executing PlaceholderAPI operation " + (i + 1) + "/" + totalCount + ": " + operationData + " for player " + player.getName());

                Map<String, Object> parameters = parseOperationData(operationData);
                String operation = (String) parameters.get("operation");

                if (operation == null) {
                    results.append("âś— Operation ").append(i + 1).append(": ").append(operationData).append(" - Missing operation parameter");
                    continue;
                }

                boolean success = executeWithErrorHandling(
                        () -> {
                            ActionSystem.ActionResult result = executeOperation(operation.toLowerCase(), parameters, player);
                            return result != null && result.isSuccess();
                        },
                        "PlaceholderAPI operation: " + operation,
                        player
                );

                if (success) {
                    successCount++;
                    results.append("âś“ Operation ").append(i + 1).append(": ").append(operation).append(" - Success");
                    logSuccess("PlaceholderAPI operation", operation, player);
                } else {
                    results.append("âś— Operation ").append(i + 1).append(": ").append(operation).append(" - Failed");
                }

                if (i < operations.size() - 1) {
                    results.append("\n");

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (Exception e) {
                results.append("âś— Operation ").append(i + 1).append(": ").append(operationData).append(" - Error: ").append(e.getMessage());
                logError("PlaceholderAPI operation", operationData, player, e);
                if (i < operations.size() - 1) {
                    results.append("\n");
                }
            }
        }

        String finalMessage = String.format("Executed %d/%d PlaceholderAPI operations successfully:\n%s",
                successCount, totalCount, results.toString());

        Map<String, Object> replacements = new HashMap<>();
        replacements.put("message", finalMessage);
        replacements.put("success_count", successCount);
        replacements.put("total_count", totalCount);

        if (successCount == totalCount) {
            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } else if (successCount > 0) {
            return createSuccessResult("ACTION_PARTIAL_SUCCESS", replacements, player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
    }

    private ActionSystem.ActionResult executeOperation(String operation, Map<String, Object> parameters, FormPlayer player) {
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
                Map<String, Object> errorReplacements = createReplacements("error", "Unknown operation: " + operation);
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
        }
    }

    private ActionSystem.ActionResult handleSetPlaceholder(Map<String, Object> parameters, FormPlayer player) {
        String placeholder = processPlaceholders((String) parameters.get("placeholder"), null, player);
        String value = processPlaceholders((String) parameters.get("value"), null, player);
        String targetPlayer = processPlaceholders((String) parameters.getOrDefault("player", player.getName()), null, player);

        if (placeholder == null) {
            Map<String, Object> errorReplacements = createReplacements("error", "Placeholder parameter is required");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
        }

        if (value == null) {
            Map<String, Object> errorReplacements = createReplacements("error", "Value parameter is required");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
        }

        String command = "papi set " + targetPlayer + " " + placeholder + " " + value;
        commandExecutor.executeAsConsole(command);

        Map<String, Object> replacements = new HashMap<>();
        replacements.put("message", "Set placeholder " + placeholder + " to " + value + " for " + targetPlayer);
        replacements.put("placeholder", placeholder);
        replacements.put("value", value);
        replacements.put("target_player", targetPlayer);

        return createSuccessResult("ACTION_SUCCESS", replacements, player);
    }

    private ActionSystem.ActionResult handleRemovePlaceholder(Map<String, Object> parameters, FormPlayer player) {
        String placeholder = processPlaceholders((String) parameters.get("placeholder"), null, player);
        String targetPlayer = processPlaceholders((String) parameters.getOrDefault("player", player.getName()), null, player);

        if (placeholder == null) {
            Map<String, Object> errorReplacements = createReplacements("error", "Placeholder parameter is required");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
        }

        String command = "papi remove " + targetPlayer + " " + placeholder;
        commandExecutor.executeAsConsole(command);

        Map<String, Object> replacements = new HashMap<>();
        replacements.put("message", "Removed placeholder " + placeholder + " for " + targetPlayer);
        replacements.put("placeholder", placeholder);
        replacements.put("target_player", targetPlayer);

        return createSuccessResult("ACTION_SUCCESS", replacements, player);
    }

    private ActionSystem.ActionResult handleGetPlaceholder(Map<String, Object> parameters, FormPlayer player) {
        String placeholder = processPlaceholders((String) parameters.get("placeholder"), null, player);
        String targetPlayerName = processPlaceholders((String) parameters.getOrDefault("player", player.getName()), null, player);

        if (placeholder == null) {
            Map<String, Object> errorReplacements = createReplacements("error", "Placeholder parameter is required");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
        }

        try {
            Object playerObj = playerManager.getPlayer(targetPlayerName);
            if (playerObj == null) {
                Map<String, Object> errorReplacements = createReplacements("error", "Target player not found: " + targetPlayerName);
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }

            FormPlayer targetPlayer = playerManager.toFormPlayer(playerObj);
            String value = processPlaceholders("%" + placeholder + "%", null, targetPlayer);

            Map<String, Object> replacements = new HashMap<>();
            replacements.put("value", value);
            replacements.put("placeholder", placeholder);
            replacements.put("target_player", targetPlayerName);

            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } catch (Exception e) {
            Map<String, Object> errorReplacements = createReplacements("error", "Failed to get placeholder value");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }

    private ActionSystem.ActionResult handleParsePlaceholder(Map<String, Object> parameters, FormPlayer player) {
        String text = processPlaceholders((String) parameters.get("text"), null, player);
        String targetPlayerName = processPlaceholders((String) parameters.getOrDefault("player", player.getName()), null, player);

        if (text == null) {
            Map<String, Object> errorReplacements = createReplacements("error", "Text parameter is required");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
        }

        try {
            Object playerObj = playerManager.getPlayer(targetPlayerName);
            if (playerObj == null) {
                Map<String, Object> errorReplacements = createReplacements("error", "Target player not found: " + targetPlayerName);
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }

            FormPlayer targetPlayer = playerManager.toFormPlayer(playerObj);
            String parsedText = processPlaceholders(text, null, targetPlayer);

            Map<String, Object> replacements = new HashMap<>();
            replacements.put("parsed_text", parsedText);
            replacements.put("original_text", text);
            replacements.put("target_player", targetPlayerName);

            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } catch (Exception e) {
            Map<String, Object> errorReplacements = createReplacements("error", "Failed to parse placeholders");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }

    private ActionSystem.ActionResult handleRegisterExpansion(Map<String, Object> parameters, FormPlayer player) {
        String expansionName = processPlaceholders((String) parameters.get("expansion"), null, player);

        if (expansionName == null) {
            Map<String, Object> errorReplacements = createReplacements("error", "Expansion parameter is required");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
        }

        try {
            String command = "papi register " + expansionName;
            commandExecutor.executeAsConsole(command);

            Map<String, Object> replacements = new HashMap<>();
            replacements.put("expansion", expansionName);
            replacements.put("message", "Registered expansion: " + expansionName);

            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } catch (Exception e) {
            Map<String, Object> errorReplacements = createReplacements("error", "Failed to register expansion");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }

    private ActionSystem.ActionResult handleUnregisterExpansion(Map<String, Object> parameters, FormPlayer player) {
        String expansionName = processPlaceholders((String) parameters.get("expansion"), null, player);

        if (expansionName == null) {
            Map<String, Object> errorReplacements = createReplacements("error", "Expansion parameter is required");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
        }

        try {
            String command = "papi unregister " + expansionName;
            commandExecutor.executeAsConsole(command);

            Map<String, Object> replacements = new HashMap<>();
            replacements.put("expansion", expansionName);
            replacements.put("message", "Unregistered expansion: " + expansionName);

            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } catch (Exception e) {
            Map<String, Object> errorReplacements = createReplacements("error", "Failed to unregister expansion");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }

    private ActionSystem.ActionResult handleReloadExpansions(Map<String, Object> parameters, FormPlayer player) {
        try {
            String command = "papi reload";
            commandExecutor.executeAsConsole(command);

            Map<String, Object> replacements = new HashMap<>();
            replacements.put("message", "Reloaded PlaceholderAPI expansions");

            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } catch (Exception e) {
            Map<String, Object> errorReplacements = createReplacements("error", "Failed to reload expansions");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }

    private ActionSystem.ActionResult handleListExpansions(Map<String, Object> parameters, FormPlayer player) {
        try {
            String command = "papi list";
            commandExecutor.executeAsConsole(command);

            Map<String, Object> replacements = new HashMap<>();
            replacements.put("message", "Listed PlaceholderAPI expansions in console");

            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } catch (Exception e) {
            Map<String, Object> errorReplacements = createReplacements("error", "Failed to list expansions");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }

    private Map<String, Object> parseOperationData(String operationData) {
        Map<String, Object> parameters = new HashMap<>();
        if (operationData == null || operationData.trim().isEmpty()) {
            return parameters;
        }


        String[] pairs = operationData.split(";");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                parameters.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return parameters;
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }


        List<String> operations = parseActionDataForValidation(actionValue);

        for (String operation : operations) {
            if (!isValidOperation(operation)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidOperation(String operation) {
        if (operation == null || operation.trim().isEmpty()) {
            return false;
        }


        return operation.contains("operation:");
    }

    @Override
    public String getDescription() {
        return "Handles PlaceholderAPI integration for parsing and setting placeholders. Supports multiple operations with sequential execution and enhanced error handling.";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{

                "operation:set_placeholder;placeholder:custom_points;value:100 - Set custom placeholder",
                "operation:get_placeholder;placeholder:player_health - Get placeholder value",
                "operation:parse_placeholder;text:Hello %player_name%! - Parse placeholders in text",
                "operation:remove_placeholder;placeholder:temp_data - Remove placeholder",
                "operation:reload_expansions - Reload all expansions",
                "operation:list_expansions - List all expansions",


                "[\"operation:set_placeholder;placeholder:points;value:100\", \"operation:set_placeholder;placeholder:level;value:5\"] - Set multiple placeholders",
                "[\"operation:parse_placeholder;text:Welcome %player_name%\", \"operation:get_placeholder;placeholder:player_balance\"] - Parse and get operations",
                "[\"operation:reload_expansions\", \"operation:list_expansions\"] - Reload then list expansions",
                "[\"operation:register_expansion;expansion:custom\", \"operation:set_placeholder;placeholder:custom_data;value:test\"] - Register and set sequence"
        };
    }
}

