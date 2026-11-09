package it.pintux.life.common.actions.handlers;

import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.platform.PlatformTitleManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;

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
                return emptyFailure(player);
            }


            if (titles.size() == 1) {
                return executeSingleTitle(player, titles.get(0), null);
            }


            return executeMultipleTitlesFromList(titles, player, null);

        } catch (Exception e) {
            logError("title execution", actionData, player, e);
            return errorFailure(player, e);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {
            List<String> titles = parseNewFormatValues(actionData);

            if (titles.isEmpty()) {
                return emptyFailure(player);
            }


            if (titles.size() == 1) {
                return executeSingleTitle(player, titles.get(0), context);
            }


            return executeMultipleTitlesFromList(titles, player, context);

        } catch (Exception e) {
            logger.error("Error executing new format title action for player " + player.getName() + ": " + e.getMessage());
            return errorFailure(player, e);
        }
    }


    private ActionSystem.ActionResult executeMultipleTitlesFromList(List<String> titles, FormPlayer player, ActionSystem.ActionContext context) {
        int successCount = 0;
        int totalCount = titles.size();
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < titles.size(); i++) {
            String title = titles.get(i);

            try {
                logger.info("Showing title " + (i + 1) + "/" + totalCount + " to player " + player.getName() + ": " + title);

                ActionSystem.ActionResult result = executeSingleTitle(player, title, context);

                if (result.isSuccess()) {
                    successCount++;
                    results.append(" Title ").append(i + 1).append(": ").append(title).append(" - Success");
                } else {
                    results.append(" Title ").append(i + 1).append(": ").append(title).append(" - Failed");
                }

                if (i < titles.size() - 1) {
                    results.append("\n");

                }

            } catch (Exception e) {
                results.append(" Title ").append(i + 1).append(": ").append(title).append(" - Error: ").append(e.getMessage());
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
            return createSuccessResult(MessageData.ACTION_SUCCESS, replacements, player);
        } else if (successCount > 0) {
            return createSuccessResult("ACTION_PARTIAL_SUCCESS", replacements, player);
        } else {
            return failure(player, "action.all_failed");
        }
    }


    private ActionSystem.ActionResult executeSingleTitle(FormPlayer player, String titleData, ActionSystem.ActionContext context) {
        TitleParts parsed = TitleParts.parse(titleData.trim());
        String title = processPlaceholders(parsed.title(), context, player);
        String subtitle = processPlaceholders(parsed.subtitle(), context, player);
        int fadeIn = parsed.fadeIn();
        int stay = parsed.stay();
        int fadeOut = parsed.fadeOut();

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


    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionValue.trim();

        // Support new unified format "title { ... }"
        if (isNewCurlyBraceFormat(trimmed, "title")) {
            try {
                java.util.List<String> vals = parseNewFormatValues(trimmed);
                for (String v : vals) {
                    if (!isValidSingleTitle(v.trim())) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
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
        return !titleData.isEmpty();
    }

    record TitleParts(String title, String subtitle, int fadeIn, int stay, int fadeOut) {

        static TitleParts parse(String data) {
            List<String> parts = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inPlaceholder = false;
            for (int i = 0; i < data.length(); i++) {
                char c = data.charAt(i);
                if (c == '%') {
                    inPlaceholder = !inPlaceholder && it.pintux.life.common.utils.ConditionEvaluator.opensPlaceholder(data, i);
                    current.append(c);
                } else if (c == '\\' && i + 1 < data.length() && data.charAt(i + 1) == ':') {
                    current.append(':');
                    i++;
                } else if (c == ':' && !inPlaceholder) {
                    parts.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            parts.add(current.toString());

            int fadeIn = 10;
            int stay = 60;
            int fadeOut = 10;
            int size = parts.size();
            if (size >= 5 && isTiming(parts.get(size - 3)) && isTiming(parts.get(size - 2)) && isTiming(parts.get(size - 1))) {
                fadeIn = Integer.parseInt(parts.get(size - 3).trim());
                stay = Integer.parseInt(parts.get(size - 2).trim());
                fadeOut = Integer.parseInt(parts.get(size - 1).trim());
                parts = parts.subList(0, size - 3);
            }
            String subtitle = parts.size() > 1 ? String.join(":", parts.subList(1, parts.size())) : "";
            return new TitleParts(parts.get(0), subtitle, fadeIn, stay, fadeOut);
        }

        private static boolean isTiming(String value) {
            return value.trim().matches("\\d{1,6}");
        }
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

}


