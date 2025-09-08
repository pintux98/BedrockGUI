package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;

/**
 * Handles sending messages to players
 */
public class MessageActionHandler extends BaseActionHandler {
    
    private final PlatformPlayerManager playerManager;
    
    public MessageActionHandler(PlatformPlayerManager playerManager) {
        this.playerManager = playerManager;
    }
    
    @Override
    public String getActionType() {
        return "message";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        // Validate basic parameters using base class method
        ActionResult validationResult = validateBasicParameters(player, actionValue);
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            String processedMessage = processPlaceholders(actionValue, context, player);
            
            if (ValidationUtils.isNullOrEmpty(processedMessage.trim())) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", MessageData.ACTION_INVALID_PARAMETERS), player);
            }
            
            // Process color codes and send the message to the player
            String coloredMessage = processColorCodes(processedMessage);
            playerManager.sendMessage(player, coloredMessage);
            
            logSuccess("message", processedMessage, player);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", MessageData.ACTION_MESSAGE_SENT), player);
            
        } catch (Exception e) {
            logError("message", actionValue, player, e);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", MessageData.ACTION_MESSAGE_FAILED), player, e);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return false;
        }
        
        // Basic validation - message should not be too long
        String trimmed = actionValue.trim();
        return trimmed.length() > 0 && trimmed.length() <= 1000; // Reasonable message length limit
    }
    
    @Override
    public String getDescription() {
        return "Sends a chat message to the player. Supports placeholders for dynamic content.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "Welcome to the server, {player}!",
            "&aYou have selected: {selected_option}",
            "&#FF5733Your balance is: ${balance}",
            "<green>Thank you for your purchase!</green>",
            "<color:#00FF00>Success!</color> <bold>Operation completed</bold>",
            "&c&lERROR: &r&7Something went wrong",
            "<red><bold>Warning:</bold></red> <yellow>Please be careful</yellow>"
        };
    }
    

    
    /**
     * Processes various color code formats in the message
     * Supports:
     * - Classic MC color codes (&a, &b, etc.)
     * - Hex color codes (&#RRGGBB)
     * - Legacy color codes (a, b, etc.)
     * - MiniMessage format (<color:#RRGGBB>, <red>, <bold>, etc.)
     * 
     * @param message the message to process
     * @return the message with processed color codes
     */
    private String processColorCodes(String message) {
        if (ValidationUtils.isNullOrEmpty(message)) {
            return message;
        }
        
        String result = message;
        
        // Process hex color codes (&#RRGGBB)
        result = processHexColorCodes(result);
        
        // Process classic MC color codes (&a, &b, etc.)
        result = processClassicColorCodes(result);
        
        // Process MiniMessage format
        result = processMiniMessageFormat(result);
        
        return result;
    }
    
    /**
     * Processes hex color codes in format &#RRGGBB
     */
    private String processHexColorCodes(String message) {
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            // Convert to legacy format with color codes
            String replacement = "x" + hexColor.charAt(0) +  hexColor.charAt(1) + 
                                hexColor.charAt(2) +  hexColor.charAt(3) + 
                                hexColor.charAt(4) +  hexColor.charAt(5);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Processes classic MC color codes (&a, &b, etc.)
     */
    private String processClassicColorCodes(String message) {
        // Convert & color codes to  color codes
        return message.replaceAll("&([0-9a-fk-or])", "$1");
    }
    
    /**
     * Processes MiniMessage format (<color:#RRGGBB>, <red>, <bold>, etc.)
     */
    private String processMiniMessageFormat(String message) {
        String result = message;
        
        // Process hex colors in MiniMessage format <color:#RRGGBB>
        Pattern miniHexPattern = Pattern.compile("<color:#([A-Fa-f0-9]{6})>");
        Matcher hexMatcher = miniHexPattern.matcher(result);
        StringBuffer hexResult = new StringBuffer();
        while (hexMatcher.find()) {
            String hexColor = hexMatcher.group(1);
            String replacement = "x" + hexColor.charAt(0) +  hexColor.charAt(1) + 
                                hexColor.charAt(2) +  hexColor.charAt(3) + 
                                hexColor.charAt(4) +  hexColor.charAt(5);
            hexMatcher.appendReplacement(hexResult, replacement);
        }
        hexMatcher.appendTail(hexResult);
        result = hexResult.toString();
        
        // Process named colors and formatting
        Map<String, String> miniMessageMap = new HashMap<>();
        // Colors
        miniMessageMap.put("<black>", "0");
        miniMessageMap.put("<dark_blue>", "1");
        miniMessageMap.put("<dark_green>", "2");
        miniMessageMap.put("<dark_aqua>", "3");
        miniMessageMap.put("<dark_red>", "4");
        miniMessageMap.put("<dark_purple>", "5");
        miniMessageMap.put("<gold>", "6");
        miniMessageMap.put("<gray>", "7");
        miniMessageMap.put("<dark_gray>", "8");
        miniMessageMap.put("<blue>", "9");
        miniMessageMap.put("<green>", "a");
        miniMessageMap.put("<aqua>", "b");
        miniMessageMap.put("<red>", "c");
        miniMessageMap.put("<light_purple>", "d");
        miniMessageMap.put("<yellow>", "e");
        miniMessageMap.put("<white>", "f");
        
        // Formatting
        miniMessageMap.put("<bold>", "l");
        miniMessageMap.put("<italic>", "o");
        miniMessageMap.put("<underlined>", "n");
        miniMessageMap.put("<strikethrough>", "m");
        miniMessageMap.put("<obfuscated>", "k");
        miniMessageMap.put("<reset>", "r");
        
        // Closing tags
        miniMessageMap.put("</bold>", "r");
        miniMessageMap.put("</italic>", "r");
        miniMessageMap.put("</underlined>", "r");
        miniMessageMap.put("</strikethrough>", "r");
        miniMessageMap.put("</obfuscated>", "r");
        miniMessageMap.put("</color>", "r");
        
        // Apply all replacements
        for (Map.Entry<String, String> entry : miniMessageMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        return result;
    }
}
