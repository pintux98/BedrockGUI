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


public class ServerActionHandler extends BaseActionHandler {
    private final PlatformCommandExecutor commandExecutor;

    public ServerActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public String getActionType() {
        return "server";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {

        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }

        try {

            if (isNewCurlyBraceFormat(actionData, "server")) {
                return executeNewFormat(player, actionData, context);
            }


            List<String> commands = parseActionData(actionData, context, player);

            if (commands.isEmpty()) {
                return emptyFailure(player);
            }


            if (commands.size() == 1) {
                return executeSingleServerCommand(commands.get(0), player);
            }


            return executeMultipleServerCommands(commands, player);

        } catch (Exception e) {
            logError("server command execution", actionData, player, e);
            return errorFailure(player, e);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {
            List<String> commands = parseNewFormatValues(actionData);

            if (commands.isEmpty()) {
                return emptyFailure(player);
            }


            List<String> processedCommands = new ArrayList<>();
            for (String command : commands) {
                String processedCommand = processPlaceholders(command, context, player);
                processedCommands.add(processedCommand);
            }


            if (processedCommands.size() == 1) {
                return executeSingleServerCommand(processedCommands.get(0), player);
            }


            return executeMultipleServerCommands(processedCommands, player);

        } catch (Exception e) {
            logger.error("Error executing new format server action for player " + player.getName() + ": " + e.getMessage());
            return errorFailure(player, e);
        }
    }

    private ActionSystem.ActionResult executeSingleServerCommand(String command, FormPlayer player) {
        try {
            logger.info("Executing server command: " + command + " for player " + player.getName());


            boolean success = executeWithErrorHandling(
                    () -> commandExecutor.executeAsConsole(command),
                    "Server command: " + command,
                    player
            );

            if (success) {
                logSuccess("server command", command, player);
                Map<String, Object> replacements = new HashMap<>();
                replacements.put("message", "Server command executed successfully: " + command);
                return createSuccessResult(MessageData.ACTION_SUCCESS, replacements, player);
            } else {
                return failure(player, "action.failed");
            }

        } catch (Exception e) {
            logError("server command execution", command, player, e);
            return errorFailure(player, e);
        }
    }

    private ActionSystem.ActionResult executeMultipleServerCommands(List<String> commands, FormPlayer player) {
        int successCount = 0;
        int totalCount = commands.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < commands.size(); i++) {
            String command = commands.get(i);

            try {
                logger.info("Executing server command " + (i + 1) + "/" + totalCount + ": " + command + " for player " + player.getName());

                boolean success = executeWithErrorHandling(
                        () -> commandExecutor.executeAsConsole(command),
                        "Server command: " + command,
                        player
                );

                if (success) {
                    successCount++;
                    results.append(" Command ").append(i + 1).append(": ").append(command).append(" - Success");
                    logSuccess("server command", command, player);
                } else {
                    results.append(" Command ").append(i + 1).append(": ").append(command).append(" - Failed");
                }

                if (i < commands.size() - 1) {
                    results.append("\n");

                }

            } catch (Exception e) {
                results.append(" Command ").append(i + 1).append(": ").append(command).append(" - Error: ").append(e.getMessage());
                logError("server command execution", command, player, e);
                if (i < commands.size() - 1) {
                    results.append("\n");
                }
            }
        }

        String finalMessage = String.format("Executed %d/%d server commands successfully:\n%s",
                successCount, totalCount, results.toString());

        Map<String, Object> replacements = new HashMap<>();
        replacements.put("message", finalMessage);
        replacements.put("success_count", successCount);
        replacements.put("total_count", totalCount);

        if (successCount == totalCount) {
            return createSuccessResult(MessageData.ACTION_SUCCESS, replacements, player);
        } else if (successCount > 0) {
            return createSuccessResult(MessageData.ACTION_SUCCESS, replacements, player);
        } else {
            return failure(player, "action.all_failed");
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }


        List<String> commands = parseActionDataForValidation(actionValue);

        for (String command : commands) {
            if (!ValidationUtils.isValidCommand(command)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        return "Executes commands as the server console with full administrative privileges. Can execute single or multiple commands with sequential processing and delay between commands.";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{

                "server:give {player} diamond 64 - Give 64 diamonds to player",
                "server:gamemode creative {player} - Set player to creative mode",
                "server:tp {player} spawn - Teleport player to spawn",
                "server:lp user {player} parent add vip - Add VIP rank to player",
                "server:weather clear - Set weather to clear",
                "server:time set day - Set time to day",


                "[\"server:give {player} diamond 64\", \"server:give {player} emerald 32\"] - Give diamonds and emeralds",
                "[\"server:gamemode creative {player}\", \"server:give {player} elytra 1\", \"server:give {player} firework_rocket 64\"] - Creative mode with flight items",
                "[\"server:weather clear\", \"server:time set day\", \"server:gamerule doDaylightCycle false\"] - Set perfect conditions",
                "[\"server:tp {player} spawn\", \"server:give {player} bread 16\", \"server:gamemode survival {player}\"] - Spawn setup sequence"
        };
    }
}

