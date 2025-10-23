package it.pintux.life.common.utils;

import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.MessageData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class ErrorHandlingManager {
    
    private static final Logger LOGGER = Logger.getLogger(ErrorHandlingManager.class.getName());
    
    
    private static final Map<String, ErrorStats> errorStats = new ConcurrentHashMap<>();
    
    
    public enum ErrorCategory {
        VALIDATION_ERROR,
        EXECUTION_ERROR,
        CONFIGURATION_ERROR,
        NETWORK_ERROR,
        PERMISSION_ERROR,
        RESOURCE_ERROR,
        UNKNOWN_ERROR
    }
    
    
    private static class ErrorStats {
        private int count = 0;
        private long lastOccurrence = 0;
        private String lastMessage = "";
        
        synchronized void increment(String message) {
            count++;
            lastOccurrence = System.currentTimeMillis();
            lastMessage = message;
        }
        
        synchronized boolean shouldLog() {
            
            return System.currentTimeMillis() - lastOccurrence > 60000 || count <= 5;
        }
    }
    
    private ErrorHandlingManager() {
        
    }
    
    
    public static boolean executeWithErrorHandling(Supplier<Boolean> operation, String operationName, FormPlayer player) {
        return executeWithErrorHandling(operation, operationName, player, 1);
    }
    
    
    public static boolean executeWithErrorHandling(Supplier<Boolean> operation, String operationName, 
                                                 FormPlayer player, int maxRetries) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts <= maxRetries) {
            try {
                boolean result = operation.get();
                if (result) {
                    
                    if (attempts > 0) {
                        logInfo("Operation succeeded after " + attempts + " retries: " + operationName, player);
                    }
                    return true;
                }
                
                
                if (attempts < maxRetries) {
                    logWarning("Operation failed, retrying (" + (attempts + 1) + "/" + maxRetries + "): " + operationName, player);
                    Thread.sleep(1000 * (attempts + 1)); 
                }
                
            } catch (Exception e) {
                lastException = e;
                if (attempts < maxRetries) {
                    logWarning("Operation failed with exception, retrying (" + (attempts + 1) + "/" + maxRetries + "): " + 
                              operationName + " - " + e.getMessage(), player);
                    try {
                        Thread.sleep(1000 * (attempts + 1)); 
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            attempts++;
        }
        
        
        logError("Operation failed after " + maxRetries + " attempts: " + operationName, player, lastException);
        return false;
    }
    
    
    public static ActionResult createFailureResult(String messageKey, Map<String, String> replacements, 
                                                 FormPlayer player, ErrorCategory category) {
        return createFailureResult(messageKey, replacements, player, category, null);
    }
    
    
    public static ActionResult createFailureResult(String messageKey, Map<String, String> replacements, 
                                                 FormPlayer player, ErrorCategory category, Exception exception) {
        
        String errorKey = category.name() + ":" + messageKey;
        errorStats.computeIfAbsent(errorKey, k -> new ErrorStats()).increment(
            replacements.getOrDefault("error", "Unknown error"));
        
        
        if (errorStats.get(errorKey).shouldLog()) {
            String errorMessage = "Action failed [" + category + "]: " + messageKey;
            if (exception != null) {
                logError(errorMessage, player, exception);
            } else {
                logError(errorMessage, player, null);
            }
        }
        
        
        MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
        String localizedMessage = messageData.getValueNoPrefix(messageKey, 
            replacements != null ? new HashMap<>(replacements) : new HashMap<>(), player);
        
        if (exception != null) {
            return ActionResult.failure(localizedMessage, exception);
        } else {
            return ActionResult.failure(localizedMessage);
        }
    }
    
    
    public static ActionResult createSuccessResult(String messageKey, Map<String, String> replacements, FormPlayer player) {
        return ActionResult.success(messageKey, replacements);
    }
    
    
    public static void logError(String operation, FormPlayer player, Exception exception) {
        String context = buildContextString(operation, player);
        
        if (exception != null) {
            LOGGER.log(Level.SEVERE, context, exception);
        } else {
            LOGGER.severe(context);
        }
    }
    
    
    public static void logWarning(String message, FormPlayer player) {
        String context = buildContextString(message, player);
        LOGGER.warning(context);
    }
    
    
    public static void logInfo(String message, FormPlayer player) {
        String context = buildContextString(message, player);
        LOGGER.info(context);
    }
    
    
    public static void logSuccess(String operation, String details, FormPlayer player) {
        String context = buildContextString("SUCCESS - " + operation + ": " + details, player);
        LOGGER.info(context);
    }
    
    
    private static String buildContextString(String message, FormPlayer player) {
        StringBuilder context = new StringBuilder(message);
        
        if (player != null) {
            context.append(" [Player: ").append(player.getName());
            if (player.getUniqueId() != null) {
                context.append(" (").append(player.getUniqueId()).append(")");
            }
            context.append("]");
        }
        
        return context.toString();
    }
    
    
    public static ActionResult validateParameters(FormPlayer player, String actionValue, 
                                                Supplier<Boolean> validator, String validationErrorMessage) {
        if (player == null) {
            return createFailureResult("PLAYER_NOT_FOUND", 
                Map.of("error", "Player context is required"), 
                null, ErrorCategory.VALIDATION_ERROR);
        }
        
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return createFailureResult("INVALID_ACTION_VALUE", 
                Map.of("error", "Action value cannot be null or empty"), 
                player, ErrorCategory.VALIDATION_ERROR);
        }
        
        try {
            if (!validator.get()) {
                return createFailureResult("VALIDATION_FAILED", 
                    Map.of("error", validationErrorMessage != null ? validationErrorMessage : "Invalid action format"), 
                    player, ErrorCategory.VALIDATION_ERROR);
            }
        } catch (Exception e) {
            return createFailureResult("VALIDATION_ERROR", 
                Map.of("error", "Validation failed: " + e.getMessage()), 
                player, ErrorCategory.VALIDATION_ERROR, e);
        }
        
        return null; 
    }
    
    
    public static Map<String, String> getErrorStatistics() {
        Map<String, String> stats = new ConcurrentHashMap<>();
        
        errorStats.forEach((key, errorStat) -> {
            synchronized (errorStat) {
                stats.put(key, String.format("Count: %d, Last: %s, Message: %s", 
                    errorStat.count, 
                    new java.util.Date(errorStat.lastOccurrence).toString(),
                    errorStat.lastMessage));
            }
        });
        
        return stats;
    }
    
    
    public static void clearErrorStatistics() {
        errorStats.clear();
    }
    
    
    public static ErrorCategory categorizeException(Exception exception) {
        if (exception == null) {
            return ErrorCategory.UNKNOWN_ERROR;
        }
        
        String exceptionName = exception.getClass().getSimpleName().toLowerCase();
        String message = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        
        if (exceptionName.contains("validation") || message.contains("invalid") || message.contains("illegal")) {
            return ErrorCategory.VALIDATION_ERROR;
        } else if (exceptionName.contains("security") || message.contains("permission") || message.contains("access")) {
            return ErrorCategory.PERMISSION_ERROR;
        } else if (exceptionName.contains("io") || exceptionName.contains("network") || message.contains("connection")) {
            return ErrorCategory.NETWORK_ERROR;
        } else if (exceptionName.contains("config") || message.contains("configuration")) {
            return ErrorCategory.CONFIGURATION_ERROR;
        } else if (exceptionName.contains("resource") || message.contains("memory") || message.contains("disk")) {
            return ErrorCategory.RESOURCE_ERROR;
        } else {
            return ErrorCategory.EXECUTION_ERROR;
        }
    }
}
