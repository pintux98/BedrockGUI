package it.pintux.life.common.actions.handlers;
import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.actions.ActionSystem;

import it.pintux.life.common.actions.*;
import it.pintux.life.common.actions.ActionRegistry;
import it.pintux.life.common.utils.ConditionEvaluator;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionalActionHandler extends BaseActionHandler {

    private final ActionExecutor actionExecutor;

    private static final Pattern CHECK_PATTERN = Pattern.compile(
            "check:\\s*(?:\"([^\"]+)\"|([^\\r\\n]+))"
    );

    private static final Pattern TRUE_ACTIONS_PATTERN = Pattern.compile(
            "true:\\s*([\\s\\S]*?)(?=\\s*false:|\\Z)", Pattern.DOTALL
    );

    private static final Pattern FALSE_ACTIONS_PATTERN = Pattern.compile(
            "false:\\s*([\\s\\S]*?)(?=\\Z)", Pattern.DOTALL
    );

    private static final Pattern ACTION_LINE_PATTERN = Pattern.compile(
            "-\\s*\"((?:[^\"\\\\]|\\\\.)*)\""
    );

    private static final String[] BRANCH_KEYS = {"check:", "true:", "false:"};

    public ConditionalActionHandler(ActionExecutor actionExecutor) {
        this.actionExecutor = actionExecutor;
    }

    private static final class Branches {
        private String check;
        private String trueBody = "";
        private String falseBody = "";
    }

    /**
     * Splits a conditional into its check / true / false parts.
     *
     * <p>Scanning is brace- and quote-aware, so a nested conditional keeps its own
     * {@code true:} and {@code false:} keys instead of terminating the outer branch.
     */
    private Branches splitBranches(String actionData) {
        Branches branches = new Branches();
        String body = innerBody(actionData);

        List<int[]> keys = topLevelKeyPositions(body);
        for (int i = 0; i < keys.size(); i++) {
            int[] key = keys.get(i);
            int valueStart = key[0] + key[1];
            int valueEnd = i + 1 < keys.size() ? keys.get(i + 1)[0] : body.length();
            String value = body.substring(valueStart, valueEnd).trim();

            switch (key[2]) {
                case 0:
                    branches.check = stripQuotes(value);
                    break;
                case 1:
                    branches.trueBody = value;
                    break;
                case 2:
                    branches.falseBody = value;
                    break;
                default:
                    break;
            }
        }
        return branches;
    }

    /** @return the text between a leading "conditional {" and its matching brace, or the input as given */
    private static String innerBody(String actionData) {
        String trimmed = actionData.trim();
        int open = trimmed.indexOf('{');
        if (open < 0 || !trimmed.substring(0, open).trim().equalsIgnoreCase("conditional")) {
            return trimmed;
        }
        int close = matchingBrace(trimmed, open);
        return close < 0 ? trimmed.substring(open + 1) : trimmed.substring(open + 1, close);
    }

    /** @return index of the brace closing the one at openIndex, or -1 */
    private static int matchingBrace(String s, int openIndex) {
        int depth = 0;
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = openIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (inQuotes && c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (inQuotes) {
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** @return {startIndex, keyLength, keyId} for each check:/true:/false: sitting at brace depth 0 */
    private static List<int[]> topLevelKeyPositions(String body) {
        List<int[]> found = new ArrayList<>();
        int depth = 0;
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (inQuotes && c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (inQuotes) {
                continue;
            }
            if (c == '{') {
                depth++;
                continue;
            }
            if (c == '}') {
                if (depth > 0) depth--;
                continue;
            }
            if (depth != 0) {
                continue;
            }
            if (i > 0 && !Character.isWhitespace(body.charAt(i - 1)) && body.charAt(i - 1) != '-') {
                continue;
            }
            for (int k = 0; k < BRANCH_KEYS.length; k++) {
                if (body.regionMatches(true, i, BRANCH_KEYS[k], 0, BRANCH_KEYS[k].length())) {
                    found.add(new int[]{i, BRANCH_KEYS[k].length(), k});
                    i += BRANCH_KEYS[k].length() - 1;
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Splits a branch body into its individual actions.
     *
     * <p>An entry starts at a "-" list marker at brace depth 0, so a nested block keeps its own
     * markers. The optional YAML block indicator after the dash is dropped.
     */
    private static List<String> splitBranchEntries(String branchBody) {
        List<String> entries = new ArrayList<>();
        if (branchBody == null || branchBody.trim().isEmpty()) {
            return entries;
        }

        int depth = 0;
        boolean inQuotes = false;
        boolean escaped = false;
        List<Integer> starts = new ArrayList<>();

        for (int i = 0; i < branchBody.length(); i++) {
            char c = branchBody.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (inQuotes && c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (inQuotes) {
                continue;
            }
            if (c == '{') {
                depth++;
                continue;
            }
            if (c == '}') {
                if (depth > 0) depth--;
                continue;
            }
            if (depth == 0 && c == '-'
                    && (i == 0 || Character.isWhitespace(branchBody.charAt(i - 1)))
                    && i + 1 < branchBody.length()
                    && (Character.isWhitespace(branchBody.charAt(i + 1)) || branchBody.charAt(i + 1) == '|')) {
                starts.add(i);
            }
        }

        if (starts.isEmpty()) {
            String single = cleanEntry(branchBody);
            if (!single.isEmpty()) {
                entries.add(single);
            }
            return entries;
        }

        for (int i = 0; i < starts.size(); i++) {
            int from = starts.get(i) + 1;
            int to = i + 1 < starts.size() ? starts.get(i + 1) : branchBody.length();
            String entry = cleanEntry(branchBody.substring(from, to));
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static String cleanEntry(String raw) {
        String entry = raw.trim();
        if (entry.startsWith("|")) {
            entry = entry.substring(1).trim();
        }
        return stripQuotes(entry);
    }

    private static String stripQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\\\"", "\"");
        }
        return trimmed;
    }

    @Override
    public String getActionType() {
        return "conditional";
    }

    @Override
    public ActionSystem.ActionResult execute(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        if (actionData == null || actionData.trim().isEmpty()) {
            logger.warn("Conditional action called with empty data for player: " + player.getName());
            return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "No conditional data specified"), player);
        }

        try {
            String processedData = processPlaceholders(actionData.trim(), context, player);
            return executeNewFormat(player, processedData, context);

        } catch (Exception e) {
            logger.error("Error executing conditional action for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "Error executing conditional action: " + e.getMessage()), player);
        }
    }


    private ActionSystem.ActionResult executeNewFormat(FormPlayer player, String actionData, ActionSystem.ActionContext context) {
        try {

            Branches branches = splitBranches(actionData);
            if (branches.check == null || branches.check.trim().isEmpty()) {
                return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "No check condition found in conditional"), player);
            }

            String checkCondition = branches.check;
            boolean conditionMet = evaluateExpression(player, checkCondition.trim(), context);

            List<String> actionsToExecute;
            if (conditionMet) {
                actionsToExecute = splitBranchEntries(branches.trueBody);
                //logger.info("Condition met for player " + player.getName() + ", executing " + actionsToExecute.size() + " success actions");
            } else {
                actionsToExecute = splitBranchEntries(branches.falseBody);
                if (actionsToExecute.isEmpty()) {
                    //logger.info("Condition not met for player " + player.getName() + ": " + checkCondition + " (no failure actions specified)");
                    return createSuccessResult(MessageData.ACTION_SUCCESS, createReplacements("message", "Condition not met, action skipped"), player);
                }
                //logger.info("Condition not met for player " + player.getName() + ", executing " + actionsToExecute.size() + " failure actions");
            }


            ActionSystem.ActionResult lastResult = null;
            for (String action : actionsToExecute) {
                ActionSystem.Action parsed = actionExecutor.parseAction(action);
                ActionSystem.ActionDefinition actionDef = parsed != null ? parsed.getActionDefinition() : parseActionString(action);
                ActionSystem.ActionResult result = actionExecutor.executeAction(player, actionDef, context);
                lastResult = result;

                if (!result.isSuccess()) {
                    logger.warn("Action failed during conditional execution: " + action + " - " + result.message());

                }
            }

            if (lastResult != null && lastResult.isSuccess()) {
                return createSuccessResult(MessageData.ACTION_SUCCESS, createReplacements("message", "Conditional actions executed successfully"), player);
            } else {
                return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "Some conditional actions failed"), player);
            }

        } catch (Exception e) {
            logger.error("Error executing new format conditional for player " + player.getName() + ": " + e.getMessage());
            return createFailureResult(MessageData.EXECUTION_ERROR, createReplacements("error", "Error parsing new conditional format: " + e.getMessage()), player);
        }
    }


    private boolean evaluateComplexCondition(FormPlayer player, String condition, ActionSystem.ActionContext context) {

        if (condition.contains("||")) {
            String[] orParts = condition.split("\\|\\|");
            for (String orPart : orParts) {
                if (evaluateComplexCondition(player, orPart.trim(), context)) {
                    return true;
                }
            }
            return false;
        }

        if (condition.contains("&&")) {
            String[] andParts = condition.split("&&");
            for (String andPart : andParts) {
                if (!evaluateComplexCondition(player, andPart.trim(), context)) {
                    return false;
                }
            }
            return true;
        }

        return evaluateSingleCondition(player, condition.trim(), context);
    }

    private boolean evaluateExpression(FormPlayer player, String expr, ActionSystem.ActionContext context) {
        java.util.List<String> tokens = tokenize(expr);
        java.util.Deque<String> output = new java.util.ArrayDeque<>();
        java.util.Deque<String> ops = new java.util.ArrayDeque<>();
        java.util.Map<String, Integer> prec = java.util.Map.of("||", 1, "&&", 2);
        for (String t : tokens) {
            if ("(".equals(t)) {
                ops.push(t);
            } else if (")".equals(t)) {
                while (!ops.isEmpty() && !"(".equals(ops.peek())) {
                    output.push(ops.pop());
                }
                if (!ops.isEmpty() && "(".equals(ops.peek())) ops.pop();
            } else if ("||".equals(t) || "&&".equals(t)) {
                while (!ops.isEmpty() && prec.getOrDefault(ops.peek(), 0) >= prec.getOrDefault(t, 0)) {
                    output.push(ops.pop());
                }
                ops.push(t);
            } else {
                output.push(t);
            }
        }
        while (!ops.isEmpty()) output.push(ops.pop());
        java.util.Deque<Boolean> stack = new java.util.ArrayDeque<>();
        java.util.List<String> rpn = new java.util.ArrayList<>(output);
        java.util.Collections.reverse(rpn);
        for (String t : rpn) {
            if ("||".equals(t)) {
                boolean b = stack.pop() | stack.pop();
                stack.push(b);
            } else if ("&&".equals(t)) {
                boolean b = stack.pop() & stack.pop();
                stack.push(b);
            } else {
                stack.push(evaluateComplexCondition(player, t.trim(), context));
            }
        }
        return stack.isEmpty() ? false : stack.pop();
    }

    private java.util.List<String> tokenize(String s) {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == '(' || c == ')') { tokens.add(String.valueOf(c)); i++; continue; }
            if (i + 1 < s.length()) {
                String two = s.substring(i, i + 2);
                if ("||".equals(two) || "&&".equals(two)) { tokens.add(two); i += 2; continue; }
            }
            if (s.startsWith("placeholder:", i) || s.startsWith("permission:", i)) {
                int j = i;
                while (j < s.length()) {
                    if (j + 1 < s.length()) {
                        String two = s.substring(j, j + 2);
                        if ("||".equals(two) || "&&".equals(two)) break;
                    }
                    char cj = s.charAt(j);
                    if (cj == ')') break;
                    j++;
                }
                tokens.add(s.substring(i, j).trim());
                i = j;
                continue;
            }
            int j = i;
            while (j < s.length()) {
                char cj = s.charAt(j);
                if (cj == '(' || cj == ')' || Character.isWhitespace(cj)) break;
                if (j + 1 < s.length()) {
                    String two = s.substring(j, j + 2);
                    if ("||".equals(two) || "&&".equals(two)) break;
                }
                j++;
            }
            tokens.add(s.substring(i, j));
            i = j;
        }
        return tokens;
    }

    private boolean evaluateSingleCondition(FormPlayer player, String condition, ActionSystem.ActionContext context) {

        if (condition.startsWith("placeholder:")) {
            return evaluatePlaceholderCondition(player, condition, context);
        } else if (condition.startsWith("permission:")) {
            return evaluatePermissionCondition(player, condition, context);
        } else {
            logger.warn("Unknown condition type in: " + condition);
            return false;
        }
    }


    private boolean evaluatePlaceholderCondition(FormPlayer player, String condition, ActionSystem.ActionContext context) {

        String conditionPart = condition.substring("placeholder:".length());


        String[] operators = {">=", "<=", "==", "!=", ">", "<"};
        String operator = null;
        String[] parts = null;

        for (String op : operators) {
            if (conditionPart.contains(" " + op + " ")) {
                operator = op;
                parts = conditionPart.split(" " + Pattern.quote(op) + " ", 2);
                break;
            }
        }

        if (operator == null || parts == null || parts.length != 2) {
            logger.warn("Invalid placeholder condition format: " + condition);
            return false;
        }

        String placeholder = parts[0].trim();
        String expectedValue = parts[1].trim();


        String conditionString = "placeholder:" + placeholder + ":" + operator + ":" + expectedValue;
        return ConditionEvaluator.evaluateCondition(player, conditionString, context, BedrockGUIApi.getInstance().getMessageData());
    }


    private boolean evaluatePermissionCondition(FormPlayer player, String condition, ActionSystem.ActionContext context) {
        String permission = condition.substring("permission:".length()).trim();
        String conditionString = "permission:" + permission;
        return ConditionEvaluator.evaluateCondition(player, conditionString, context, BedrockGUIApi.getInstance().getMessageData());
    }


    private List<String> parseActionList(String actionData, Pattern pattern) {
        List<String> actions = new ArrayList<>();
        Matcher matcher = pattern.matcher(actionData);

        if (matcher.find()) {
            String actionsBlock = matcher.group(1);
            // First, handle multiple inline '- |' actions on a single line
            java.util.regex.Matcher inlineMulti = java.util.regex.Pattern.compile("-\\s*\\|\\s*").matcher(actionsBlock);
            if (inlineMulti.find()) {
                java.util.regex.Matcher splitter = java.util.regex.Pattern.compile("-\\s*\\|\\s*(.+?)(?=\\s*-\\s*\\||\\Z)", java.util.regex.Pattern.DOTALL).matcher(actionsBlock);
                while (splitter.find()) {
                    String content = splitter.group(1).trim();
                    if (!content.isEmpty()) {
                        actions.add(content);
                    }
                }
                return actions;
            }

            // Handle inline actions without '|', e.g., "- message { ... } - server { ... }"
            java.util.regex.Matcher inlineNoPipe = java.util.regex.Pattern.compile("-\\s+(\\w+[^\\{]*\\{[\\s\\S]*?\\})(?=\\s*-\\s+|\\Z)", java.util.regex.Pattern.DOTALL).matcher(actionsBlock);
            boolean anyNoPipe = false;
            while (inlineNoPipe.find()) {
                String content = inlineNoPipe.group(1).trim();
                if (!content.isEmpty()) {
                    actions.add(content);
                    anyNoPipe = true;
                }
            }
            if (anyNoPipe) {
                return actions;
            }

            String[] lines = actionsBlock.split("\\r?\\n");
            int i = 0;
            while (i < lines.length) {
                String line = lines[i];
                if (line.trim().isEmpty()) { i++; continue; }

                Matcher pipeMatcher = Pattern.compile("^\\s*-\\s*\\|\\s*$").matcher(line);
                if (pipeMatcher.matches()) {
                    int dashIndex = line.indexOf('-');
                    String indent = dashIndex > 0 ? line.substring(0, dashIndex) : "";
                    StringBuilder block = new StringBuilder();
                    i++;
                    while (i < lines.length) {
                        String next = lines[i];
                        if (next.startsWith(indent + "-")) {
                            break;
                        }
                        block.append(next).append("\n");
                        i++;
                    }
                    String content = block.toString().trim();
                    if (!content.isEmpty()) {
                        actions.add(content);
                    }
                    continue;
                }

                Matcher simpleMatcher = ACTION_LINE_PATTERN.matcher(line);
                if (simpleMatcher.find()) {
                    String value = simpleMatcher.group(1).trim();
                    if (!value.isEmpty()) {
                        actions.add(value);
                    }
                } else {
                    Matcher inlinePipe = Pattern.compile("^\\s*-\\s*\\|\\s*(.+)$").matcher(line);
                    if (inlinePipe.find()) {
                        String content = inlinePipe.group(1).trim();
                        if (!content.isEmpty()) {
                            actions.add(content);
                        }
                    }
                }
                // Fallback: action line without '|', capture single-line action
                Matcher plainMatcher = Pattern.compile("^\\s*-\\s*(.+)$").matcher(line);
                if (plainMatcher.matches()) {
                    String content = plainMatcher.group(1).trim();
                    if (!content.isEmpty()) {
                        actions.add(content);
                    }
                }
                i++;
            }
        }

        return actions;
    }


    private ActionSystem.ActionDefinition parseActionString(String actionString) {
        ActionSystem.ActionDefinition actionDef = new ActionSystem.ActionDefinition();


        actionString = actionString.replaceAll("^\"|\"$", "");


        int colonIndex = actionString.indexOf(':');
        if (colonIndex != -1) {
            String actionType = actionString.substring(0, colonIndex);
            String actionData = actionString.substring(colonIndex + 1);
            actionDef.addAction(actionType, actionData);
        } else {

            actionDef.addAction("command", actionString);
        }

        return actionDef;
    }

    @Override
    public boolean isValidAction(String actionValue) {
        if (actionValue == null || actionValue.trim().isEmpty()) {
            return false;
        }
        String trimmed = actionValue.trim();
        if (isNewCurlyBraceFormat(trimmed, "conditional")) {
            Branches branches = splitBranches(trimmed);
            if (branches.check == null || branches.check.trim().isEmpty()) {
                return false;
            }
            return !splitBranchEntries(branches.trueBody).isEmpty()
                    || !splitBranchEntries(branches.falseBody).isEmpty();
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "Executes actions based on conditions like permissions or placeholder values";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[]{};
    }
}

