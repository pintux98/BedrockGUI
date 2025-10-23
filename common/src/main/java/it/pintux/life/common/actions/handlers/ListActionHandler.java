package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionDefinition;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.actions.ActionRegistry;

import java.util.*;


public class ListActionHandler extends BaseActionHandler {
    
    
    public static void register() {
        ActionRegistry.getInstance().registerHandler(new ListActionHandler());
    }
    
    @Override
    public String getActionType() {
        return "list";
    }
    
    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {
        
        ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }
        
        try {
            List<String> listConfigurations = parseActionData(actionData, context, player);
            
            if (listConfigurations.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid list configurations found");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }
            
            
            if (listConfigurations.size() == 1) {
                return executeSingleList(listConfigurations.get(0), player, context);
            }
            
            
            return executeMultipleLists(listConfigurations, player, context);
            
        } catch (Exception e) {
            logError("list generation", actionData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error generating list: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }
    
    private ActionResult executeSingleList(String listConfig, FormPlayer player, ActionContext context) {
        try {
            logger.info("Generating list: " + listConfig + " for player " + player.getName());
            
            Map<String, String> params = parseListConfiguration(listConfig);
            
            
            String formId = params.get("form_id");
            if (formId == null || formId.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "Form ID parameter is required");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }
            
            
            boolean success = executeWithErrorHandling(
                () -> generateList(params, player, context),
                "Generate list: " + formId,
                player
            );
            
            if (success) {
                logSuccess("list generation", formId, player);
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("message", "Successfully generated list: " + formId);
                replacements.put("form_id", formId);
                return createSuccessResult("ACTION_SUCCESS", replacements, player);
            } else {
                Map<String, Object> errorReplacements = new HashMap<>();
                errorReplacements.put("error", "Failed to generate list: " + formId);
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }
            
        } catch (Exception e) {
            logError("list generation", listConfig, player, e);
            Map<String, Object> errorReplacements = new HashMap<>();
            errorReplacements.put("error", "Error generating list: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }
    
    private ActionResult executeMultipleLists(List<String> listConfigs, FormPlayer player, ActionContext context) {
        int successCount = 0;
        int totalCount = listConfigs.size();
        StringBuilder results = new StringBuilder();
        
        for (int i = 0; i < listConfigs.size(); i++) {
            String listConfig = listConfigs.get(i);
            
            try {
                logger.info("Generating list " + (i + 1) + "/" + totalCount + ": " + listConfig + " for player " + player.getName());
                
                Map<String, String> params = parseListConfiguration(listConfig);
                String formId = params.get("form_id");
                
                if (formId == null || formId.isEmpty()) {
                    results.append("âś— List ").append(i + 1).append(": ").append(listConfig).append(" - Missing form_id");
                    continue;
                }
                
                boolean success = executeWithErrorHandling(
                    () -> generateList(params, player, context),
                    "Generate list: " + formId,
                    player
                );
                
                if (success) {
                    successCount++;
                    results.append("âś“ List ").append(i + 1).append(": ").append(formId).append(" - Success");
                    logSuccess("list generation", formId, player);
                } else {
                    results.append("âś— List ").append(i + 1).append(": ").append(formId).append(" - Failed");
                }
                
                if (i < listConfigs.size() - 1) {
                    results.append("\n");
                    
                    try {
                        Thread.sleep(300); 
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
            } catch (Exception e) {
                results.append("âś— List ").append(i + 1).append(": ").append(listConfig).append(" - Error: ").append(e.getMessage());
                logError("list generation", listConfig, player, e);
                if (i < listConfigs.size() - 1) {
                    results.append("\n");
                }
            }
        }
        
        String finalMessage = String.format("Generated %d/%d lists successfully:\n%s", 
            successCount, totalCount, results.toString());
        
        Map<String, Object> replacements = new HashMap<>();
        replacements.put("message", finalMessage);
        replacements.put("success_count", successCount);
        replacements.put("total_count", totalCount);
        
        if (successCount == totalCount) {
            return createSuccessResult("ACTION_SUCCESS", replacements, player);
        } else if (successCount > 0) {
            return createSuccessResult("ACTION_PARTIAL_SUCCESS", replacements, player);
        } else {
            return createFailureResult("ACTION_EXECUTION_ERROR", replacements, player);
        }
    }
    
    private boolean generateList(Map<String, String> params, FormPlayer player, ActionContext context) {
        try {
            int page = Integer.parseInt(params.getOrDefault("page", "1"));
            int perPage = Integer.parseInt(params.getOrDefault("per_page", "5"));
            String buttonTemplate = params.getOrDefault("button_template", "{name}");
            String buttonAction = params.getOrDefault("button_action", "message:Selected {name}");
            String formId = params.get("form_id");
            
            
            List<Map<String, String>> items = getDataFromSource(params, player);
            
            if (items == null) {
                logger.warn("No valid data source specified for list: " + formId);
                return false;
            }
            
            if (items.isEmpty()) {
                logger.warn("No valid items found in data source for list: " + formId);
                return false;
            }
            
            
            int totalItems = items.size();
            int totalPages = (int) Math.ceil((double) totalItems / perPage);
            int startIndex = (page - 1) * perPage;
            int endIndex = Math.min(startIndex + perPage, totalItems);
            
            if (page > totalPages || page < 1) {
                logger.warn("Invalid page number " + page + " for list: " + formId);
                return false;
            }
            
            
            List<Map<String, String>> pageItems = items.subList(startIndex, endIndex);
            
            
            generatePaginatedForm(player, formId, pageItems, page, totalPages, perPage, buttonTemplate, buttonAction, params);
            
            return true;
            
        } catch (NumberFormatException e) {
            logger.error("Invalid number format in list parameters: " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Error generating list: " + e.getMessage(), e);
            return false;
        }
    }
    
    private Map<String, String> parseListConfiguration(String listConfig) {
        Map<String, String> params = new HashMap<>();
        
        if (listConfig == null || listConfig.trim().isEmpty()) {
            return params;
        }
        
        
        String processedValue = listConfig;
        if (listConfig.startsWith("placeholder:")) {
            
            String remaining = listConfig.substring(12); 
            int commaIndex = remaining.indexOf(",");
            if (commaIndex != -1) {
                params.put("placeholder", remaining.substring(0, commaIndex));
                processedValue = remaining.substring(commaIndex + 1);
            } else {
                params.put("placeholder", remaining);
                return params;
            }
        } else if (listConfig.startsWith("custom_source:")) {
            
            String remaining = listConfig.substring(14); 
            int commaIndex = remaining.indexOf(",");
            if (commaIndex != -1) {
                params.put("custom_source", remaining.substring(0, commaIndex));
                processedValue = remaining.substring(commaIndex + 1);
            } else {
                params.put("custom_source", remaining);
                return params;
            }
        }
        
        
        if (processedValue.contains("static_list:[{")) {
            
            int jsonStart = processedValue.indexOf("[");
            int jsonEnd = processedValue.lastIndexOf("]") + 1;
            
            if (jsonStart != -1 && jsonEnd > jsonStart) {
                String jsonPart = processedValue.substring(jsonStart, jsonEnd);
                String beforeJson = processedValue.substring(0, jsonStart);
                String afterJson = processedValue.substring(jsonEnd);
                
                
                if (!beforeJson.isEmpty()) {
                    String[] beforePairs = beforeJson.split(",");
                    for (String pair : beforePairs) {
                        String[] keyValue = pair.trim().split(":", 2);
                        if (keyValue.length == 2) {
                            params.put(keyValue[0].trim(), keyValue[1].trim());
                        }
                    }
                }
                
                
                params.put("static_list", jsonPart);
                
                
                if (!afterJson.isEmpty() && afterJson.startsWith(",")) {
                    afterJson = afterJson.substring(1); 
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
    
    
    private List<Map<String, String>> getDataFromSource(Map<String, String> params, FormPlayer player) {
        
        String placeholder = params.get("placeholder");
        if (placeholder != null && !placeholder.isEmpty()) {
            return getDataFromPlaceholder(placeholder, player);
        }
        
        
        String staticList = params.get("static_list");
        if (staticList != null && !staticList.isEmpty()) {
            return getDataFromStaticList(staticList);
        }
        
        
        String filePath = params.get("file");
        if (filePath != null && !filePath.isEmpty()) {
            return getDataFromFile(filePath, player);
        }
        
        
        String customSource = params.get("custom_source");
        if (customSource != null && !customSource.isEmpty()) {
            return getDataFromCustomSource(customSource, params, player);
        }
        
        return null; 
    }
    
    
    private List<Map<String, String>> getDataFromPlaceholder(String placeholder, FormPlayer player) {
        try {
            String placeholderValue = processPlaceholders(placeholder, null, player);
            
            if (placeholderValue == null || placeholderValue.isEmpty()) {
                return new ArrayList<>();
            }
            
            return parseListData(placeholderValue);
        } catch (Exception e) {
            logger.warn("Error processing placeholder: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    
    private List<Map<String, String>> getDataFromStaticList(String staticList) {
        return parseListData(staticList);
    }
    
    
    private List<Map<String, String>> getDataFromFile(String filePath, FormPlayer player) {
        List<Map<String, String>> items = new ArrayList<>();
        
        try {
            
            
            logger.info("File data source not yet implemented: " + filePath);
        } catch (Exception e) {
            logger.warn("Error reading file data: " + e.getMessage());
        }
        
        return items;
    }
    
    
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
                logger.warn("Unknown custom source: " + customSource);
                break;
        }
        
        return items;
    }
    
    
    private List<Map<String, String>> getOnlinePlayersData() {
        return BedrockGUIApi.getInstance().getDataProvider().getOnlinePlayersData();
    }
    
    
    private List<Map<String, String>> getServerInfoData() {
        return BedrockGUIApi.getInstance().getDataProvider().getServerInfoData();
    }
    
    
    private List<Map<String, String>> getPlayerPermissionsData(FormPlayer player) {
        return BedrockGUIApi.getInstance().getDataProvider().getPlayerPermissionsData(player);
    }
    
    
    private Map<String, String> createDataItem(String name, String value, String description) {
        Map<String, String> item = new HashMap<>();
        item.put("name", name);
        item.put("value", value);
        item.put("description", description);
        return item;
    }
    
    
    private List<Map<String, String>> parseListData(String data) {
        List<Map<String, String>> items = new ArrayList<>();
        
        if (data.trim().isEmpty()) {
            return items;
        }
        
        
        if (data.trim().startsWith("[") && data.trim().endsWith("]")) {
            return parseJsonLikeListData(data);
        } else {
            logger.warn("Invalid list data format. Expected JSON-like format: [{name:value,value:data},...]");
            return items;
        }
    }
    
    
    private List<Map<String, String>> parseJsonLikeListData(String data) {
        List<Map<String, String>> items = new ArrayList<>();
        
        try {
            
            String content = data.trim().substring(1, data.trim().length() - 1);
            
            
            String[] objects = content.split("\\},\\s*\\{");
            
            for (int i = 0; i < objects.length; i++) {
                String obj = objects[i].trim();
                
                
                if (obj.startsWith("{")) {
                    obj = obj.substring(1);
                }
                if (obj.endsWith("}")) {
                    obj = obj.substring(0, obj.length() - 1);
                }
                
                Map<String, String> item = new HashMap<>();
                
                
                String[] pairs = obj.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.trim().split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        item.put(key, value);
                    }
                }
                
                
                if (!item.containsKey("name")) {
                    item.put("name", "Item " + (i + 1));
                }
                if (!item.containsKey("value")) {
                    item.put("value", item.get("name"));
                }
                
                
                item.put("uuid", item.get("value"));
                item.put("index", String.valueOf(items.size() + 1));
                
                items.add(item);
            }
        } catch (Exception e) {
            logger.warn("Error parsing JSON-like list data: " + e.getMessage());
        }
        
        return items;
    }
    
    
    private void generatePaginatedForm(FormPlayer player, String formId, List<Map<String, String>> items, 
                                     int currentPage, int totalPages, int perPage, 
                                     String buttonTemplate, String buttonAction, Map<String, String> params) {
        
        BedrockGUIApi api = BedrockGUIApi.getInstance();
        
        
        String title = params.getOrDefault("title", "List (Page {page}/{total_pages})");
        title = title.replace("{page}", String.valueOf(currentPage))
                    .replace("{total_pages}", String.valueOf(totalPages));
        
        
        BedrockGUIApi.SimpleFormBuilder builder = api.createSimpleForm(title);
        
        
        String description = params.get("description");
        if (description != null) {
            description = description.replace("{page}", String.valueOf(currentPage))
                                  .replace("{total_pages}", String.valueOf(totalPages))
                                  .replace("{total_items}", String.valueOf(items.size()));
            builder.content(description);
        }
        
        
        for (Map<String, String> item : items) {
            String buttonText = processTemplate(buttonTemplate, item);
            String action = processTemplate(buttonAction, item);
            builder.button(buttonText, formPlayer -> {
                
                String[] actionParts = action.split(":", 2);
                String actionType = actionParts[0];
                String actionValue = actionParts.length > 1 ? actionParts[1] : "";
                
                ActionContext ctx = ActionContext.builder()
                    .placeholders(item)
                    .build();
                ActionDefinition actionDef = new ActionDefinition();
                actionDef.addAction(actionType, actionValue);
                BedrockGUIApi.getInstance().executeAction(formPlayer, actionDef, ctx);
            });
        }
        
        
        if (totalPages > 1) {
            
            String prevButtonText = params.getOrDefault("previous_button_text", 
                BedrockGUIApi.getInstance().getFormMenuUtil().getConfig().getString("settings.pagination.default_previous_text", " Previous Page"));
            String nextButtonText = params.getOrDefault("next_button_text", 
                BedrockGUIApi.getInstance().getFormMenuUtil().getConfig().getString("settings.pagination.default_next_text", " Next Page"));
            
            
            String buttonOrder = BedrockGUIApi.getInstance().getFormMenuUtil().getConfig().getString("settings.pagination.button_order", "previous_first");
            
            
            Runnable addPrevButton = () -> {
                if (currentPage > 1) {
                    String prevAction = "list:" + buildActionValue(params, currentPage - 1);
                    builder.button(prevButtonText, formPlayer -> {
                        String[] actionParts = prevAction.split(":", 2);
                        String actionType = actionParts[0];
                        String actionValue = actionParts.length > 1 ? actionParts[1] : "";
                        
                        ActionContext ctx = ActionContext.builder()
                            .placeholders(new HashMap<>())
                            .build();
                        ActionDefinition actionDef = new ActionDefinition();
                        actionDef.addAction(actionType, actionValue);
                        BedrockGUIApi.getInstance().executeAction(formPlayer, actionDef, ctx);
                    });
                }
            };
            
            Runnable addNextButton = () -> {
                if (currentPage < totalPages) {
                    String nextAction = "list:" + buildActionValue(params, currentPage + 1);
                    builder.button(nextButtonText, formPlayer -> {
                        String[] actionParts = nextAction.split(":", 2);
                        String actionType = actionParts[0];
                        String actionValue = actionParts.length > 1 ? actionParts[1] : "";
                        
                        ActionContext ctx = ActionContext.builder()
                            .placeholders(new HashMap<>())
                            .build();
                        ActionDefinition actionDef = new ActionDefinition();
                        actionDef.addAction(actionType, actionValue);
                        BedrockGUIApi.getInstance().executeAction(formPlayer, actionDef, ctx);
                    });
                }
            };
            
            
            if ("next_first".equals(buttonOrder)) {
                addNextButton.run();
                addPrevButton.run();
            } else {
                addPrevButton.run();
                addNextButton.run();
            }
        }
        
        
        builder.send(player);
    }
    
    
    private String processTemplate(String template, Map<String, String> item) {
        String result = template;
        for (Map.Entry<String, String> entry : item.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        
        if (item.containsKey("name")) {
            result = result.replace("{item}", item.get("name"));
        }
        
        return result;
    }
    
    
    private String buildActionValue(Map<String, String> params, int newPage) {
        StringBuilder sb = new StringBuilder();
        
        
        if (params.containsKey("placeholder")) {
            sb.append("placeholder:").append(params.get("placeholder")).append(",");
        } else if (params.containsKey("custom_source")) {
            sb.append("custom_source:").append(params.get("custom_source")).append(",");
        }
        
        
        if (sb.length() > 0 && !sb.toString().endsWith(",")) sb.append(",");
        sb.append("page:").append(newPage);
        
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            
            if ("placeholder".equals(key) || "custom_source".equals(key) || "page".equals(key)) {
                continue;
            }
            
            if (sb.length() > 0 && !sb.toString().endsWith(",")) sb.append(",");
            if ("static_list".equals(key) && entry.getValue().startsWith("[")) {
                
                sb.append("static_list:").append(entry.getValue());
            } else {
                sb.append(key).append(":").append(entry.getValue());
            }
        }
        return sb.toString();
    }
    
    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        
        
        List<String> configurations = parseActionDataForValidation(actionValue);
        
        for (String config : configurations) {
            if (!isValidListConfiguration(config)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isValidListConfiguration(String config) {
        if (config == null || config.trim().isEmpty()) {
            return false;
        }
        
        
        return config.contains("form_id:");
    }
    
    @Override
    public String getDescription() {
        return "Creates paginated lists from placeholder data for SIMPLE forms. Supports multiple list operations with sequential execution and enhanced error handling.";
    }
    
    @Override
    public String[] getUsageExamples() {
        return new String[]{
            
            "placeholder:%bgui_online_players_list%,form_id:player_list,per_page:5,title:Online Players,button_template:{name},button_action:teleport:{name} - Online players list",
            "static_list:[{name:Apple,value:fruit},{name:Banana,value:fruit}],form_id:food_list,per_page:3,title:Food Items,button_template:{name} ({value}),button_action:message:You selected {name} - Static food list",
            "custom_source:online_players,form_id:online_list,per_page:10,title:Online Players,button_template:{name} - {description},button_action:teleport:{name} - Custom online players source",
            "custom_source:server_info,form_id:server_stats,per_page:5,title:Server Information,button_template:{name}: {value},button_action:message:Server {name} is {value} - Server info list",
            "custom_source:permissions,form_id:perm_list,per_page:8,title:Your Permissions,button_template:{name} ({value}),button_action:message:Permission {name} is {value} - Player permissions list",
            
            
            "[\"placeholder:%players%,form_id:players_list,title:Players\", \"placeholder:%warps%,form_id:warps_list,title:Warps\"] - Multiple placeholder lists",
            "[\"custom_source:online_players,form_id:online,title:Online\", \"custom_source:server_info,form_id:info,title:Server Info\"] - Multiple custom source lists",
            "[\"static_list:[{name:PvP,value:pvp}],form_id:pvp_list\", \"static_list:[{name:Creative,value:creative}],form_id:creative_list\"] - Multiple static lists"
        };
    }
}

