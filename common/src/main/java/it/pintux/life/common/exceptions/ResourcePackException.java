package it.pintux.life.common.exceptions;

import it.pintux.life.common.utils.MessageData;
import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for resource pack related errors
 */
public class ResourcePackException extends Exception {
    
    private final String packIdentifier;
    private final ErrorType errorType;
    
    public enum ErrorType {
        PACK_NOT_FOUND,
        PACK_LOAD_FAILED,
        PACK_INVALID_FORMAT,
        PACK_TOO_LARGE,
        GEYSER_CONNECTION_FAILED,
        PLAYER_NOT_BEDROCK,
        CONFIGURATION_ERROR
    }
    
    public ResourcePackException(String message, String packIdentifier, ErrorType errorType) {
        super(message);
        this.packIdentifier = packIdentifier;
        this.errorType = errorType;
    }
    
    public ResourcePackException(String message, String packIdentifier, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.packIdentifier = packIdentifier;
        this.errorType = errorType;
    }
    
    public String getPackIdentifier() {
        return packIdentifier;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Checks if this error is recoverable
     */
    public boolean isRecoverable() {
        return errorType == ErrorType.GEYSER_CONNECTION_FAILED || 
               errorType == ErrorType.PACK_LOAD_FAILED;
    }
    
    /**
     * Gets a user-friendly error message using MessageData
     */
    public String getUserMessage(MessageData messageData) {
        Map<String, Object> replacements = new HashMap<>();
        replacements.put("pack", packIdentifier);
        
        switch (errorType) {
            case PACK_NOT_FOUND:
                return messageData.getValueNoPrefix(MessageData.RESOURCE_PACK_NOT_FOUND, replacements, null);
            case PACK_LOAD_FAILED:
                return messageData.getValueNoPrefix(MessageData.RESOURCE_PACK_LOAD_FAILED, replacements, null);
            case PACK_INVALID_FORMAT:
                return messageData.getValueNoPrefix(MessageData.RESOURCE_PACK_INVALID_FORMAT, replacements, null);
            case PACK_TOO_LARGE:
                return messageData.getValueNoPrefix(MessageData.RESOURCE_PACK_TOO_LARGE, replacements, null);
            case GEYSER_CONNECTION_FAILED:
                return messageData.getValueNoPrefix(MessageData.RESOURCE_PACK_GEYSER_CONNECTION_FAILED, replacements, null);
            case PLAYER_NOT_BEDROCK:
                return messageData.getValueNoPrefix(MessageData.RESOURCE_PACK_PLAYER_NOT_BEDROCK, replacements, null);
            case CONFIGURATION_ERROR:
                return messageData.getValueNoPrefix(MessageData.RESOURCE_PACK_CONFIGURATION_ERROR, replacements, null);
            default:
                return messageData.getValueNoPrefix(MessageData.RESOURCE_PACK_UNKNOWN_ERROR, replacements, null);
        }
    }
    
    /**
     * Gets a user-friendly error message (deprecated - use getUserMessage(MessageData) instead)
     * @deprecated Use getUserMessage(MessageData messageData) instead
     */
    @Deprecated
    public String getUserMessage() {
        switch (errorType) {
            case PACK_NOT_FOUND:
                return "Resource pack '" + packIdentifier + "' was not found";
            case PACK_LOAD_FAILED:
                return "Failed to load resource pack '" + packIdentifier + "'";
            case PACK_INVALID_FORMAT:
                return "Resource pack '" + packIdentifier + "' has invalid format";
            case PACK_TOO_LARGE:
                return "Resource pack '" + packIdentifier + "' is too large";
            case GEYSER_CONNECTION_FAILED:
                return "Failed to connect to Geyser for resource pack delivery";
            case PLAYER_NOT_BEDROCK:
                return "Resource packs are only available for Bedrock players";
            case CONFIGURATION_ERROR:
                return "Resource pack configuration error";
            default:
                return "Unknown resource pack error";
        }
    }
}