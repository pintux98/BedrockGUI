package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ActionBarActionHandler extends BaseActionHandler {
    private final PlatformTitleManager titleManager;

    public ActionBarActionHandler(PlatformTitleManager titleManager) {
        this.titleManager = titleManager;
    }

    @Override
    public String getActionType() {
        return "actionbar";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {

        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }

        try {

            if (isNewCurlyBraceFormat(actionData, "actionbar")) {
                return executeNewFormat(player, actionData, context);
            }


            List<String> messages = parseActionData(actionData, context, player);

            if (messages.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid action bar messages found");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }


            if (messages.size() == 1) {
                return executeSingleActionBar(player, messages.get(0), null);
            }


            return executeMultipleActionBarsFromList(messages, player);

        } catch (Exception e) {
            logError("action bar execution", actionData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing action bar: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {
            List<String> messages = parseNewFormatValues(actionData);

            if (messages.isEmpty()) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No action bar messages found in new format"), player);
            }


            List<String> processedMessages = new ArrayList<>();
            for (String message : messages) {
                String processedMessage = processPlaceholders(message, context, player);
                processedMessages.add(processedMessage);
            }


            if (processedMessages.size() == 1) {
                return executeSingleActionBar(player, processedMessages.get(0), null);
            }


            return executeMultipleActionBarsFromList(processedMessages, player);

        } catch (Exception e) {
            logger.error("Error executing new format action bar action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error parsing new action bar format: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeMultipleActionBarsFromList(List<String> messages, FormPlayer player) {
        int successCount = 0;
        int totalCount = messages.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);

            try {
                logger.info("Sending action bar " + (i + 1) + "/" + totalCount + " to player " + player.getName() + ": " + message);

                ActionSystem.ActionResult result = executeSingleActionBar(player, message, null);

                if (result.isSuccess()) {
                    successCount++;
                    results.append("âś“ Action Bar ").append(i + 1).append(": Sent successfully");
                } else {
                    results.append("âś— Action Bar ").append(i + 1).append(": Failed to send");
                }

                if (i < messages.size() - 1) {
                    results.append("\n");

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (Exception e) {
                results.append("âś— Action Bar ").append(i + 1).append(": Error - ").append(e.getMessage());
                logger.error("Error sending action bar " + (i + 1) + " to player " + player.getName(), e);
                if (i < messages.size() - 1) {
                    results.append("\n");
                }
            }
        }

        String finalMessage = String.format("Sent %d/%d action bars successfully:\n%s",
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


    private ActionSystem.ActionResult executeSingleActionBar(FormPlayer player, String message, ActionSystem.ActionContext context) {
        String processedMessage = processPlaceholders(message.trim(), context, player);

        boolean success = titleManager.sendActionBar(player, processedMessage);

        if (success) {
            logSuccess("actionbar", processedMessage, player);
            return createSuccessResult("ACTION_ACTIONBAR_SUCCESS",
                    createReplacements("message", processedMessage), player);
        } else {
            logFailure("actionbar", processedMessage, player);
            return createFailureResult("ACTION_ACTIONBAR_FAILED",
                    createReplacements("message", processedMessage), player);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionValue.trim();

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {

            String listContent = trimmed.substring(1, trimmed.length() - 1);
            String[] messages = listContent.split(",\\s*");
            for (String message : messages) {
                if (message.trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        } else {
            return !trimmed.isEmpty();
        }
    }

    @Override
    public String getDescription() {
        return "Sends action bar messages to players with support for multiple sequential messages";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "New Format Examples:",
                "actionbar { - \"Processing your request...\" }",
                "actionbar { - \"Loading...\" - \"Almost done...\" - \"Complete!\" }",
                "actionbar { - \"Â§eWelcome {player}!\" - \"Â§aEnjoy your stay!\" }"
        };
    }
}

