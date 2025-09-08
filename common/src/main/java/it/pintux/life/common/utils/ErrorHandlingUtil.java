package it.pintux.life.common.utils;

import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility class for enhanced error handling with retry mechanisms and fallback strategies
 */
public class ErrorHandlingUtil {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingUtil.class);
    
    // Default retry configuration
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    
    /**
     * Executes an operation with retry logic and fallback
     */
    public static <T> T executeWithRetry(Supplier<T> operation, Supplier<T> fallback, 
                                        String operationName, int maxRetries) {
        return executeWithRetry(operation, fallback, operationName, maxRetries, 
                              DEFAULT_RETRY_DELAY_MS, DEFAULT_BACKOFF_MULTIPLIER);
    }
    
    /**
     * Executes an operation with full retry configuration
     */
    public static <T> T executeWithRetry(Supplier<T> operation, Supplier<T> fallback, 
                                        String operationName, int maxRetries, 
                                        long initialDelayMs, double backoffMultiplier) {
        Exception lastException = null;
        long currentDelay = initialDelayMs;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                T result = operation.get();
                if (result != null) {
                    if (attempt > 1) {
                        logger.info("Operation '{}' succeeded on attempt {}", operationName, attempt);
                    }
                    return result;
                }
            } catch (Exception e) {
                lastException = e;
                logger.warn("Operation '{}' failed on attempt {} of {}: {}", 
                           operationName, attempt, maxRetries, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(currentDelay);
                        currentDelay = (long) (currentDelay * backoffMultiplier);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        logger.error("Operation '{}' failed after {} attempts, using fallback", operationName, maxRetries, lastException);
        
        try {
            return fallback.get();
        } catch (Exception e) {
            logger.error("Fallback for operation '{}' also failed: {}", operationName, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Executes an async operation with timeout and fallback
     */
    public static <T> CompletableFuture<T> executeWithTimeout(Supplier<CompletableFuture<T>> operation,
                                                             Supplier<T> fallback,
                                                             String operationName,
                                                             long timeoutMs) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        try {
            CompletableFuture<T> operationFuture = operation.get();
            
            operationFuture.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.warn("Operation '{}' failed or timed out: {}", operationName, throwable.getMessage());
                        try {
                            T fallbackResult = fallback.get();
                            future.complete(fallbackResult);
                        } catch (Exception e) {
                            logger.error("Fallback for operation '{}' failed: {}", operationName, e.getMessage());
                            future.completeExceptionally(e);
                        }
                    } else {
                        future.complete(result);
                    }
                });
        } catch (Exception e) {
            logger.error("Failed to start operation '{}': {}", operationName, e.getMessage());
            try {
                T fallbackResult = fallback.get();
                future.complete(fallbackResult);
            } catch (Exception fe) {
                future.completeExceptionally(fe);
            }
        }
        
        return future;
    }
    
    /**
     * Creates a safe ActionResult with error handling
     */
    public static ActionResult createSafeActionResult(Supplier<ActionResult> operation, 
                                                     FormPlayer player, String actionType) {
        try {
            ActionResult result = operation.get();
            return result != null ? result : createFallbackFailureResult(player, actionType, "Operation returned null");
        } catch (Exception e) {
            logger.error("Error creating ActionResult for {} action: {}", actionType, e.getMessage(), e);
            return createFallbackFailureResult(player, actionType, e.getMessage());
        }
    }
    
    /**
     * Creates a fallback failure result when normal error handling fails
     */
    private static ActionResult createFallbackFailureResult(FormPlayer player, String actionType, String error) {
        try {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", error);
            replacements.put("action", actionType);
            
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player));
        } catch (Exception e) {
            // Ultimate fallback - create basic ActionResult without MessageData
            logger.error("Failed to create proper error message, using basic fallback", e);
            return ActionResult.failure("Action failed: " + error);
        }
    }
    
    /**
     * Validates external service availability with fallback message
     */
    public static boolean validateServiceAvailability(Supplier<Boolean> serviceCheck, 
                                                     String serviceName, 
                                                     FormPlayer player) {
        try {
            boolean available = executeWithRetry(
                serviceCheck,
                () -> false,
                serviceName + " availability check",
                2  // Quick retry for availability checks
            );
            
            if (!available) {
                logger.warn("{} service is not available for player: {}", serviceName, player.getName());
                player.sendMessage("§c" + serviceName + " service is currently unavailable. Please try again later.");
            }
            
            return available;
        } catch (Exception e) {
            logger.error("Error checking {} service availability: {}", serviceName, e.getMessage());
            player.sendMessage("§cUnable to verify " + serviceName + " service status. Please try again later.");
            return false;
        }
    }
    
    /**
     * Handles form sending with retry and fallback to chat message
     */
    public static boolean sendFormWithFallback(FormPlayer player, 
                                              Supplier<Boolean> formSender,
                                              String fallbackMessage) {
        try {
            boolean sent = executeWithRetry(
                formSender,
                () -> false,
                "form sending to " + player.getName(),
                2
            );
            
            if (!sent && fallbackMessage != null) {
                logger.info("Form sending failed, sending fallback message to player: {}", player.getName());
                player.sendMessage(fallbackMessage);
                return true; // Consider fallback message as success
            }
            
            return sent;
        } catch (Exception e) {
            logger.error("Error in form sending with fallback: {}", e.getMessage());
            if (fallbackMessage != null) {
                player.sendMessage(fallbackMessage);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Handles command execution with fallback notification
     */
    public static boolean executeCommandWithFallback(Supplier<Boolean> commandExecutor,
                                                    String commandDescription,
                                                    FormPlayer player) {
        try {
            boolean success = executeWithRetry(
                commandExecutor,
                () -> false,
                commandDescription,
                DEFAULT_MAX_RETRIES
            );
            
            if (!success) {
                player.sendMessage("§cCommand execution failed: " + commandDescription + ". Please contact an administrator.");
            }
            
            return success;
        } catch (Exception e) {
            logger.error("Error executing command '{}': {}", commandDescription, e.getMessage());
            player.sendMessage("§cAn error occurred while executing the command. Please try again later.");
            return false;
        }
    }
}