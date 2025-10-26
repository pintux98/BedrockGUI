package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }

        if (formMenuUtil == null) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> errorReplacements = createReplacements("error", "FormMenuUtil not available");
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
        }

        try {
            List<String> menuNames;
            
            // Check if it's the new YAML format with curly braces
            if (actionData.trim().startsWith("{") && actionData.trim().endsWith("}")) {
                List<String> rawMenuNames = parseNewFormatValues(actionData);
                menuNames = new ArrayList<>();
                for (String menuName : rawMenuNames) {
                    String processedMenuName = processPlaceholders(menuName, context, player);
                    menuNames.add(processedMenuName);
                }
            } else {
                // Legacy format support
                menuNames = parseActionData(actionData, context, player);
            }

            if (menuNames.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid menu names found");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }

            // Single form opening
            if (menuNames.size() == 1) {
                return executeSingleFormOpen(menuNames.get(0), player);
            }

            // Multiple form opening
            return executeMultipleFormOpens(menuNames, player);

        } catch (Exception e) {
            logError("form opening", actionData, player, e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> errorReplacements = new HashMap<>();
            errorReplacements.put("error", e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }

    private ActionSystem.ActionResult executeSingleFormOpen(String menuName, FormPlayer player) {
        try {
            logger.info("Opening form: " + menuName + " for player " + player.getName());


            if (!ValidationUtils.isValidMenuName(menuName)) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> errorReplacements = new HashMap<>();
                errorReplacements.put("menu", menuName);
                return createFailureResult("ACTION_INVALID_FORMAT", errorReplacements, player);
            }


            if (!formMenuUtil.hasMenu(menuName)) {
                logger.warn("Attempted to open non-existent menu '" + menuName + "' for player " + player.getName());
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> errorReplacements = new HashMap<>();
                errorReplacements.put("menu", menuName);
                return createFailureResult("ACTION_FORM_NOT_FOUND", errorReplacements, player);
            }


            boolean success = executeWithErrorHandling(
                    () -> {
                        BedrockGUIApi.getInstance().openMenu(player, menuName);
                        return true;
                    },
                    "Open form: " + menuName,
                    player
            );

            if (success) {
                logSuccess("form opening", menuName, player);
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("menu", menuName);
                replacements.put("message", "Successfully opened form: " + menuName);
                return createSuccessResult("ACTION_SUCCESS", replacements, player);
            } else {
                Map<String, Object> errorReplacements = new HashMap<>();
                errorReplacements.put("error", "Failed to open form: " + menuName);
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }

        } catch (Exception e) {
            logError("form opening", menuName, player, e);
            Map<String, Object> errorReplacements = new HashMap<>();
            errorReplacements.put("error", "Error opening form: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }

    private ActionSystem.ActionResult executeMultipleFormOpens(List<String> menuNames, FormPlayer player) {
        int successCount = 0;
        int totalCount = menuNames.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < menuNames.size(); i++) {
            String menuName = menuNames.get(i);

            try {
                logger.info("Opening form " + (i + 1) + "/" + totalCount + ": " + menuName + " for player " + player.getName());


                if (!ValidationUtils.isValidMenuName(menuName)) {
                    results.append("âś— Form ").append(i + 1).append(": ").append(menuName).append(" - Invalid name");
                    continue;
                }


                if (!formMenuUtil.hasMenu(menuName)) {
                    results.append("âś— Form ").append(i + 1).append(": ").append(menuName).append(" - Not found");
                    continue;
                }

                boolean success = executeWithErrorHandling(
                        () -> {
                            BedrockGUIApi.getInstance().openMenu(player, menuName);
                            return true;
                        },
                        "Open form: " + menuName,
                        player
                );

                if (success) {
                    successCount++;
                    results.append("âś“ Form ").append(i + 1).append(": ").append(menuName).append(" - Success");
                    logSuccess("form opening", menuName, player);
                } else {
                    results.append("âś— Form ").append(i + 1).append(": ").append(menuName).append(" - Failed");
                }

                if (i < menuNames.size() - 1) {
                    results.append("\n");

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (Exception e) {
                results.append("âś— Form ").append(i + 1).append(": ").append(menuName).append(" - Error: ").append(e.getMessage());
                logError("form opening", menuName, player, e);
                if (i < menuNames.size() - 1) {
                    results.append("\n");
                }
            }
        }

        String finalMessage = String.format("Opened %d/%d forms successfully:\n%s",
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

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionValue.trim();
        
        // Support new YAML format
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                List<String> menuNames = parseNewFormatValues(trimmed);
                for (String menuName : menuNames) {
                    if (!isValidMenuName(menuName)) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        // Legacy format support
        List<String> menuNames = parseActionDataForValidation(actionValue);

        for (String menuName : menuNames) {
            if (!isValidMenuName(menuName)) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidMenuName(String menuName) {
        if (ValidationUtils.isNullOrEmpty(menuName)) {
            return false;
        }

        String trimmed = menuName.trim();


        if (containsDynamicPlaceholders(trimmed)) {
            return true;
        }


        return ValidationUtils.isValidMenuName(trimmed);
    }

    @Override
    public String getDescription() {
        return "Opens other forms/menus with support for multiple sequential form operations. Supports placeholders for dynamic menu names and form validation.";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "New Format Examples:",
                "open { - \"main_menu\" }",
                "open { - \"shop_menu\" }",
                "open { - \"player_settings\" }",
                "open { - \"{dynamic_menu_name}\" }",
                "open { - \"category_{selected_category}\" }",
                "open { - \"main_menu\" - \"shop_menu\" }",
                "open { - \"player_settings\" - \"inventory_menu\" - \"stats_menu\" }",
                "Legacy Format Examples:",
                "main_menu - Open main menu",
                "shop_menu - Open shop menu",
                "player_settings - Open player settings",
                "{dynamic_menu_name} - Dynamic menu name",
                "category_{selected_category} - Category-based menu",
                "[\"main_menu\", \"shop_menu\"] - Open main then shop menu",
                "[\"player_settings\", \"inventory_menu\", \"stats_menu\"] - Settings sequence",
                "[\"category_{type}\", \"item_list_{category}\"] - Dynamic category flow",
                "[\"welcome_menu\", \"tutorial_menu\", \"main_menu\"] - Onboarding sequence"
        };
    }
}

