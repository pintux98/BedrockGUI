package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.platform.PlatformCommandExecutor;



import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CommandActionHandler extends BaseActionHandler {
    private final PlatformCommandExecutor commandExecutor;

    public CommandActionHandler() {
        this.commandExecutor = null;
    }

    public CommandActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public String getActionType() {
        return "command";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {

        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }

        try {

            if (isNewCurlyBraceFormat(actionData, "command")) {
                return executeNewFormat(player, actionData, context);
            }


            List<String> commands = parseActionData(actionData, context, player);

            if (commands.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid commands found");
                return createFailureResult("execution_error", errorReplacements, player);
            }


            if (commands.size() == 1) {
                return executeSingleCommand(commands.get(0), player);
            }


            return executeMultipleCommands(commands, player);

        } catch (Exception e) {
            logError("command execution", actionData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing command: " + e.getMessage());
            return createFailureResult("execution_error", errorReplacements, player, e);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {
            List<String> commands = parseNewFormatValues(actionData);

            if (commands.isEmpty()) {
                return createFailureResult("execution_error", createReplacements("error", "No commands found in new format"), player);
            }


            List<String> processedCommands = new ArrayList<>();
            for (String command : commands) {
                String processedCommand = processPlaceholders(command, context, player);
                processedCommands.add(processedCommand);
            }


            if (processedCommands.size() == 1) {
                return executeSingleCommand(processedCommands.get(0), player);
            }


            return executeMultipleCommands(processedCommands, player);

        } catch (Exception e) {
            logger.error("Error executing new format command action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("execution_error", createReplacements("error", "Error parsing new command format: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeSingleCommand(String command, FormPlayer player) {
        String normalizedCommand = normalizeCommand(command);

        if (normalizedCommand.isEmpty()) {
            return createFailureResult("execution_error",
                    createReplacements("error", "Empty command after processing"), player);
        }

        boolean success;
        if (commandExecutor != null) {
            success = executeWithErrorHandling(
                    () -> commandExecutor.executeAsPlayer(player.getName(), normalizedCommand),
                    "Player command: " + normalizedCommand,
                    player
            );
        } else {
            success = executeWithErrorHandling(
                    () -> player.executeAction("/" + normalizedCommand),
                    "Player command: " + normalizedCommand,
                    player
            );
        }

        if (success) {
            logSuccess("command", normalizedCommand, player);
            return createSuccessResult(MessageData.ACTION_COMMAND_SUCCESS,
                    createReplacements("command", normalizedCommand), player);
        } else {
            logFailure("command", normalizedCommand, player);
            return createFailureResult(MessageData.ACTION_COMMAND_FAILED,
                    createReplacements("command", normalizedCommand), player);
        }
    }


    private ActionSystem.ActionResult executeMultipleCommands(List<String> commands, FormPlayer player) {
        int successCount = 0;
        int totalCount = commands.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < commands.size(); i++) {
            String command = commands.get(i);

            try {
                logger.info("Executing command " + (i + 1) + "/" + totalCount + " for player " + player.getName() + ": " + command);

                ActionSystem.ActionResult result = executeSingleCommand(command, player);

                if (result.isSuccess()) {
                    successCount++;
                    results.append("âś“ Command ").append(i + 1).append(": ").append(command).append(" - Success");
                } else {
                    results.append("âś— Command ").append(i + 1).append(": ").append(command).append(" - Failed");
                }

                if (i < commands.size() - 1) {
                    results.append("\n");

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (Exception e) {
                results.append("âś— Command ").append(i + 1).append(": ").append(command).append(" - Error: ").append(e.getMessage());
                logger.error("Error executing command " + (i + 1) + " for player " + player.getName(), e);
                if (i < commands.size() - 1) {
                    results.append("\n");
                }
            }
        }

        String finalMessage = String.format("Executed %d/%d commands successfully:\n%s",
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
            return createFailureResult("execution_error", replacements, player);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        return !ValidationUtils.isNullOrEmpty(actionValue);
    }

    @Override
    public String getDescription() {
        return "Executes commands as the player with support for placeholders";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "New Format Examples:",
                "command { - \"gamemode creative\" }",
                "command { - \"gamemode creative\" - \"give diamond 64\" - \"tp spawn\" }",
                "command { - \"eco give {player} 1000\" - \"title {player} title Welcome!\" }",
                "",
                "Legacy Format Examples (deprecated):",
                "command:gamemode creative",
                "command:give {player} diamond 64",
                "command:tp {player} spawn"
        };
    }


    protected String normalizeCommand(String command) {
        if (command == null) return "";
        String trimmed = command.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }


    @Deprecated
    protected String processPlaceholders(String command, ActionSystem.ActionContext context, FormPlayer player) {
        return super.processPlaceholders(command, context, player);
    }
}


