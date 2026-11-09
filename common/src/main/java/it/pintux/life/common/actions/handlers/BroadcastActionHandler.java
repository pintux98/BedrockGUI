package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.pintux.life.common.platform.PlatformPlayerManager;


public class BroadcastActionHandler extends BaseActionHandler {
    private final PlatformPlayerManager playerManager;

    public BroadcastActionHandler(PlatformPlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public String getActionType() {
        return "broadcast";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {

        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }

        try {

            if (isNewCurlyBraceFormat(actionData, "broadcast")) {
                return executeNewFormat(player, actionData, context);
            }

            List<String> broadcasts = parseActionData(actionData, context, player);

            if (broadcasts.isEmpty()) {
                return emptyFailure(player);
            }


            if (broadcasts.size() == 1) {
                return executeSingleBroadcast(player, broadcasts.get(0), null);
            }

            return executeMultipleBroadcastsFromList(broadcasts, player);

        } catch (Exception e) {
            logError("broadcast execution", actionData, player, e);
            return errorFailure(player, e);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {
            List<String> broadcasts = parseNewFormatValues(actionData);

            if (broadcasts.isEmpty()) {
                return emptyFailure(player);
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
            return errorFailure(player, e);
        }
    }


    private ActionSystem.ActionResult executeMultipleBroadcastsFromList(List<String> broadcasts, FormPlayer player) {
        int successCount = 0;
        int totalCount = broadcasts.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < broadcasts.size(); i++) {
            String broadcast = broadcasts.get(i);

            try {
                //logger.info("Sending broadcast " + (i + 1) + "/" + totalCount + " from player " + player.getName() + ": " + broadcast);

                ActionSystem.ActionResult result = executeSingleBroadcast(player, broadcast, null);

                if (result.isSuccess()) {
                    successCount++;
                    results.append(" Broadcast ").append(i + 1).append(": Sent successfully");
                } else {
                    results.append(" Broadcast ").append(i + 1).append(": Failed to send");
                }

                if (i < broadcasts.size() - 1) {
                    results.append("\n");

                }

            } catch (Exception e) {
                results.append(" Broadcast ").append(i + 1).append(": Error - ").append(e.getMessage());
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
            return createSuccessResult(MessageData.ACTION_SUCCESS, replacements, player);
        } else if (successCount > 0) {
            return createSuccessResult(MessageData.ACTION_SUCCESS, replacements, player);
        } else {
            return failure(player, "action.all_failed");
        }
    }


    private ActionSystem.ActionResult executeSingleBroadcast(FormPlayer player, String broadcastData, ActionSystem.ActionContext context) {

        String processedData = processPlaceholders(broadcastData.trim(), context, player);
        String[] parts = processedData.split(":", 3);

        String audience = "all";
        String target = "";
        String message = processedData;
        if (parts.length == 3 && isBroadcastType(parts[0])) {
            audience = parts[0].toLowerCase();
            target = parts[1];
            message = parts[2];
        }

        String finalAudience = audience;
        String finalTarget = target;
        String finalMessage = message;
        boolean success = executeWithErrorHandling(
                () -> playerManager.broadcast(player, finalAudience, finalTarget, finalMessage),
                "Broadcast to " + audience + " " + target,
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
        return !broadcastData.isEmpty();
    }

    private static boolean isBroadcastType(String type) {
        String lower = type.toLowerCase();
        return lower.equals("permission") || lower.equals("world") || lower.equals("radius");
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


