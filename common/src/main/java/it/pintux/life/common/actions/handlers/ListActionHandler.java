package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.actions.ActionRegistry;

import java.util.*;
import java.util.logging.Logger;

/**
 * Action handler for creating paginated lists from placeholder data
 * Works only with SIMPLE forms to generate dynamic buttons with pagination
 */
public class ListActionHandler implements ActionHandler {
    
    private static final Logger logger = Logger.getLogger(ListActionHandler.class.getName());
    
    /**
     * Registers this action handler with the ActionRegistry
     */
    public static void register() {
        ActionRegistry.getInstance().registerHandler(new ListActionHandler());
    }
    
    @Override
    public String getActionType() {
        return "list";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        try {
            logger.info("Execute Debug - Received actionValue: " + actionValue);
            Map<String, String> params = parseActionValue(actionValue);
            logger.info("Execute Debug - Parsed params: " + params);
            
            int page = Integer.parseInt(params.getOrDefault("page", "1"));
            logger.info("Execute Debug - Extracted page: " + page);
            int perPage = Integer.parseInt(params.getOrDefault("per_page", "5"));
            String buttonTemplate = params.getOrDefault("button_template", "{name}");
            String buttonAction = params.getOrDefault("button_action", "message:Selected {name}");
            String formId = params.get("form_id");
            
            if (formId == null || formId.isEmpty()) {
                return ActionResult.failure(getErrorMessage("ACTION_INVALID_PARAMETERS", "form_id", "Form ID parameter is required"));
            }
            
            // Get data from various sources
            List<Map<String, String>> items = getDataFromSource(params, player);
            
            if (items == null) {
                return ActionResult.failure(getErrorMessage("ACTION_INVALID_PARAMETERS", "data_source", "No valid data source specified (placeholder, static_list, or file)"));
            }
            
            if (items.isEmpty()) {
                return ActionResult.failure(getErrorMessage("ACTION_LIST_NO_DATA", "items", "No valid items found in placeholder data"));
            }
            
            // Calculate pagination
            int totalItems = items.size();
            int totalPages = (int) Math.ceil((double) totalItems / perPage);
            int startIndex = (page - 1) * perPage;
            int endIndex = Math.min(startIndex + perPage, totalItems);
            
            // Debug logging
            logger.info("Pagination Debug - Total Items: " + totalItems + ", Per Page: " + perPage + ", Total Pages: " + totalPages + ", Current Page: " + page);
            
            if (page > totalPages || page < 1) {
                return ActionResult.failure(getErrorMessage("ACTION_LIST_INVALID_PAGE", "page", "Invalid page number"));
            }
            
            // Get items for current page
            List<Map<String, String>> pageItems = items.subList(startIndex, endIndex);
            logger.info("Page Items Debug - Start Index: " + startIndex + ", End Index: " + endIndex + ", Page Items Count: " + pageItems.size());
            
            // Generate the paginated form
            generatePaginatedForm(player, formId, pageItems, page, totalPages, perPage, buttonTemplate, buttonAction, params);
            
            return ActionResult.success(getSuccessMessage("ACTION_LIST_SUCCESS", "Generated list with " + pageItems.size() + " items (page " + page + "/" + totalPages + ")"));
            
        } catch (NumberFormatException e) {
            return ActionResult.failure(getErrorMessage("ACTION_INVALID_FORMAT", "number", "Invalid number format in parameters"));
        } catch (Exception e) {
            logger.severe("Error executing list action: " + e.getMessage());
            return ActionResult.failure(getErrorMessage("ACTION_EXECUTION_ERROR", "error", "Error executing list action"));
        }
    }
    
    /**
     * Gets data from various sources based on parameters
     */
    private List<Map<String, String>> getDataFromSource(Map<String, String> params, FormPlayer player) {
        // Check for placeholder data source
        String placeholder = params.get("placeholder");
        if (placeholder != null && !placeholder.isEmpty()) {
            return getDataFromPlaceholder(placeholder, player);
        }
        
        // Check for static list data source
        String staticList = params.get("static_list");
        if (staticList != null && !staticList.isEmpty()) {
            return getDataFromStaticList(staticList);
        }
        
        // Check for file data source
        String filePath = params.get("file");
        if (filePath != null && !filePath.isEmpty()) {
            return getDataFromFile(filePath, player);
        }
        
        // Check for custom data source
        String customSource = params.get("custom_source");
        if (customSource != null && !customSource.isEmpty()) {
            return getDataFromCustomSource(customSource, params, player);
        }
        
        return null; // No valid data source found
    }
    
