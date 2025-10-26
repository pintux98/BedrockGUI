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
        if (!validationResult.isSuccess()) {
            return validationResult;
        }

        try {

            String processedData = processPlaceholders(actionData.trim(), context, player);


            String[] actionOptions = processedData.split("\\|");

            if (actionOptions.length == 0) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, null, player)), player);
            }


            List<WeightedAction> weightedActions = new ArrayList<>();
            for (String action : actionOptions) {
                String trimmedAction = action.trim();
                if (!trimmedAction.isEmpty()) {
                    WeightedAction weightedAction = parseWeightedAction(trimmedAction);
                    if (weightedAction != null) {
                        weightedActions.add(weightedAction);
                    }
                }
            }

            if (weightedActions.isEmpty()) {
                MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, null, player)), player);
            }


            String selectedAction = selectWeightedRandom(weightedActions);

            logger.info("Selected random action for player " + player.getName() + ": " + selectedAction + " (1/" + weightedActions.size() + ")");


            CompletableFuture<ActionSystem.ActionResult> future = CompletableFuture.supplyAsync(() -> {

                ActionSystem.Action parsedAction = actionExecutor.parseAction(selectedAction);
                if (parsedAction == null) {
                    MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
                    HashMap<String, Object> replacements = new HashMap<>();
                    replacements.put("action", selectedAction);
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", messageData.getValueNoPrefix(MessageData.ACTION_INVALID_FORMAT, replacements, player)), player);
                }

                return actionExecutor.executeAction(player, parsedAction.getActionDefinition(), context);
            }, executorService);

            try {
                ActionSystem.ActionResult result = future.get();
                if (result.isSuccess()) {
                    return createSuccessResult("ACTION_SUCCESS", createReplacements("message", "Executed random action: " + selectedAction), player);
                } else {
                    return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Random action failed: " + result.message()), player);
                }
            } catch (Exception e) {
                logger.error("Error waiting for random action execution: " + e.getMessage());
                return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Random action execution interrupted: " + e.getMessage()), player);
            }

        } catch (Exception e) {
            logger.error("Error executing random action for player " + player.getName() + ": " + e.getMessage());
            MessageData messageData = BedrockGUIApi.getInstance().getMessageData();
            HashMap<String, Object> replacements = new HashMap<>();
            replacements.put("error", e.getMessage());
            return createFailureResult("ACTION_EXECUTION_ERROR", createReplacements("error", "Failed to execute random action: " + e.getMessage()), player);
        }
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }


        String[] actionOptions = actionValue.trim().split("\\|");

        if (actionOptions.length == 0) {
            return false;
        }


        for (String action : actionOptions) {
            String trimmedAction = action.trim();
            if (!trimmedAction.isEmpty() && trimmedAction.contains(":")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Randomly selects and executes one action from a list of possible actions";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{
                "random:command:give {player} diamond|command:give {player} emerald|message:You got lucky!",
                "random:sound:ui.button.click|sound:entity.experience_orb.pickup",
                "random:economy:add:100|economy:add:200|economy:add:500",
                "random:message:Option 1|message:Option 2|message:Option 3"
        };
    }


    private WeightedAction parseWeightedAction(String actionString) {
        if (actionString == null || actionString.trim().isEmpty()) {
            return null;
        }

        String trimmed = actionString.trim();


        String[] parts = trimmed.split(":");
        if (parts.length >= 3) {

            String lastPart = parts[parts.length - 1];
            try {
                double weight = Double.parseDouble(lastPart);
                if (weight > 0) {

                    StringBuilder actionBuilder = new StringBuilder();
                    for (int i = 0; i < parts.length - 1; i++) {
                        if (i > 0) actionBuilder.append(":");
                        actionBuilder.append(parts[i]);
                    }
                    return new WeightedAction(actionBuilder.toString(), weight);
                }
            } catch (NumberFormatException e) {

            }
        }


        return new WeightedAction(trimmed, 1.0);
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

