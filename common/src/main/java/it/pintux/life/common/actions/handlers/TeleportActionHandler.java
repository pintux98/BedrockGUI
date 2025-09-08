package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;
import it.pintux.life.common.utils.PlaceholderUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Example action handler for teleporting players
 * Demonstrates how to create custom action handlers
 */
public class TeleportActionHandler extends BaseActionHandler {
    
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    
    @Override
    public String getActionType() {
        return "teleport";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        // Validate basic parameters using base class method
        ActionResult validationResult = validateBasicParameters(player, actionValue);
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            String processedValue = processPlaceholders(actionValue, context, player);
            String[] parts = processedValue.trim().split("\\s+");
            
            if (parts.length != 3) {
                return createFailureResult(MessageData.ACTION_INVALID_FORMAT, null, player);
            }
            
            // Validate coordinates
            for (String coord : parts) {
                if (!COORDINATE_PATTERN.matcher(coord).matches()) {
                    return createFailureResult(MessageData.ACTION_INVALID_FORMAT, createReplacements("coordinate", coord), player);
                }
            }
            
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            
            // Validate coordinate ranges
            if (Math.abs(x) > 30000000 || Math.abs(z) > 30000000) {
                return createFailureResult(MessageData.ACTION_TELEPORT_OUT_OF_BOUNDS, null, player);
            }
            
            if (y < -64 || y > 320) {
                return createFailureResult(MessageData.ACTION_TELEPORT_INVALID_Y, null, player);
            }
            
            // Execute teleport command
            String teleportCommand = String.format("tp %s %.2f %.2f %.2f", 
                player.getName(), x, y, z);
            
            player.executeAction("/" + teleportCommand);
            boolean success = true; // Assume success since executeAction doesn't return a boolean
            
            if (success) {
                logSuccess("teleport", String.format("%.2f, %.2f, %.2f", x, y, z), player);
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("x", String.valueOf(x));
                replacements.put("y", String.valueOf(y));
                replacements.put("z", String.valueOf(z));
                return createSuccessResult(MessageData.ACTION_TELEPORT_SUCCESS, replacements, player);
            } else {
                logFailure("teleport", actionValue, player);
                return createFailureResult(MessageData.ACTION_TELEPORT_FAILED, null, player);
            }
            
        } catch (NumberFormatException e) {
            return createFailureResult(MessageData.ACTION_INVALID_FORMAT, null, player);
        } catch (Exception e) {
            logError("teleport", actionValue, player, e);
            return createFailureResult(MessageData.ACTION_EXECUTION_ERROR, createReplacements("error", e.getMessage()), player, e);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return false;
        }
        
        String trimmed = actionValue.trim();
        
        // Check if it contains placeholders (allow them for validation)
        if (containsPlaceholders(trimmed)) {
            return true;
        }
        
        // Validate coordinate format
        String[] parts = trimmed.split("\\s+");
        if (parts.length != 3) {
            return false;
        }
        
        for (String part : parts) {
            if (!COORDINATE_PATTERN.matcher(part).matches()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Teleports the player to specified coordinates (x y z). Supports placeholders for dynamic coordinates.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "0 100 0",
            "100.5 64 -200.3",
            "{spawn_x} {spawn_y} {spawn_z}",
            "{selected_x} 100 {selected_z}"
        };
    }
    

    
    /**
     * Checks if the string contains placeholders
     * @param value the string to check
     * @return true if contains placeholders
     */
    private boolean containsPlaceholders(String value) {
        return PlaceholderUtil.containsDynamicPlaceholders(value);
    }
}