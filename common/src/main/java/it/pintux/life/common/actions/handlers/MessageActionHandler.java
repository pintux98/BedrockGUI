package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.platform.PlatformPlayerManager;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ValidationUtils;
import it.pintux.life.common.utils.PlaceholderUtil;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.HashMap;
import java.util.List;


public class MessageActionHandler extends BaseActionHandler {

    private final PlatformPlayerManager playerManager;

    public MessageActionHandler(PlatformPlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    @Override
    public String getActionType() {
        return "message";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionValue, ActionSystem.ActionContext context) {

        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionValue);
        if (validationResult != null) {
            return validationResult;
        }

        try {

            if (isNewCurlyBraceFormat(actionValue, "message")) {
                return executeNewFormat(player, actionValue, context);
            } else {
                return executeSingleMessage(player, actionValue, context);
            }
        } catch (Exception e) {
            logger.error("Error executing message action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error executing message action: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {
            List<String> messages = parseNewFormatValues(actionData);

            if (messages.isEmpty()) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No messages found in new format"), player);
            }


            if (messages.size() == 1) {
                return executeSingleMessage(player, messages.get(0), context);
            }


            return executeMultipleMessagesFromList(player, messages, context);

        } catch (Exception e) {
            logger.error("Error executing new format message action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error parsing new message format: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeMultipleMessagesFromList(FormPlayer player, List<String> messages, ActionSystem.ActionContext context) {
        int successCount = 0;
        int totalCount = messages.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);

            try {
                logger.info("Sending message " + (i + 1) + "/" + totalCount + " to player " + player.getName() + ": " + message);

                ActionSystem.ActionResult result = executeSingleMessage(player, message, context);

                if (result.isSuccess()) {
                    successCount++;
                    results.append("âś“ Message ").append(i + 1).append(": Sent successfully");
                } else {
                    results.append("âś— Message ").append(i + 1).append(": Failed to send");
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
                results.append("âś— Message ").append(i + 1).append(": Error - ").append(e.getMessage());
                logger.error("Error sending message " + (i + 1) + " to player " + player.getName(), e);
                if (i < messages.size() - 1) {
                    results.append("\n");
                }
            }
        }

        String finalMessage = String.format("Sent %d/%d messages successfully:\n%s",
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


    private ActionSystem.ActionResult executeSingleMessage(FormPlayer player, String message, ActionSystem.ActionContext context) {
        String processedMessage = processPlaceholders(message, context, player);

        if (ValidationUtils.isNullOrEmpty(processedMessage.trim())) {
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", MessageData.ACTION_INVALID_PARAMETERS), player);
        }


        String coloredMessage = processColorCodes(processedMessage);
        playerManager.sendMessage(player, coloredMessage);

        logSuccess("message", processedMessage, player);
        return createSuccessResult("ACTION_MESSAGE_SUCCESS", createReplacements("message", processedMessage), player);
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (ValidationUtils.isNullOrEmpty(actionValue)) {
            return false;
        }

        String trimmed = actionValue.trim();
        return trimmed.length() > 0 && trimmed.length() <= 1000;
    }

    @Override
    public String getDescription() {
        return "Sends messages to players with support for color codes and placeholders";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
        };
    }


    private String processColorCodes(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String result = message;


        result = processHexColorCodes(result);


        result = processClassicColorCodes(result);


        result = processMiniMessageFormat(result);

        return result;
    }


    private String processHexColorCodes(String message) {
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            String replacement = "\u00A7x\u00A7" + hexColor.charAt(0) + "\u00A7" + hexColor.charAt(1) +
                                 "\u00A7" + hexColor.charAt(2) + "\u00A7" + hexColor.charAt(3) +
                                 "\u00A7" + hexColor.charAt(4) + "\u00A7" + hexColor.charAt(5);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }


    private String processClassicColorCodes(String message) {
        return message.replace('&', '\u00A7');
    }


    private String processMiniMessageFormat(String message) {
        String result = message;


        Pattern miniHexPattern = Pattern.compile("<color:#([A-Fa-f0-9]{6})>");
        Matcher hexMatcher = miniHexPattern.matcher(result);
        StringBuffer hexResult = new StringBuffer();
        while (hexMatcher.find()) {
            String hexColor = hexMatcher.group(1);
            String replacement = "\u00A7x\u00A7" + hexColor.charAt(0) + "\u00A7" + hexColor.charAt(1) +
                                 "\u00A7" + hexColor.charAt(2) + "\u00A7" + hexColor.charAt(3) +
                                 "\u00A7" + hexColor.charAt(4) + "\u00A7" + hexColor.charAt(5);
            hexMatcher.appendReplacement(hexResult, replacement);
        }
        hexMatcher.appendTail(hexResult);
        result = hexResult.toString();


        Map<String, String> miniMessageMap = new HashMap<>();

        miniMessageMap.put("<black>", "\u00A70");
        miniMessageMap.put("<dark_blue>", "\u00A71");
        miniMessageMap.put("<dark_green>", "\u00A72");
        miniMessageMap.put("<dark_aqua>", "\u00A73");
        miniMessageMap.put("<dark_red>", "\u00A74");
        miniMessageMap.put("<dark_purple>", "\u00A75");
        miniMessageMap.put("<gold>", "\u00A76");
        miniMessageMap.put("<gray>", "\u00A77");
        miniMessageMap.put("<dark_gray>", "\u00A78");
        miniMessageMap.put("<blue>", "\u00A79");
        miniMessageMap.put("<green>", "\u00A7a");
        miniMessageMap.put("<aqua>", "\u00A7b");
        miniMessageMap.put("<red>", "\u00A7c");
        miniMessageMap.put("<light_purple>", "\u00A7d");
        miniMessageMap.put("<yellow>", "\u00A7e");
        miniMessageMap.put("<white>", "\u00A7f");


        miniMessageMap.put("<bold>", "\u00A7l");
        miniMessageMap.put("<italic>", "\u00A7o");
        miniMessageMap.put("<underlined>", "\u00A7n");
        miniMessageMap.put("<strikethrough>", "\u00A7m");
        miniMessageMap.put("<obfuscated>", "\u00A7k");
        miniMessageMap.put("<reset>", "\u00A7r");


        miniMessageMap.put("</bold>", "\u00A7r");
        miniMessageMap.put("</italic>", "\u00A7r");
        miniMessageMap.put("</underlined>", "\u00A7r");
        miniMessageMap.put("</strikethrough>", "\u00A7r");
        miniMessageMap.put("</obfuscated>", "\u00A7r");
        miniMessageMap.put("</color>", "\u00A7r");


        for (Map.Entry<String, String> entry : miniMessageMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }
}


