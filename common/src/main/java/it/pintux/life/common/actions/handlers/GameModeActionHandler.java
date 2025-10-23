package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.platform.PlatformCommandExecutor;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GameModeActionHandler extends BaseActionHandler {
    
    private final PlatformCommandExecutor commandExecutor;
    
    public GameModeActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }
    
    @Override
    public String getActionType() {
        return "gamemode";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        
        ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            
            if (isNewCurlyBraceFormat(actionData, "gamemode")) {
                return executeNewFormat(player, actionData, context);
            }
            
            
            List<String> operations = parseActionData(actionData, context, player);
            
            if (operations.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid game mode operations found");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }
            
            
            if (operations.size() == 1) {
                return executeSingleGameMode(player, operations.get(0), null);
            }
            
            
            return executeMultipleGameModesFromList(operations, player);
            
        } catch (Exception e) {
            logError("game mode operation", actionData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing game mode operation: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }
    
    
    private ActionResult executeNewFormat(FormPlayer player, String actionData, ActionContext context) {
        try {
            List<String> operations = parseNewFormatValues(actionData);
            
            if (operations.isEmpty()) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No game mode operations found in new format"), player);
            }
            
            
            List<String> processedOperations = new ArrayList<>();
            for (String operation : operations) {
                String processedOperation = processPlaceholders(operation, context, player);
                processedOperations.add(processedOperation);
            }
            
            
            if (processedOperations.size() == 1) {
                return executeSingleGameMode(player, processedOperations.get(0), null);
            }
            
            
            return executeMultipleGameModesFromList(processedOperations, player);
            
        } catch (Exception e) {
            logger.error("Error executing new format game mode action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error parsing new game mode format: " + e.getMessage()), player);
        }
    }
    
    
    private ActionResult executeMultipleGameModesFromList(List<String> operations, FormPlayer player) {
        int successCount = 0;
        int totalCount = operations.size();
        StringBuilder results = new StringBuilder();
        
        for (int i = 0; i < operations.size(); i++) {
            String operation = operations.get(i);
            
            try {
                logger.info("Executing game mode operation " + (i + 1) + "/" + totalCount + " for player " + player.getName() + ": " + operation);
                
                ActionResult result = executeSingleGameMode(player, operation, null);
                
                if (result.isSuccess()) {
                    successCount++;
                    results.append("âś“ Game Mode ").append(i + 1).append(": ").append(operation).append(" - Success");
                } else {
                    results.append("âś— Game Mode ").append(i + 1).append(": ").append(operation).append(" - Failed");
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
                results.append("âś— Game Mode ").append(i + 1).append(": ").append(operation).append(" - Error: ").append(e.getMessage());
                logger.error("Error executing game mode operation " + (i + 1) + " for player " + player.getName(), e);
                if (i < operations.size() - 1) {
                    results.append("\n");
                }
            }
        }
        
        String finalMessage = String.format("Executed %d/%d game mode operations successfully:\n%s", 
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
    
    
    private ActionResult executeSingleGameMode(FormPlayer player, String gameModeData, ActionContext context) {
        
        String processedData = processPlaceholders(gameModeData.trim(), context, player);
        
        
        if (!processedData.contains(":")) {
            return handleSetGameMode(player.getName(), processedData, player);
        }
        
        
        String[] parts = processedData.split(":", 3);
        
        if (parts.length == 2) {
            
            String targetPlayer = parts[0];
            String gameMode = parts[1];
            
            
            if (targetPlayer.equals("%player_name%") || targetPlayer.equals("@s")) {
                targetPlayer = player.getName();
            }
            
            return handleSetGameMode(targetPlayer, gameMode, player);
        } else if (parts.length == 3) {
            
            String operation = parts[0].toLowerCase();
            String targetPlayer = parts[1];
            String gameMode = parts[2];
            
            
            if (targetPlayer.equals("%player_name%") || targetPlayer.equals("@s")) {
                targetPlayer = player.getName();
            }
            
            switch (operation) {
                case "set":
                case "change":
                    return handleSetGameMode(targetPlayer, gameMode, player);
                case "creative":
                case "c":
                    return handleSetGameMode(targetPlayer, "creative", player);
                case "survival":
                case "s":
                    return handleSetGameMode(targetPlayer, "survival", player);
                case "adventure":
                case "a":
                    return handleSetGameMode(targetPlayer, "adventure", player);
                case "spectator":
                case "sp":
                    return handleSetGameMode(targetPlayer, "spectator", player);
                default:
                    return createFailureResult("ACTION_EXECUTION_ERROR", 
                        createReplacements("error", "Unknown gamemode operation: " + operation), player);
            }
        }
        
        return createFailureResult("ACTION_EXECUTION_ERROR", 
            createReplacements("error", "Invalid gamemode format"), player);
    }
    
    private ActionResult handleSetGameMode(String targetPlayer, String gameMode, FormPlayer executor) {
        String normalizedGameMode = normalizeGameMode(gameMode);
        if (normalizedGameMode == null) {
            return createFailureResult("ACTION_EXECUTION_ERROR", 
                createReplacements("error", "Invalid game mode: " + gameMode), executor);
        }
        
        String command = "gamemode " + normalizedGameMode + " " + targetPlayer;
        
        boolean success = executeWithErrorHandling(
            () -> {
                commandExecutor.executeAsConsole(command);
                return true;
            },
            "Gamemode command: " + command,
            executor
        );
        
        if (success) {
            String displayName = getGameModeDisplayName(normalizedGameMode);
            logSuccess("gamemode", "Set " + targetPlayer + " to " + displayName, executor);
            Map<String, Object> replacements = createReplacements("player", targetPlayer);
            replacements.put("gamemode", displayName);
            return createSuccessResult("ACTION_GAMEMODE_SUCCESS", replacements, executor);
        } else {
            logFailure("gamemode", "Failed to set " + targetPlayer + " to " + gameMode, executor);
            Map<String, Object> replacements = createReplacements("player", targetPlayer);
            replacements.put("gamemode", gameMode);
            return createFailureResult("ACTION_GAMEMODE_FAILED", replacements, executor);
        }
    }
    
    private String normalizeGameMode(String gameMode) {
        if (gameMode == null) return null;
        
        String normalized = gameMode.toLowerCase().trim();
        switch (normalized) {
            case "creative":
            case "c":
            case "1":
                return "creative";
            case "survival":
            case "s":
            case "0":
                return "survival";
            case "adventure":
            case "a":
            case "2":
                return "adventure";
            case "spectator":
            case "sp":
            case "3":
                return "spectator";
            default:
                return null;
        }
    }
    
    private String getGameModeDisplayName(String gameMode) {
        switch (gameMode.toLowerCase()) {
            case "creative": return "Creative";
            case "survival": return "Survival";
            case "adventure": return "Adventure";
            case "spectator": return "Spectator";
            default: return gameMode;
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = actionValue.trim();
        
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            
            String listContent = trimmed.substring(1, trimmed.length() - 1);
            if (listContent.trim().isEmpty()) {
                return false;
            }
            String[] gameModes = listContent.split(",\\s*");
            for (String gameMode : gameModes) {
                if (!isValidSingleGameMode(gameMode.trim())) {
                    return false;
                }
            }
            return true;
        } else {
            return isValidSingleGameMode(trimmed);
        }
    }
    
    private boolean isValidSingleGameMode(String gameModeData) {
        if (gameModeData.isEmpty()) return false;
        
        
        if (!gameModeData.contains(":")) {
            return normalizeGameMode(gameModeData) != null;
        }
        
        String[] parts = gameModeData.split(":");
        if (parts.length < 2 || parts.length > 3) return false;
        
        
        String gameMode = parts[parts.length - 1];
        return normalizeGameMode(gameMode) != null;
    }

    @Override
    public String getDescription() {
        return "Changes player game modes with support for multiple operations and various targeting options";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "New Format Examples:",
            "gamemode { - \"creative\" }",
            "gamemode { - \"survival\" }",
            "gamemode { - \"PlayerName:creative\" }",
            "gamemode { - \"set:PlayerName:adventure\" }",
            "gamemode { - \"creative\" - \"survival\" - \"adventure\" }"
        };
    }
}
