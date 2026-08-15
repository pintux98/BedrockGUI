package it.pintux.life.common.actions.handlers;
import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.actions.ActionSystem;



import it.pintux.life.common.actions.ActionExecutor;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;

import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RandomActionHandler extends BaseActionHandler {

    // "<action>@<weight>", e.g. "economy:add:100@2.5". A colon cannot carry the weight:
    // "sound:ui.button.click:1.0:1.2" already ends in a pitch, and "delay:1000" in a duration.
    // The '@' must follow a non-space character, so message text ending in "@1" is left alone.
    private static final java.util.regex.Pattern WEIGHT_SUFFIX =
            java.util.regex.Pattern.compile("^(.*\\S)@(\\d+(?:\\.\\d+)?)$");

    private final ActionExecutor actionExecutor;
    private final Random random;
    private static final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "RandomActionHandler-" + System.currentTimeMillis());
        thread.setDaemon(true);
        return thread;
    });

    public RandomActionHandler(ActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
        this.random = new Random();
    }

    private List<WeightedAction> parseNewFormatRandomActions(String actionData, ActionSystem.ActionContext context, FormPlayer player) {
        List<WeightedAction> weightedActions = new ArrayList<>();
        
        try {
            List<String> actions = parseNewFormatValues(actionData);
            
            for (String action : actions) {
                // Process placeholders for each action
                String processedAction = processPlaceholders(action, context, player);
                WeightedAction weightedAction = parseWeightedAction(processedAction);
                if (weightedAction != null) {
                    weightedActions.add(weightedAction);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing new format random actions: " + e.getMessage(), e);
        }
        
        return weightedActions;
    }
    
    private List<WeightedAction> parseLegacyFormatRandomActions(String actionData, ActionSystem.ActionContext context, FormPlayer player) {
        List<WeightedAction> weightedActions = new ArrayList<>();
        
        try {
            String processedData = processPlaceholders(actionData.trim(), context, player);
            String[] actionOptions = processedData.split("\\|");
            
            for (String action : actionOptions) {
                String trimmedAction = action.trim();
                if (!trimmedAction.isEmpty()) {
                    WeightedAction weightedAction = parseWeightedAction(trimmedAction);
                    if (weightedAction != null) {
                        weightedActions.add(weightedAction);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing legacy format random actions: " + e.getMessage(), e);
        }
        
        return weightedActions;
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class WeightedAction {
        private final String action;
        private final double weight;

        public WeightedAction(String action, double weight) {
            this.action = action;
            this.weight = weight;
        }

        public String getAction() {
            return action;
        }

        public double getWeight() {
            return weight;
        }
    }

    @Override
    public String getActionType() {
        return "random";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        ActionSystem.ActionResult validationResult = validateBasicParameters(player, actionData);
        if (validationResult != null && !validationResult.isSuccess()) {
            return validationResult;
        }

        try {
            List<WeightedAction> weightedActions;
            
            // Check if it's the new YAML format with curly braces
            if (isNewCurlyBraceFormat(actionData, "random")) {
                weightedActions = parseNewFormatRandomActions(actionData, context, player);
            } else {
                // Legacy format support - pipe-separated
                weightedActions = parseLegacyFormatRandomActions(actionData, context, player);
            }

            if (weightedActions.isEmpty()) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", messageData.getValue(MessageData.ACTION_INVALID_FORMAT, null, player)), player);
            }

            // Filter out recursive random entries
            java.util.List<WeightedAction> filtered = new java.util.ArrayList<>();
            for (WeightedAction wa : weightedActions) {
                String a = wa.getAction();
                if (a != null && !a.trim().matches("^random\\s*\\{[\\s\\S]*\\}$")) {
                    filtered.add(wa);
                }
            }
            if (filtered.isEmpty()) {
                return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "No valid random actions"), player);
            }
            String selectedAction = selectWeightedRandom(filtered);

            logger.debug("Selected random action for player " + player.getName() + ": " + selectedAction + " (1 of " + filtered.size() + ")");

            // Run on the calling thread. Handing this to a worker thread and then blocking on the
            // result gained nothing and put command dispatch off the main thread, which Paper's
            // AsyncCatcher rejects.
            ActionSystem.Action parsedAction = actionExecutor.parseAction(selectedAction);
            if (parsedAction == null) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                HashMap<String, Object> replacements = new HashMap<>();
                replacements.put("action", selectedAction);
                return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", messageData.getValue(MessageData.ACTION_INVALID_FORMAT, replacements, player)), player);
            }

            ActionSystem.ActionResult result = actionExecutor.executeAction(player, parsedAction.getActionDefinition(), context);
            if (result.isSuccess()) {
                return createSuccessResult(MessageData.ACTION_SUCCESS, createReplacements("message", "Executed random action: " + selectedAction), player);
            } else {
                return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "Random action failed: " + result.message()), player);
            }

        } catch (Exception e) {
            logger.error("Error executing random action for player " + player.getName() + ": " + e.getMessage());
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            HashMap<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "Failed to execute random action: " + e.getMessage()), player);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }

        String trimmed = actionValue.trim();
        
        // Support new YAML format
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                List<String> actions = parseNewFormatValues(trimmed);
                for (String action : actions) {
                    if (!isValidRandomAction(action)) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        // Legacy format support - pipe-separated
        String[] actionOptions = trimmed.split("\\|");

        if (actionOptions.length == 0) {
            return false;
        }

        // Check if at least one action is valid
        for (String action : actionOptions) {
            String trimmedAction = action.trim();
            if (!trimmedAction.isEmpty() && trimmedAction.contains(":")) {
                return true;
            }
        }

        return false;
    }
    
    private boolean isValidRandomAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            return false;
        }
        String trimmed = stripWeightSuffix(action);
        return !trimmed.isEmpty() && trimmed.contains(":");
    }

    @Override
    public String getDescription() {
        return "Randomly selects and executes one action from a list, optionally weighted with @<weight>";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "New Format Examples:",
                "random { - \"command:give {player} diamond\" - \"command:give {player} emerald\" - \"message:You got lucky!\" }",
                "random { - \"sound:ui.button.click\" - \"sound:entity.experience_orb.pickup\" }",
                "random { - \"economy:add:100\" - \"economy:add:200\" - \"economy:add:500\" }",
                "random { - \"message:Option 1\" - \"message:Option 2\" - \"message:Option 3\" }",
                "Weighted Examples (append @<weight>, default 1.0):",
                "random { - \"inventory:give:diamond:1@1.0\" - \"inventory:give:iron_ingot:8@9.0\" }",
                "random { - \"economy:add:1000@0.5\" - \"economy:add:100@5\" }",
                "random { - \"sound:entity.player.levelup:1.0:1.2@2.5\" - \"sound:ui.button.click@7.5\" }",
                "Legacy Format Examples:",
                "random:command:give {player} diamond|command:give {player} emerald|message:You got lucky!",
                "random:sound:ui.button.click|sound:entity.experience_orb.pickup",
                "random:economy:add:100|economy:add:200|economy:add:500",
                "random:message:Option 1|message:Option 2|message:Option 3",
                "random:inventory:give:diamond:1@1.0|inventory:give:iron_ingot:8@9.0"
        };
    }


    private WeightedAction parseWeightedAction(String actionString) {
        if (actionString == null || actionString.trim().isEmpty()) {
            return null;
        }

        String trimmed = actionString.trim();
        java.util.regex.Matcher matcher = WEIGHT_SUFFIX.matcher(trimmed);
        if (matcher.matches()) {
            try {
                double weight = Double.parseDouble(matcher.group(2));
                if (weight > 0) {
                    return new WeightedAction(matcher.group(1), weight);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return new WeightedAction(trimmed, 1.0);
    }

    private String stripWeightSuffix(String actionString) {
        if (actionString == null) {
            return null;
        }
        java.util.regex.Matcher matcher = WEIGHT_SUFFIX.matcher(actionString.trim());
        return matcher.matches() ? matcher.group(1) : actionString.trim();
    }

    private String selectWeightedRandom(List<WeightedAction> weightedActions) {

        double totalWeight = 0.0;
        for (WeightedAction action : weightedActions) {
            totalWeight += action.getWeight();
        }


        double randomValue = random.nextDouble() * totalWeight;


        double currentWeight = 0.0;
        for (WeightedAction action : weightedActions) {
            currentWeight += action.getWeight();
            if (randomValue <= currentWeight) {
                return action.getAction();
            }
        }


        return weightedActions.get(weightedActions.size() - 1).getAction();
    }
}

