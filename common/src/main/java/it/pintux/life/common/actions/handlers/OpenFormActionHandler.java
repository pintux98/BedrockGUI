package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.Map;

/**
 * Handles opening other forms/menus
 */
public class OpenFormActionHandler implements ActionHandler {
    
    private static final Logger logger = Logger.getLogger(OpenFormActionHandler.class);
    private final FormMenuUtil formMenuUtil;
    
    public OpenFormActionHandler(FormMenuUtil formMenuUtil) {
        this.formMenuUtil = formMenuUtil;
    }
    
    @Override
    public String getActionType() {
        return "open";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null) {
            return ActionResult.failure("Player cannot be null");
        }
        
        if (formMenuUtil == null) {
            return ActionResult.failure("FormMenuUtil not available");
        }
        
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return ActionResult.failure("Menu name cannot be null or empty");
        }
        
        try {
            String processedMenuName = processPlaceholders(actionValue, context);
            
            if (!ValidationUtils.isValidMenuName(processedMenuName)) {
                return ActionResult.failure("Invalid menu name: " + processedMenuName);
            }
            
            // Check if menu exists before trying to open it
            if (!formMenuUtil.hasMenu(processedMenuName)) {
                logger.warn("Attempted to open non-existent menu '" + processedMenuName + "' for player " + player.getName());
                return ActionResult.failure("Menu '" + processedMenuName + "' does not exist");
            }
            
            // Open the menu
            formMenuUtil.openMenu(player, processedMenuName);
            
            logger.debug("Successfully opened menu '" + processedMenuName + "' for player " + player.getName());
            return ActionResult.success("Menu opened successfully");
            
        } catch (Exception e) {
            logger.error("Error opening menu '" + actionValue + "' for player " + player.getName(), e);
            return ActionResult.failure("Failed to open menu: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return false;
        }
        
        String trimmed = actionValue.trim();
        
        // Basic menu name validation
        return ValidationUtils.isValidMenuName(trimmed) || containsPlaceholders(trimmed);
    }
    
    @Override
    public String getDescription() {
        return "Opens another form/menu. Supports placeholders for dynamic menu names.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            "main_menu",
            "shop_menu",
            "player_settings",
            "{dynamic_menu_name}",
            "category_{selected_category}"
        };
    }
    
    /**
     * Processes placeholders in the menu name
     * @param menuName the menu name with placeholders
     * @param context the action context containing placeholder values
     * @return the processed menu name
     */
    private String processPlaceholders(String menuName, ActionContext context) {
        if (context == null) {
            return menuName;
        }
        
        String result = PlaceholderUtil.processDynamicPlaceholders(menuName, context.getPlaceholders());
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