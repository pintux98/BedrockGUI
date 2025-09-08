package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.FormPlayer;

import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles opening other forms/menus
 */
public class OpenFormActionHandler extends BaseActionHandler {
    

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
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player)), player);
        }
        
        if (formMenuUtil == null) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, null, player)), player);
        }
        
        // Validate basic parameters using base class method
        ActionResult validationResult = validateBasicParameters(player, actionValue);
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            String processedMenuName = processPlaceholders(actionValue, context, player);
            
            if (!ValidationUtils.isValidMenuName(processedMenuName)) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("menu", processedMenuName);
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, replacements, player)), player);
            }
            
            // Check if menu exists before trying to open it
            if (!formMenuUtil.hasMenu(processedMenuName)) {
                logger.warn("Attempted to open non-existent menu '" + processedMenuName + "' for player " + player.getName());
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("menu", processedMenuName);
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_FORM_NOT_FOUND, replacements, player)), player);
            }
            
            // Open the menu
            // Open the menu
            BedrockGUIApi.getInstance().openMenu(player, processedMenuName);
            
            logger.debug("Successfully opened menu '" + processedMenuName + "' for player " + player.getName());
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("menu", processedMenuName);
            return createSuccessResult("ACTION_SUCCESS", createReplacements("message", messageData.getValueNoPrefix(MessageData.ACTION_FORM_OPENED, replacements, player)), player);
            
        } catch (Exception e) {
            logger.error("Error opening menu '" + actionValue + "' for player " + player.getName(), e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player)), player, e);
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
     * @param player the player for PlaceholderAPI processing
     * @return the processed menu name
     */
    protected String processPlaceholders(String menuName, ActionContext context, FormPlayer player) {
        if (context == null) {
            return menuName;
        }
        
        String result = PlaceholderUtil.processDynamicPlaceholders(menuName, context.getPlaceholders());
        result = PlaceholderUtil.processFormResults(result, context.getFormResults());
        
        // Process PlaceholderAPI placeholders if MessageData is available
        if (context.getMetadata() != null && context.getMetadata().containsKey("messageData")) {
            Object messageData = context.getMetadata().get("messageData");
            result = PlaceholderUtil.processPlaceholders(result, player, messageData);
        }
        
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