package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionHandler;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.Logger;
import it.pintux.life.common.utils.PlaceholderUtil;
import it.pintux.life.common.utils.ValidationUtils;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.api.BedrockGUIApi;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Handles command execution actions
 */
public class CommandActionHandler implements ActionHandler {

    private static final Logger logger = Logger.getLogger(CommandActionHandler.class);

    @Override
    public String getActionType() {
        return "command";
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionValue, ActionContext context) {
        if (player == null) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }

        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
        }

        try {
            String processedCommand = processPlaceholders(actionValue, context, player);

            if (processedCommand.startsWith("/")) {
                processedCommand = processedCommand.substring(1);
            }

            if (ValidationUtils.isNullOrEmpty(processedCommand.trim())) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_INVALID_PARAMETERS, null, player));
            }

            boolean success = player.executeAction("/" + processedCommand);

            if (success) {
                logger.debug("Successfully executed command '" + processedCommand + "' for player " + player.getName());
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("command", processedCommand);
                return ActionResult.success(messageData.getValueNoPrefix(MessageData.ACTION_COMMAND_SUCCESS, replacements, player));
            } else {
                logger.warn("Failed to execute command '" + processedCommand + "' for player " + player.getName());
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("command", processedCommand);
                return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_COMMAND_FAILED, replacements, player));
            }

        } catch (Exception e) {
            logger.error("Error executing command '" + actionValue + "' for player " + player.getName(), e);
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            Map<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return ActionResult.failure(messageData.getValueNoPrefix(MessageData.ACTION_EXECUTION_ERROR, replacements, player), e);
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
    private String processPlaceholders(String command, ActionContext context, FormPlayer player) {
        if (context == null) {
            return command;
        }

        String result = PlaceholderUtil.processDynamicPlaceholders(command, context.getPlaceholders());
        result = PlaceholderUtil.processFormResults(result, context.getFormResults());
        
        // Process PlaceholderAPI placeholders if MessageData is available
        if (context.getMetadata() != null && context.getMetadata().containsKey("messageData")) {
            Object messageData = context.getMetadata().get("messageData");
            result = PlaceholderUtil.processPlaceholders(result, player, messageData);
        }

        return result;
    }
}