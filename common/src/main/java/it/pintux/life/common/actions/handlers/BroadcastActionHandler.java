package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionContext;
import it.pintux.life.common.actions.ActionResult;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.pintux.life.common.platform.PlatformCommandExecutor;


public class BroadcastActionHandler extends BaseActionHandler {
    private final PlatformCommandExecutor commandExecutor;

    public BroadcastActionHandler(PlatformCommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public String getActionType() {
        return "broadcast";
    }

    @Override
    public ActionResult execute(FormPlayer player, String actionData, ActionContext context) {

        ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }

        try {

            if (isNewCurlyBraceFormat(actionData, "broadcast")) {
                return executeNewFormat(player, actionData, context);
            }


            List<String> broadcasts = parseActionData(actionData, context, player);

            if (broadcasts.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid broadcasts found");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }


            if (broadcasts.size() == 1) {
                return executeSingleBroadcast(player, broadcasts.get(0), null);
            }


            return executeMultipleBroadcastsFromList(broadcasts, player);

        } catch (Exception e) {
            logError("broadcast execution", actionData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing broadcast: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }


    private ActionResult executeNewFormat(FormPlayer player, String actionData, ActionContext context) {
        try {
            List<String> broadcasts = parseNewFormatValues(actionData);

            if (broadcasts.isEmpty()) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No broadcasts found in new format"), player);
            }


            List<String> processedBroadcasts = new ArrayList<>();
            for (String broadcast : broadcasts) {
                String processedBroadcast = processPlaceholders(broadcast, context, player);
                processedBroadcasts.add(processedBroadcast);
            }


            if (processedBroadcasts.size() == 1) {
                return executeSingleBroadcast(player, processedBroadcasts.get(0), null);
            }


            return executeMultipleBroadcastsFromList(processedBroadcasts, player);

        } catch (Exception e) {
            logger.error("Error executing new format broadcast action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error parsing new broadcast format: " + e.getMessage()), player);
        }
    }


    private ActionResult executeMultipleBroadcastsFromList(List<String> broadcasts, FormPlayer player) {
        int successCount = 0;
        int totalCount = broadcasts.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < broadcasts.size(); i++) {
            String broadcast = broadcasts.get(i);

            try {
                logger.info("Sending broadcast " + (i + 1) + "/" + totalCount + " from player " + player.getName() + ": " + broadcast);

                ActionResult result = executeSingleBroadcast(player, broadcast, null);

                if (result.isSuccess()) {
                    successCount++;
                    results.append("âś“ Broadcast ").append(i + 1).append(": Sent successfully");
                } else {
                    results.append("âś— Broadcast ").append(i + 1).append(": Failed to send");
                }

                if (i < broadcasts.size() - 1) {
                    results.append("\n");

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (Exception e) {
                results.append("âś— Broadcast ").append(i + 1).append(": Error - ").append(e.getMessage());
                logger.error("Error sending broadcast " + (i + 1) + " from player " + player.getName(), e);
                if (i < broadcasts.size() - 1) {
                    results.append("\n");
                }
            }
        }

        String finalMessage = String.format("Sent %d/%d broadcasts successfully:\n%s",
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


    private ActionResult executeSingleBroadcast(FormPlayer player, String broadcastData, ActionContext context) {

        String processedData = processPlaceholders(broadcastData.trim(), context, player);
        String[] parts = processedData.split(":", 3);

        String message;
        String broadcastCommand;

        if (parts.length == 1) {

            message = parts[0];
            broadcastCommand = "say " + message;
        } else if (parts.length == 3) {

            String type = parts[0];
            String target = parts[1];
            message = parts[2];

            switch (type.toLowerCase()) {
                case "permission":
                    broadcastCommand = "broadcast permission " + target + " " + message;
                    break;
                case "world":
                    broadcastCommand = "broadcast world " + target + " " + message;
                    break;
                case "radius":
                    broadcastCommand = "broadcast radius " + target + " " + player.getName() + " " + message;
                    break;
                default:
                    return createFailureResult("ACTION_INVALID_PARAMETERS", createReplacements("type", type), player);
            }
        } else {

            message = parts[0] + ":" + parts[1];
            broadcastCommand = "say " + message;
        }


        boolean success = executeWithErrorHandling(
                () -> {
                    commandExecutor.executeAsConsole(broadcastCommand);
                    return true;
                },
                "Broadcast command: " + broadcastCommand,
                player
        );

        if (success) {
            logSuccess("broadcast", message, player);
            return createSuccessResult("ACTION_BROADCAST_SUCCESS",
                    createReplacements("message", message), player);
        } else {
            logFailure("broadcast", message, player);
            return createFailureResult("ACTION_BROADCAST_FAILED",
                    createReplacements("message", message), player);
        }
    }


    private ActionResult executeMultipleBroadcasts(FormPlayer player, String multiValue, ActionContext context) {

        String listContent = multiValue.trim().substring(1, multiValue.trim().length() - 1);
        String[] broadcasts = listContent.split(",\\s*");

        for (String broadcast : broadcasts) {
            ActionResult result = executeSingleBroadcast(player, broadcast.trim(), context);
            if (result.isFailure()) {
                return result;
            }


            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logSuccess("broadcast", "Sent " + broadcasts.length + " broadcasts", player);
        return createSuccessResult("ACTION_BROADCAST_SUCCESS",
                createReplacements("message", "Sent " + broadcasts.length + " broadcasts"), player);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionValue.trim();

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {

            String listContent = trimmed.substring(1, trimmed.length() - 1);
            if (listContent.trim().isEmpty()) {
                return false;
            }
            String[] broadcasts = listContent.split(",\\s*");
            for (String broadcast : broadcasts) {
                if (!isValidSingleBroadcast(broadcast.trim())) {
                    return false;
                }
            }
            return true;
        } else {
            return isValidSingleBroadcast(trimmed);
        }
    }

    private boolean isValidSingleBroadcast(String broadcastData) {
        if (broadcastData.isEmpty()) return false;

        String[] parts = broadcastData.split(":");


        if (parts.length <= 2) return true;


        if (parts.length == 3) {
            String type = parts[0].toLowerCase();
            return type.equals("permission") || type.equals("world") || type.equals("radius");
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Broadcasts messages to all players or specific groups with support for multiple broadcasts";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "New Format Examples:",
                "broadcast { - \"Welcome to the server!\" }",
                "broadcast { - \"Server restart in 5 minutes\" - \"Please save your work\" }",
                "broadcast { - \"permission:vip.access:VIP only message!\" }",
                "broadcast { - \"world:world_nether:Nether announcement!\" }",
                "broadcast { - \"Welcome everyone!\" - \"permission:admin.access:Admin meeting in 10 minutes\" }"
        };
    }
}

