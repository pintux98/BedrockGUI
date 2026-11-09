package it.pintux.life.common.form.obj;

import it.pintux.life.common.actions.ActionSystem;

import java.util.List;
import java.util.StringJoiner;


public class FormButton {
    private final String text;
    private final String image;
    private final String onClick;
    private ActionSystem.ActionDefinition action;
    private String cooldownKey;
    private int cooldownSeconds;

    public FormButton(String text, String image, String onClick) {
        this.text = text;
        this.image = image;
        this.onClick = onClick;
    }

    public String getText() {
        return text;
    }

    public String getImage() {
        return image;
    }

    public String getOnClick() {
        return onClick;
    }

    public ActionSystem.ActionDefinition getAction() {
        return action;
    }

    public void setAction(ActionSystem.ActionDefinition action) {
        this.action = action;
    }

    public String getCooldownKey() {
        return cooldownKey;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldown(String key, int seconds) {
        this.cooldownKey = key;
        this.cooldownSeconds = seconds;
    }

    public List<ActionSystem.ActionDefinition> getAllActions() {
        return action != null ? List.of(action) : List.of();
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", FormButton.class.getSimpleName() + "[", "]")
                .add("text='" + text + "'")
                .add("image='" + image + "'");
        if (onClick != null) {
            joiner.add("onClick='" + onClick + "'");
        }
        if (action != null) {
            joiner.add("action=" + action);
        }
        return joiner.toString();
    }
}
