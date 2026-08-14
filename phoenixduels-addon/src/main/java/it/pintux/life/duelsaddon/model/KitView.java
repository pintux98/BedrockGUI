package it.pintux.life.duelsaddon.model;

import java.util.List;

public record KitView(String id, String displayName, List<KitItem> items) {

    public record KitItem(String name, int amount) {
    }
}
