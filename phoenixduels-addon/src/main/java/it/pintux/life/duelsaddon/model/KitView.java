package it.pintux.life.duelsaddon.model;

import java.util.List;

/**
 * A premade kit, reduced to a readable item list.
 *
 * <p>Slots are deliberately dropped: a Bedrock form has no grid, so only the contents can be
 * shown. Editing a layout stays on Java, where drag-and-drop works.</p>
 */
public record KitView(String id, String displayName, List<KitItem> items) {

    /**
     * One stack in a kit, named as the player would see it.
     */
    public record KitItem(String name, int amount) {
    }
}
