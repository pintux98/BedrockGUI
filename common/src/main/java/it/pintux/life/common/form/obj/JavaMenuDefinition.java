package it.pintux.life.common.form.obj;

import java.util.Map;

public class JavaMenuDefinition {
    private final JavaMenuType type;
    private final String title;
    private final int size;
    private final Map<Integer, JavaMenuItem> items;
    private java.util.List<JavaMenuFill> fills;

    public JavaMenuDefinition(JavaMenuType type, String title, int size, Map<Integer, JavaMenuItem> items) {
        this.type = type;
        this.title = title;
        this.size = size;
        this.items = items;
    }

    public JavaMenuType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public Map<Integer, JavaMenuItem> getItems() {
        return items;
    }

    public java.util.List<JavaMenuFill> getFills() {
        return fills;
    }

    public void setFills(java.util.List<JavaMenuFill> fills) {
        this.fills = fills;
    }
}