    /**
     * Gets data from PlaceholderAPI placeholder
     */
    private List<Map<String, String>> getDataFromPlaceholder(String placeholder, FormPlayer player) {
        try {
            String placeholderValue = PlaceholderUtil.processPlaceholders(placeholder, new HashMap<>(), player, BedrockGUIApi.getInstance().getMessageData());
            
            if (placeholderValue == null || placeholderValue.isEmpty()) {
                return new ArrayList<>();
            }
            
            return parseListData(placeholderValue);
        } catch (Exception e) {
            logger.warning("Error processing placeholder: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets data from static list parameter
     * Format: "item1:value1,item2:value2" or "item1,item2,item3"
     */
    private List<Map<String, String>> getDataFromStaticList(String staticList) {
        return parseListData(staticList);
    }
    
    /**
     * Gets data from file (supports various formats)
     */
    private List<Map<String, String>> getDataFromFile(String filePath, FormPlayer player) {
        List<Map<String, String>> items = new ArrayList<>();
        
        try {
            // For now, return empty list - file reading can be implemented later
            // This would require proper file handling and security considerations
            logger.info("File data source not yet implemented: " + filePath);
        } catch (Exception e) {
            logger.warning("Error reading file data: " + e.getMessage());
        }
        
        return items;
    }
    
    /**
     * Gets data from custom source (extensible for future implementations)
     */
    private List<Map<String, String>> getDataFromCustomSource(String customSource, Map<String, String> params, FormPlayer player) {
        List<Map<String, String>> items = new ArrayList<>();
        
        switch (customSource.toLowerCase()) {
            case "online_players":
                return getOnlinePlayersData();
            case "server_info":
                return getServerInfoData();
            case "permissions":
                return getPlayerPermissionsData(player);
            default:
                logger.warning("Unknown custom source: " + customSource);
                break;
        }
        
        return items;
    }
    
    /**
     * Gets online players data
     */
    private List<Map<String, String>> getOnlinePlayersData() {
        return BedrockGUIApi.getInstance().getDataProvider().getOnlinePlayersData();
    }
    
    /**
     * Gets server information data
     */
    private List<Map<String, String>> getServerInfoData() {
        return BedrockGUIApi.getInstance().getDataProvider().getServerInfoData();
    }
    
    /**
     * Gets player permissions data
     */
    private List<Map<String, String>> getPlayerPermissionsData(FormPlayer player) {
        return BedrockGUIApi.getInstance().getDataProvider().getPlayerPermissionsData(player);
    }
    
    /**
     * Helper method to create data items
     */
    private Map<String, String> createDataItem(String name, String value, String description) {
        Map<String, String> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        item.put("description", description);
        return item;
    }
    
    /**
     * Parses list data from string value (JSON-like format only)
     */
    private List<Map<String, String>> parseListData(String data) {
        List<Map<String, String>> items = new ArrayList<>();
        
        if (data.trim().isEmpty()) {
            return items;
        }
        
        // Only support JSON-like format: "[{name:Apple,value:fruit},{name:Orange,value:fruit}]"
        if (data.trim().startsWith("[") && data.trim().endsWith("]")) {
            return parseJsonLikeListData(data);
        } else {
            logger.warning("Invalid list data format. Expected JSON-like format: [{name:value,value:data},...]");
            return items;
        }
    }
    
    /**
     * Parses JSON-like list data format
     * Format: "[{name:Apple,value:fruit},{name:Orange,value:fruit}]"
     */
    private List<Map<String, String>> parseJsonLikeListData(String data) {
        List<Map<String, String>> items = new ArrayList<>();
        
        try {
            // Remove outer brackets
            String content = data.trim().substring(1, data.trim().length() - 1);
            
            // Split by },{ to get individual objects
            String[] objects = content.split("\\},\\s*\\{");
            
            for (int i = 0; i < objects.length; i++) {
                String obj = objects[i].trim();
                
                // Remove leading/trailing braces
                if (obj.startsWith("{")) {
                    obj = obj.substring(1);
                }
                if (obj.endsWith("}")) {
                    obj = obj.substring(0, obj.length() - 1);
                }
                
                Map<String, String> item = new HashMap<>();
                
                // Parse key:value pairs
                String[] pairs = obj.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.trim().split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        item.put(key, value);
                    }
                }
                
                // Ensure required fields
                if (!item.containsKey("name")) {
                    item.put("name", "Item " + (i + 1));
                }
                if (!item.containsKey("value")) {
                    item.put("value", item.get("name"));
                }
                
                // Add uuid for backward compatibility
                item.put("uuid", item.get("value"));
                item.put("index", String.valueOf(items.size() + 1));
                
                items.add(item);
            }
        } catch (Exception e) {
            logger.warning("Error parsing JSON-like list data: " + e.getMessage());
        }
        
        return items;
    }
    
    /**
     * Generates a paginated form with the given items
     */
    private void generatePaginatedForm(FormPlayer player, String formId, List<Map<String, String>> items, 
                                     int currentPage, int totalPages, int perPage, 
                                     String buttonTemplate, String buttonAction, Map<String, String> params) {
        
        BedrockGUIApi api = BedrockGUIApi.getInstance();
        
        // Create form title with pagination info
        String title = params.getOrDefault("title", "List (Page {page}/{total_pages})");
        title = title.replace("{page}", String.valueOf(currentPage))
                    .replace("{total_pages}", String.valueOf(totalPages));
        
        // Create simple form builder
        BedrockGUIApi.SimpleFormBuilder builder = api.createSimpleForm(title);
        
        // Add description if provided
        String description = params.get("description");
        if (description != null) {
            description = description.replace("{page}", String.valueOf(currentPage))
                                  .replace("{total_pages}", String.valueOf(totalPages))
                                  .replace("{total_items}", String.valueOf(items.size()));
            builder.content(description);
        }
        
        // Add buttons for each item
        for (Map<String, String> item : items) {
            String buttonText = processTemplate(buttonTemplate, item);
            String action = processTemplate(buttonAction, item);
            builder.button(buttonText, formPlayer -> {
                // Parse action to get type and value
                String[] actionParts = action.split(":", 2);
                String actionType = actionParts[0];
                String actionValue = actionParts.length > 1 ? actionParts[1] : "";
                
                ActionContext ctx = ActionContext.builder()
                    .placeholders(item)
                    .build();
                BedrockGUIApi.getInstance().executeAction(formPlayer, actionType, actionValue, ctx);
            });
        }
        
        // Add navigation buttons only if there are multiple pages
        logger.info("Pagination Button Debug - Total Pages: " + totalPages + ", Current Page: " + currentPage);
        if (totalPages > 1) {
            logger.info("Adding pagination buttons because totalPages (" + totalPages + ") > 1");
            
            // Get button texts from params or config defaults
            String prevButtonText = params.getOrDefault("previous_button_text", 
                BedrockGUIApi.getInstance().getFormMenuUtil().getConfig().getString("settings.pagination.default_previous_text", "⬅ Previous Page"));
            String nextButtonText = params.getOrDefault("next_button_text", 
                BedrockGUIApi.getInstance().getFormMenuUtil().getConfig().getString("settings.pagination.default_next_text", "➡ Next Page"));
            
            // Get button order from config
            String buttonOrder = BedrockGUIApi.getInstance().getFormMenuUtil().getConfig().getString("settings.pagination.button_order", "previous_first");
            
            // Create button actions
            Runnable addPrevButton = () -> {
                if (currentPage > 1) {
                    logger.info("Adding Previous Page button because currentPage (" + currentPage + ") > 1");
                    String prevAction = "list:" + buildActionValue(params, currentPage - 1);
                    builder.button(prevButtonText, formPlayer -> {
                        String[] actionParts = prevAction.split(":", 2);
                        String actionType = actionParts[0];
                        String actionValue = actionParts.length > 1 ? actionParts[1] : "";
                        
                        ActionContext ctx = ActionContext.builder()
                            .placeholders(new HashMap<>())
                            .build();
                        BedrockGUIApi.getInstance().executeAction(formPlayer, actionType, actionValue, ctx);
                    });
                }
            };
            
            Runnable addNextButton = () -> {
                if (currentPage < totalPages) {
                    logger.info("Adding Next Page button because currentPage (" + currentPage + ") < totalPages (" + totalPages + ")");
                    String nextAction = "list:" + buildActionValue(params, currentPage + 1);
                    builder.button(nextButtonText, formPlayer -> {
                        String[] actionParts = nextAction.split(":", 2);
                        String actionType = actionParts[0];
                        String actionValue = actionParts.length > 1 ? actionParts[1] : "";
                        
                        ActionContext ctx = ActionContext.builder()
                            .placeholders(new HashMap<>())
                            .build();
                        BedrockGUIApi.getInstance().executeAction(formPlayer, actionType, actionValue, ctx);
                    });
                }
            };
            
            // Add buttons in the configured order
            if ("next_first".equals(buttonOrder)) {
                addNextButton.run();
                addPrevButton.run();
            } else {
                addPrevButton.run();
                addNextButton.run();
            }
        }
        
        // Send the form
        builder.send(player);
    }
    
    /**
     * Processes a template string with item data
     */
    private String processTemplate(String template, Map<String, String> item) {
        String result = template;
        for (Map.Entry<String, String> entry : item.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        // Add support for {item} placeholder as alias for {name}
        if (item.containsKey("name")) {
            result = result.replace("{item}", item.get("name"));
        }
        
        return result;
    }
    
    /**
     * Builds action value string for pagination
     */
    private String buildActionValue(Map<String, String> params, int newPage) {
        StringBuilder sb = new StringBuilder();
        
        // First, add list type prefix if present
        if (params.containsKey("placeholder")) {
            sb.append("placeholder:").append(params.get("placeholder")).append(",");
        } else if (params.containsKey("custom_source")) {
            sb.append("custom_source:").append(params.get("custom_source")).append(",");
        }
        
        // Add the page parameter first
        if (sb.length() > 0 && !sb.toString().endsWith(",")) sb.append(",");
        sb.append("page:").append(newPage);
        
        // Then add other parameters
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            // Skip list type parameters as they're already added above, and skip page as we already added it
            if ("placeholder".equals(key) || "custom_source".equals(key) || "page".equals(key)) {
                continue;
            }
            
            if (sb.length() > 0 && !sb.toString().endsWith(",")) sb.append(",");
            if ("static_list".equals(key) && entry.getValue().startsWith("[")) {
                // Handle JSON format - don't add colon after static_list
                sb.append("static_list:").append(entry.getValue());
            } else {
                sb.append(key).append(":").append(entry.getValue());
            }
        }
        return sb.toString();
    }
    
    /**
     * Parses action value into parameters
     */
    private Map<String, String> parseActionValue(String actionValue) {
        Map<String, String> params = new HashMap<>();
        
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return params;
        }
        
        // Check for list type at the beginning (placeholder:, custom_source:, etc.)
        String processedValue = actionValue;
        if (actionValue.startsWith("placeholder:")) {
            // Extract placeholder value and remaining parameters
            String remaining = actionValue.substring(12); // Remove "placeholder:"
            int commaIndex = remaining.indexOf(",");
            if (commaIndex != -1) {
                params.put("placeholder", remaining.substring(0, commaIndex));
                processedValue = remaining.substring(commaIndex + 1);
            } else {
                params.put("placeholder", remaining);
                return params;
            }
        } else if (actionValue.startsWith("custom_source:")) {
            // Extract custom_source value and remaining parameters
            String remaining = actionValue.substring(14); // Remove "custom_source:"
            int commaIndex = remaining.indexOf(",");
            if (commaIndex != -1) {
                params.put("custom_source", remaining.substring(0, commaIndex));
                processedValue = remaining.substring(commaIndex + 1);
            } else {
                params.put("custom_source", remaining);
                return params;
            }
        }
        
        // Handle JSON-like static_list format specially
        if (processedValue.contains("static_list:[{")) {
            // Find the JSON array part
            int jsonStart = processedValue.indexOf("[");
            int jsonEnd = processedValue.lastIndexOf("]") + 1;
            
            if (jsonStart != -1 && jsonEnd > jsonStart) {
                String jsonPart = processedValue.substring(jsonStart, jsonEnd);
                String beforeJson = processedValue.substring(0, jsonStart);
                String afterJson = processedValue.substring(jsonEnd);
                
                // Parse the part before JSON
                if (!beforeJson.isEmpty()) {
                    String[] beforePairs = beforeJson.split(",");
                    for (String pair : beforePairs) {
                        String[] keyValue = pair.trim().split(":", 2);
                        if (keyValue.length == 2) {
                            params.put(keyValue[0].trim(), keyValue[1].trim());
                        }
                    }
                }
                
                // Add the JSON part as static_list
                params.put("static_list", jsonPart);
                
                // Parse the part after JSON
                if (!afterJson.isEmpty() && afterJson.startsWith(",")) {
                    afterJson = afterJson.substring(1); // Remove leading comma
                    String[] afterPairs = afterJson.split(",");
                    for (String pair : afterPairs) {
                        String[] keyValue = pair.trim().split(":", 2);
                        if (keyValue.length == 2) {
                            params.put(keyValue[0].trim(), keyValue[1].trim());
                        }
                    }
                }
            }
        } else {
            // Standard parsing for non-JSON formats
            String[] pairs = processedValue.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.trim().split(":", 2);
                if (keyValue.length == 2) {
                    params.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
        
        return params;
    }
    
    private String getErrorMessage(String key, String placeholder, String defaultMessage) {
        try {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> placeholders = new HashMap<>();
            placeholders.put("placeholder", placeholder);
            return messageData.getValueNoPrefix(key, placeholders, null);
        } catch (Exception e) {
            return defaultMessage;
        }
    }
    
    private String getSuccessMessage(String key, String defaultMessage) {
        try {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return messageData.getValueNoPrefix(key, new HashMap<>(), null);
        } catch (Exception e) {
            return defaultMessage;
        }
    }
    
    private String getLocalizedMessage(String key, Map<String, String> placeholders) {
        try {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> objectPlaceholders = new HashMap<>();
            if (placeholders != null) {
                objectPlaceholders.putAll(placeholders);
            }
            return messageData.getValueNoPrefix(key, objectPlaceholders, null);
        } catch (Exception e) {
            logger.warning("MessageData not available, using fallback message for key: " + key);
            return "Message not available";
        }
    }
    
    private String getLocalizedMessage(String key) {
        try {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return messageData.getValueNoPrefix(key, new HashMap<>(), null);
        } catch (Exception e) {
            logger.warning("MessageData not available, using fallback message for key: " + key);
            return "Message not available";
        }
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        return actionValue != null && !actionValue.trim().isEmpty();
    }
    
    public String getDescription() {
        return "Creates paginated lists from placeholder data for SIMPLE forms";
    }
    
    public String[] getUsageExamples() {
        return new String[]{
            // Placeholder data source
            "list:placeholder:%bgui_online_players_list%,form_id:player_list,per_page:5,title:Online Players,button_template:{name},button_action:teleport:{name}",
            
            // Static list data source
            "list:static_list:Apple:fruit,Banana:fruit,Carrot:vegetable,form_id:food_list,per_page:3,title:Food Items,button_template:{name} ({value}),button_action:message:You selected {name}",
            
            // Custom data source - online players
            "list:custom_source:online_players,form_id:online_list,per_page:10,title:Online Players,button_template:{name} - {description},button_action:teleport:{name}",
            
            // Custom data source - server info
            "list:custom_source:server_info,form_id:server_stats,per_page:5,title:Server Information,button_template:{name}: {value},button_action:message:Server {name} is {value}",
            
            // Custom data source - player permissions
            "list:custom_source:permissions,form_id:perm_list,per_page:8,title:Your Permissions,button_template:{name} ({value}),button_action:message:Permission {name} is {value}"
        };
    }
}