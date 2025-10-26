package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TitleActionHandler extends BaseActionHandler {
    private final PlatformTitleManager titleManager;

    public TitleActionHandler(PlatformTitleManager titleManager) {
        this.titleManager = titleManager;
    }

    @Override
    public String getActionType() {
        return "title";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {

        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null) {
            return validationResult;
        }

        try {

            if (isNewCurlyBraceFormat(actionData, "title")) {
                return executeNewFormat(player, actionData, context);
            }


            List<String> titles = parseActionData(actionData, context, player);

            if (titles.isEmpty()) {
                Map<String, Object> errorReplacements = createReplacements("error", "No valid titles found");
                return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player);
            }


            if (titles.size() == 1) {
                return executeSingleTitle(player, titles.get(0), null);
            }


            return executeMultipleTitlesFromList(titles, player);

        } catch (Exception e) {
            logError("title execution", actionData, player, e);
            Map<String, Object> errorReplacements = createReplacements("error", "Error executing title: " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", errorReplacements, player, e);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {
            List<String> titles = parseNewFormatValues(actionData);

            if (titles.isEmpty()) {
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "No titles found in new format"), player);
            }


            List<String> processedTitles = new ArrayList<>();
            for (String title : titles) {
                String processedTitle = processPlaceholders(title, context, player);
                processedTitles.add(processedTitle);
            }


            if (processedTitles.size() == 1) {
                return executeSingleTitle(player, processedTitles.get(0), null);
            }


            return executeMultipleTitlesFromList(processedTitles, player);

        } catch (Exception e) {
            logger.error("Error executing new format title action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Error parsing new title format: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeMultipleTitlesFromList(List<String> titles, FormPlayer player) {
        int successCount = 0;
        int totalCount = titles.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i);

            try {
                logger.info("Showing title " + (i + 1) + "/" + totalCount + " to player " + player.getName() + ": " + title);

                ActionSystem.ActionResult result = executeSingleTitle(player, title, null);

                if (result.isSuccess()) {
                    successCount++;
                    results.append("âś“ Title ").append(i + 1).append(": ").append(title).append(" - Success");
                } else {
                    results.append("âś— Title ").append(i + 1).append(": ").append(title).append(" - Failed");
                }

                if (i < titles.size() - 1) {
                    results.append("\n");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (Exception e) {
                results.append("âś— Title ").append(i + 1).append(": ").append(title).append(" - Error: ").append(e.getMessage());
                logger.error("Error showing title " + (i + 1) + " to player " + player.getName(), e);
                if (i < titles.size() - 1) {
                    results.append("\n");
                }
            }
        }

        String finalMessage = String.format("Showed %d/%d titles successfully:\n%s",
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


    private ActionSystem.ActionResult executeSingleTitle(FormPlayer player, String titleData, ActionSystem.ActionContext context) {
        String processedData = processPlaceholders(titleData.trim(), context, player);
        String[] parts = processedData.split(":");

        String title = parts.length > 0 ? parts[0] : "";
        String subtitle = parts.length > 1 ? parts[1] : "";
        int fadeIn = parts.length > 2 ? parseIntSafe(parts[2], 10) : 10;
        int stay = parts.length > 3 ? parseIntSafe(parts[3], 60) : 60;
        int fadeOut = parts.length > 4 ? parseIntSafe(parts[4], 10) : 10;

        boolean success = titleManager.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);

        if (success) {
            logSuccess("title", title + (subtitle.isEmpty() ? "" : " / " + subtitle), player);
            Map<String, Object> replacements = createReplacements("title", title);
            replacements.put("subtitle", subtitle);
            return createSuccessResult("ACTION_TITLE_SUCCESS", replacements, player);
        } else {
            logFailure("title", title, player);
            return createFailureResult("ACTION_TITLE_FAILED",
                    createReplacements("title", title), player);
        }
    }


    private boolean validateParameters(FormPlayer player, String actionValue) {
        return player != null && actionValue != null && !actionValue.trim().isEmpty();
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionValue.trim();

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {

            String listContent = trimmed.substring(1, trimmed.length() - 1);
            String[] titles = listContent.split(",\\s*");
            for (String title : titles) {
                if (!isValidSingleTitle(title.trim())) {
                    return false;
                }
            }
            return true;
        } else {
            return isValidSingleTitle(trimmed);
        }
    }

    private boolean isValidSingleTitle(String titleData) {
        if (titleData.isEmpty()) return false;

        String[] parts = titleData.split(":");


        if (parts.length > 2) {
            for (int i = 2; i < parts.length && i < 5; i++) {
                try {
                    int value = Integer.parseInt(parts[i]);
                    if (value < 0) return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        return "Sends titles and subtitles to players with customizable timing and support for multiple titles";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "New Format Examples:",
                "title { - \"Welcome!\" }",
                "title { - \"Welcome!:Enjoy your stay\" }",
                "title { - \"Welcome!:Enjoy your stay:10:60:10\" }",
                "title { - \"Loading...\" - \"Ready!:Let's go!:5:20:5\" - \"Have fun!\" }"
        };
    }

    private int parseIntSafe(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}


