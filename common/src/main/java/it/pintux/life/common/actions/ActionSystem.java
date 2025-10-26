package it.pintux.life.common.actions;

import it.pintux.life.common.utils.FormPlayer;

import java.util.*;

/**
 * Core action system containing all fundamental interfaces and data classes
 */
public class ActionSystem {

    // ==================== INTERFACES ====================

    /**
     * Interface for handling specific action types
     */
    public interface ActionHandler {
        String getActionType();

        ActionResult execute(FormPlayer player, String actionValue, ActionContext context);

        boolean isValidAction(String actionValue);

        String getDescription();

        String[] getUsageExamples();
    }

    // ==================== RESULT CLASSES ====================

    /**
     * Represents the result of an action execution
     */
    public record ActionResult(Status status, String message, Throwable exception, Object data) {

        public enum Status {
            SUCCESS,
            FAILURE,
            PARTIAL_SUCCESS,
            SKIPPED
        }

        public static ActionResult success() {
            return new ActionResult(Status.SUCCESS, null, null, null);
        }

        public static ActionResult success(String message) {
            return new ActionResult(Status.SUCCESS, message, null, null);
        }

        public static ActionResult success(String message, Object data) {
            return new ActionResult(Status.SUCCESS, message, null, data);
        }

        public static ActionResult failure(String message) {
            return new ActionResult(Status.FAILURE, message, null, null);
        }

        public static ActionResult failure(String message, Throwable exception) {
            return new ActionResult(Status.FAILURE, message, exception, null);
        }

        public static ActionResult partialSuccess(String message) {
            return new ActionResult(Status.PARTIAL_SUCCESS, message, null, null);
        }

        public static ActionResult skipped(String message) {
            return new ActionResult(Status.SKIPPED, message, null, null);
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public boolean isFailure() {
            return status == Status.FAILURE;
        }

        public boolean hasException() {
            return exception != null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ActionResult{status=").append(status);
            if (message != null) {
                sb.append(", message='").append(message).append("'");
            }
            if (exception != null) {
                sb.append(", exception=").append(exception.getClass().getSimpleName());
            }
            sb.append("}");
            return sb.toString();
        }
    }

    // ==================== CONTEXT CLASS ====================

    /**
     * Context information for action execution
     */
    public static class ActionContext {
        private final Map<String, String> placeholders;
        private final Map<String, Object> formResults;
        private final Map<String, Object> metadata;
        private final String menuName;
        private final String formType;

        private ActionContext(Builder builder) {
            this.placeholders = new HashMap<>(builder.placeholders);
            this.formResults = new HashMap<>(builder.formResults);
            this.metadata = new HashMap<>(builder.metadata);
            this.menuName = builder.menuName;
            this.formType = builder.formType;
        }

        public Map<String, String> getPlaceholders() {
            return new HashMap<>(placeholders);
        }

        public String getPlaceholder(String key) {
            return placeholders.get(key);
        }

        public Map<String, Object> getFormResults() {
            return new HashMap<>(formResults);
        }

        public Object getFormResult(String key) {
            return formResults.get(key);
        }

        public Map<String, Object> getMetadata() {
            return new HashMap<>(metadata);
        }

        public Object getMetadata(String key) {
            return metadata.get(key);
        }

        public String getMenuName() {
            return menuName;
        }

        public String getFormType() {
            return formType;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Map<String, String> placeholders = new HashMap<>();
            private Map<String, Object> formResults = new HashMap<>();
            private Map<String, Object> metadata = new HashMap<>();
            private String menuName;
            private String formType;

            public Builder placeholders(Map<String, String> placeholders) {
                if (placeholders != null) {
                    this.placeholders.putAll(placeholders);
                }
                return this;
            }

            public Builder placeholder(String key, String value) {
                this.placeholders.put(key, value);
                return this;
            }

            public Builder formResults(Map<String, Object> formResults) {
                if (formResults != null) {
                    this.formResults.putAll(formResults);
                }
                return this;
            }

            public Builder formResult(String key, Object value) {
                this.formResults.put(key, value);
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                if (metadata != null) {
                    this.metadata.putAll(metadata);
                }
                return this;
            }

            public Builder metadata(String key, Object value) {
                this.metadata.put(key, value);
                return this;
            }

            public Builder menuName(String menuName) {
                this.menuName = menuName;
                return this;
            }

            public Builder formType(String formType) {
                this.formType = formType;
                return this;
            }

            public ActionContext build() {
                return new ActionContext(this);
            }
        }

        @Override
        public String toString() {
            return "ActionContext{" +
                   "placeholders=" + placeholders +
                   ", formResults=" + formResults +
                   ", metadata=" + metadata +
                   ", menuName='" + menuName + '\'' +
                   ", formType='" + formType + '\'' +
                   '}';
        }
    }

    // ==================== DEFINITION CLASS ====================

    /**
     * Defines an action with its type and parameters
     */
    public static class ActionDefinition {
        private Map<String, Object> actions;

        public ActionDefinition() {
            this.actions = new LinkedHashMap<>();
        }

        public ActionDefinition(Map<String, Object> actions) {
            this.actions = actions != null ? new LinkedHashMap<>(actions) : new LinkedHashMap<>();
        }

        public Map<String, Object> getActions() {
            return actions;
        }

        public void setActions(Map<String, Object> actions) {
            this.actions = actions != null ? new LinkedHashMap<>(actions) : new LinkedHashMap<>();
        }

        public void addAction(String actionType, Object value) {
            actions.put(actionType, value);
        }

        public Object getAction(String actionType) {
            return actions.get(actionType);
        }

        public boolean hasAction(String actionType) {
            return actions.containsKey(actionType);
        }

        public void removeAction(String actionType) {
            actions.remove(actionType);
        }

        public Set<String> getActionTypes() {
            return actions.keySet();
        }

        public boolean isConditional() {
            return hasAction("conditional");
        }

        public boolean isMultiAction() {
            return actions.size() > 1 || (actions.size() == 1 && !isConditional());
        }

        public boolean isEmpty() {
            return actions.isEmpty();
        }

        public static ActionDefinition simple(String actionType, Object value) {
            ActionDefinition action = new ActionDefinition();
            action.addAction(actionType, value);
            return action;
        }

        public static List<ActionDefinition> multi(ActionDefinition... actions) {
            return Arrays.asList(actions);
        }

        @Override
        public String toString() {
            return "ActionDefinition{actions=" + actions + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ActionDefinition that = (ActionDefinition) o;
            return Objects.equals(actions, that.actions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(actions);
        }
    }

    // ==================== ACTION CLASS ====================

    /**
     * Represents a single action with metadata
     */
    public static class Action {
        private final ActionDefinition ActionDefinition;
        private final boolean critical;

        public Action(ActionDefinition ActionDefinition) {
            this(ActionDefinition, false);
        }

        public Action(ActionDefinition ActionDefinition, boolean critical) {
            this.ActionDefinition = ActionDefinition;
            this.critical = critical;
        }

        public ActionDefinition getActionDefinition() {
            return ActionDefinition;
        }

        public boolean isCritical() {
            return critical;
        }

        @Override
        public String toString() {
            return "Action{ActionDefinition=" + ActionDefinition + ", critical=" + critical + "}";
        }
    }
}

