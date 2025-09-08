package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.ValidationUtils;
import it.pintux.life.common.utils.MessageData;

/**
 * Handles command execution actions
 */
public class CommandActionHandler extends BaseActionHandler {

    @Override
    public String getActionType() {
        return "command";
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        // Validate basic parameters using base class method
        ActionResult validationResult = validateBasicParameters(player, actionValue);
        if (validationResult != null) {
            return validationResult;
        }

        try {
            String processedCommand = processPlaceholders(actionValue, context, player);

            if (processedCommand.startsWith("/")) {
                processedCommand = processedCommand.substring(1);
            }

            if (ValidationUtils.isNullOrEmpty(processedCommand.trim())) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", MessageData.ACTION_INVALID_PARAMETERS), player);
            }

            boolean success = player.executeAction("/" + processedCommand);

            if (success) {
                logSuccess("command", processedCommand, player);
                return createSuccessResult(MessageData.ACTION_COMMAND_SUCCESS, createReplacements("command", processedCommand), player);
            } else {
                logFailure("command", processedCommand, player);
                return createFailureResult(MessageData.ACTION_COMMAND_FAILED, createReplacements("command", processedCommand), player);
            }

        } catch (Exception e) {
            logError("command", actionValue, player, e);
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", e.getMessage()), player, e);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return false;
        }

        String trimmed = actionValue.trim();

        if (trimmed.isEmpty() || trimmed.equals("/")) {
            return false;
        }

        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        return !trimmed.contains("\n") && !trimmed.contains("\r");
    }

    @Override
    public String getDescription() {
        return "Executes a server command. Supports placeholders for dynamic values.";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "give $player diamond 1",
                "/tp $player 0 100 0",
                "say Hello $player!",
                "gamemode creative $player"
        };
    }

    /**
     * Processes placeholders in the command string
     *
     * @param command the command with placeholders
     * @param context the action context containing placeholder values
     * @return the processed command
     */

}
