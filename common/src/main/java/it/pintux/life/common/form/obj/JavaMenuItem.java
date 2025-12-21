package it.pintux.life.common.form.obj;

import it.pintux.life.common.actions.ActionSystem;

import java.util.List;

public class JavaMenuItem {
    private String material;
    private int amount;
    private String name;
    private List<String> lore;
    private boolean glow;
    private java.util.List<ActionSystem.Action> actions;

    public JavaMenuItem(String material, int amount, String name, List<String> lore, boolean glow) {
        this.material = material;
        this.amount = amount;
        this.name = name;
        this.lore = lore;
        this.glow = glow;
    }

    public String getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public boolean isGlow() {
        return glow;
    }

    public java.util.List<ActionSystem.Action> getActions() {
        return actions;
    }

    public void setActions(java.util.List<ActionSystem.Action> actions) {
        this.actions = actions;
    }
}

