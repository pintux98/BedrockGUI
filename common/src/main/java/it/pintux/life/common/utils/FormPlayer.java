package it.pintux.life.common.utils;

import java.util.UUID;

public interface FormPlayer {
    UUID getUniqueId();

    String getName();

    void sendMessage(String message);

    boolean executeAction(String action);

    boolean hasPermission(String permission);
}
