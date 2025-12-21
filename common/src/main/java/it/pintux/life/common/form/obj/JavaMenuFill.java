package it.pintux.life.common.form.obj;

import it.pintux.life.common.actions.ActionSystem;

import java.util.List;

public class JavaMenuFill {
    private final JavaFillType type;
    private final Integer row;
    private final Integer column;
    private final JavaMenuItem item;

    private final List<ActionSystem.Action> actions;

    public JavaMenuFill(JavaFillType type, Integer row, Integer column, JavaMenuItem item, List<ActionSystem.Action> actions) {
        this.type = type;
        this.row = row;
        this.column = column;
        this.item = item;
        this.actions = actions;
    }

    public JavaFillType getType() {
        return type;
    }

    public Integer getRow() {
        return row;
    }

    public Integer getColumn() {
        return column;
    }

    public JavaMenuItem getItem() {
        return item;
    }

    public List<ActionSystem.Action> getActions() {
        return actions;
    }
}

