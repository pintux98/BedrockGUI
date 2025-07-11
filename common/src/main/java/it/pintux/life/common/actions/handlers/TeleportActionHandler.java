package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ValidationUtils;


import java.util.Map;
import java.util.regex.Pattern;

/**
 * Example action handler for teleporting players
 * Demonstrates how to create custom action handlers
 */
public class TeleportActionHandler implements ActionHandler {
    
    private static final Logger logger = Logger.getLogger(TeleportActionHandler.class);
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    
    @Override
    public String getActionType() {
        return "teleport";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null) {
            return ActionResult.failure("Player cannot be null");
        }
        
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return ActionResult.failure("Teleport coordinates cannot be null or empty");
        }
        
        try {
            String processedValue = processPlaceholders(actionValue, context);
            String[] parts = processedValue.trim().split("\\s+");
            
            if (parts.length != 3) {
                return ActionResult.failure("Teleport requires exactly 3 coordinates (x y z)");
            }
            
            // Validate coordinates
            for (String coord : parts) {
                if (!COORDINATE_PATTERN.matcher(coord).matches()) {
                    return ActionResult.failure("Invalid coordinate format: " + coord);
                }
            }
            
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            
            // Validate coordinate ranges
            if (Math.abs(x) > 30000000 || Math.abs(z) > 30000000) {
                return ActionResult.failure("Coordinates are outside world boundaries");
            }
            
            if (y < -64 || y > 320) {
                return ActionResult.failure("Y coordinate must be between -64 and 320");
            }
            
            // Execute teleport command
            String teleportCommand = String.format("tp %s %.2f %.2f %.2f", 
                player.getName(), x, y, z);
            
            player.executeAction("/" + teleportCommand);
            boolean success = true; // Assume success since executeAction doesn't return a boolean
            
            if (success) {
                logger.debug("Successfully teleported player " + player.getName() + 
                           " to " + x + ", " + y + ", " + z);
                return ActionResult.success("Teleported successfully");
            } else {
                logger.warn("Failed to teleport player " + player.getName());
                return ActionResult.failure("Teleport command failed");
            }
            
        } catch (NumberFormatException e) {
            return ActionResult.failure("Invalid number format in coordinates");
        } catch (Exception e) {
            logger.error("Error teleporting player " + player.getName(), e);
            return ActionResult.failure("Teleport error: " + e.getMessage(), e);
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
     * Processes placeholders in the coordinates
     * @param coordinates the coordinates with placeholders
     * @param context the action context containing placeholder values
     * @return the processed coordinates
     */
    private String processPlaceholders(String coordinates, ActionContext context) {
        if (context == null) {
            return coordinates;
        }
        
        String result = PlaceholderUtil.processDynamicPlaceholders(coordinates, context.getPlaceholders());
        result = PlaceholderUtil.processFormResults(result, context.getFormResults());
        
        return result;
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