package it.pintux.life.common.utils;

public interface MessageConfig {
    String getString(String path);

    String setPlaceholders(FormPlayer player, String message);

    String applyColor(String message);
}
