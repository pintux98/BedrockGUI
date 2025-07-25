package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.ValidationUtils;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.platform.PlatformResourcePackManager;

/**
 * Handles resource pack related actions
 * Supports sending, removing, and checking resource packs for players
 */
public class ResourcePackActionHandler implements ActionHandler {
    
    private final BedrockGUIApi api;
    private final Logger logger;
    
    public ResourcePackActionHandler(BedrockGUIApi api, Logger logger) {
        this.api = api;
        this.logger = logger;
    }
    
    @Override
    public String getActionType() {
        return "resourcepack";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null || ValidationUtils.isNullOrEmpty(actionValue)) {
            return ActionResult.failure("Invalid parameters for resource pack action");
        }
        
        PlatformResourcePackManager packManager = api.getResourcePackManager();
        if (packManager == null) {
            return ActionResult.failure("Resource pack manager not available");
        }
        
        if (!packManager.isEnabled()) {
            return ActionResult.failure("Resource pack system is disabled. Please enable it in the configuration.");
        }
        
        // Parse action - format: "action:parameter" or just "packname"
        String[] parts = actionValue.split(":", 2);
        String actionType = parts[0].toLowerCase();
        String parameter = parts.length > 1 ? parts[1] : null;
        
        switch (actionType) {
            case "send":
                if (parameter != null) {
                    return handleSendAction(packManager, player, parameter);
                } else {
                    return ActionResult.failure("Send action requires a pack name parameter");
                }
                
            case "remove":
                if ("all".equals(parameter)) {
                    return handleRemoveAllAction(packManager, player);
                } else if (parameter != null) {
                    return handleRemoveSpecificAction(packManager, player, parameter);
                } else {
                    return ActionResult.failure("Remove action requires 'all' or pack name parameter");
                }
                
            case "check":
                if (parameter != null) {
                    return handleCheckAction(packManager, player, parameter);
                } else {
                    return ActionResult.failure("Check action requires a pack name parameter");
                }
                
            case "list":
                return handleListAction(packManager, player);
                
            default:
                // Backward compatibility - treat as direct pack name
                return handleSendAction(packManager, player, actionValue);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return false;
        }
        
        String[] parts = actionValue.split(":", 2);
        String actionType = parts[0].toLowerCase();
        
        switch (actionType) {
            case "send":
            case "check":
                return parts.length == 2 && !ValidationUtils.isNullOrEmpty(parts[1]);
            case "remove":
                return parts.length == 2 && ("all".equals(parts[1]) || !ValidationUtils.isNullOrEmpty(parts[1]));
            case "list":
                return true;
            default:
                // Backward compatibility - any non-empty string is valid as pack name
                return true;
        }
    }
    
    @Override
    public String getDescription() {
        return "Manages resource packs for players - send, remove, check, and list resource packs";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "send:ui_enhanced - Send the ui_enhanced resource pack",
            "remove:all - Remove all resource packs",
            "remove:ui_enhanced - Remove specific resource pack",
            "check:dark_theme - Check if player has dark_theme pack",
            "list - List all available resource packs",
            "ui_enhanced - Direct pack name (backward compatibility)"
        };
    }
    
    private ActionResult handleSendAction(PlatformResourcePackManager packManager, FormPlayer player, String packName) {
        try {
            boolean success = packManager.sendResourcePack(player.getUniqueId(), packName);
            if (success) {
                logger.info("Sent resource pack '" + packName + "' to player " + player.getName());
                return ActionResult.success("Resource pack '" + packName + "' sent successfully");
            } else {
                logger.warn("Failed to send resource pack '" + packName + "' to player " + player.getName());
                return ActionResult.failure("Failed to send resource pack '" + packName + "'");
            }
        } catch (Exception e) {
            logger.error("Error sending resource pack: " + e.getMessage());
            return ActionResult.failure("Error sending resource pack: " + e.getMessage());
        }
    }
    
    private ActionResult handleCheckAction(PlatformResourcePackManager packManager, FormPlayer player, String packName) {
        try {
            boolean hasPack = packManager.hasResourcePack(player.getUniqueId(), packName);
            String message = hasPack ? 
                "Player has resource pack '" + packName + "' loaded" :
                "Player doesn't have resource pack '" + packName + "' loaded";
            
            logger.info(message + " for player " + player.getName());
            return ActionResult.success(message);
        } catch (Exception e) {
            logger.error("Error checking resource pack: " + e.getMessage());
            return ActionResult.failure("Error checking resource pack: " + e.getMessage());
        }
    }
    
    private ActionResult handleRemoveAllAction(PlatformResourcePackManager packManager, FormPlayer player) {
        try {
            // Clear all resource packs for the player
            packManager.clearPlayerResourcePacks(player.getUniqueId());
            
            logger.info("Removed all resource packs for player: " + player.getName());
            return ActionResult.success("All resource packs removed successfully");
        } catch (Exception e) {
            logger.warn("Failed to remove resource packs for player " + player.getName() + ": " + e.getMessage());
            return ActionResult.failure("Failed to remove resource packs: " + e.getMessage());
        }
    }
    
    private ActionResult handleRemoveSpecificAction(PlatformResourcePackManager packManager, FormPlayer player, String packName) {
        try {
            // Remove specific resource pack
            packManager.removeResourcePack(player.getUniqueId(), packName);
            
            logger.info("Removed resource pack '" + packName + "' for player: " + player.getName());
            return ActionResult.success("Resource pack '" + packName + "' removed successfully");
        } catch (Exception e) {
            logger.warn("Failed to remove resource pack '" + packName + "' for player " + player.getName() + ": " + e.getMessage());
            return ActionResult.failure("Failed to remove resource pack '" + packName + "': " + e.getMessage());
        }
    }
    
    private ActionResult handleListAction(PlatformResourcePackManager packManager, FormPlayer player) {
        try {
            var playerPacks = packManager.getPlayerResourcePacks(player.getUniqueId());
            var availablePacks = packManager.getAvailableResourcePacks();
            
            StringBuilder result = new StringBuilder();
            result.append("Resource Pack Status:\n");
            result.append("Loaded Packs: ").append(playerPacks.isEmpty() ? "None" : String.join(", ", playerPacks)).append("\n");
            result.append("Available Packs: ").append(String.join(", ", availablePacks));
            
            logger.info("Listed resource packs for player: " + player.getName());
            return ActionResult.success(result.toString());
        } catch (Exception e) {
            logger.warn("Failed to list resource packs for player " + player.getName() + ": " + e.getMessage());
            return ActionResult.failure("Failed to retrieve resource pack information: " + e.getMessage());
        }
    }
    

}