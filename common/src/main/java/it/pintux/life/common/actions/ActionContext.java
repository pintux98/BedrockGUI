package it.pintux.life.common.actions;

import java.util.HashMap;
import java.util.Map;


public class ActionContext {

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
