package it.pintux.life.common.utils;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;


public class ErrorHandlingUtil {
    private static final Logger logger = Logger.getLogger(ErrorHandlingUtil.class.getSimpleName());


    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1000;
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;


    public static <T> T executeWithRetry(Supplier<T> operation, Supplier<T> fallback,
                                         String operationName, int maxRetries) {
        return executeWithRetry(operation, fallback, operationName, maxRetries,
                DEFAULT_RETRY_DELAY_MS, DEFAULT_BACKOFF_MULTIPLIER);
    }


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


    public static ActionSystem.ActionResult createSafeActionResult(Supplier<ActionSystem.ActionResult> operation,
                                                      FormPlayer player, String actionType) {
        try {
            ActionSystem.ActionResult result = operation.get();
            return result != null ? result : createFallbackFailureResult(player, actionType, "Operation returned null");
        } catch (Exception e) {
            logger.error("Error creating ActionResult for {} action: {}", actionType, e.getMessage(), e);
            return createFallbackFailureResult(player, actionType, e.getMessage());
        }
    }


    private static ActionSystem.ActionResult createFallbackFailureResult(FormPlayer player, String actionType, String error) {
        try {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", error);
            replacements.put("action", actionType);

            return ActionSystem.ActionResult.failure(messageData.getValue(MessageData.EXECUTION_ERROR, replacements, player));
        } catch (Exception e) {

            logger.error("Failed to create proper error message, using basic fallback", e);
            return ActionSystem.ActionResult.failure("Action failed: " + error);
        }
    }


    public static boolean validateServiceAvailability(Supplier<Boolean> serviceCheck,
                                                      String serviceName,
                                                      FormPlayer player) {
        try {
            boolean available = executeWithRetry(
                    serviceCheck,
                    () -> false,
                    serviceName + " availability check",
                    2
            );

            if (!available) {
                logger.warn("{} service is not available for player: {}", serviceName, player.getName());
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("service", serviceName);
                player.sendMessage(messageData.getValue(MessageData.SYSTEM_SERVICE_UNAVAILABLE, replacements, player));
            }

            return available;
        } catch (Exception e) {
            logger.error("Error checking {} service availability: {}", serviceName, e.getMessage());
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("service", serviceName);
            player.sendMessage(messageData.getValue(MessageData.SYSTEM_SERVICE_STATUS_CHECK_FAILED, replacements, player));
            return false;
        }
    }


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
                return true;
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
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("description", commandDescription);
                player.sendMessage(messageData.getValue(MessageData.SYSTEM_COMMAND_EXECUTION_FAILED, replacements, player));
            }

            return success;
        } catch (Exception e) {
            logger.error("Error executing command '{}': {}", commandDescription, e.getMessage());
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            player.sendMessage(messageData.getValue(MessageData.SYSTEM_OPERATION_INTERRUPTED, null, player));
            return false;
        }
    }
}

