package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.util.AddonText;
import it.pintux.life.duelsaddon.util.BukkitFormPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Shared plumbing for the form services: config text lookup, the BedrockGUI handle, the Bedrock
 * eligibility check, and list pagination.
 *
 * <p>Cumulus custom forms return their results keyed by a slug derived from each component's
 * label, so {@link #formValue} and {@link #formToggle} exist to read a component back by the same
 * label string it was created with, rather than by a positional index that shifts whenever a
 * component is added.</p>
 */
public abstract class BedrockServiceSupport {
    protected final DuelsAddonConfiguration config;
    protected final DuelsGateway gateway;
    protected final BedrockPlayerDetector detector;

    protected BedrockServiceSupport(DuelsAddonConfiguration config, DuelsGateway gateway,
                                    BedrockPlayerDetector detector) {
        this.config = config;
        this.gateway = gateway;
        this.detector = detector;
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player) && gateway.isAvailable();
    }

    protected BedrockGUIApi requireApi(Player player) {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            AddonText.send(player, config.text("messages.duels-unavailable"));
            return null;
        }
    }

    protected boolean ensureAvailable(Player player) {
        if (!gateway.isAvailable()) {
            AddonText.send(player, config.text("messages.duels-unavailable"));
            return false;
        }
        return true;
    }

    protected BukkitFormPlayer wrap(Player player) {
        return new BukkitFormPlayer(player);
    }

    protected String text(String path) {
        return config.text(path);
    }

    protected String render(String path, Map<String, String> placeholders) {
        return config.apply(config.text(path), placeholders);
    }

    protected void fail(Player player, String path) {
        AddonText.send(player, config.text(path));
    }

    protected String formValue(Map<String, Object> results, String label) {
        Object value = results.get(componentName(label));
        return value == null ? "" : value.toString().trim();
    }

    protected boolean formToggle(Map<String, Object> results, String label) {
        Object value = results.get(componentName(label));
        return value instanceof Boolean bool && bool;
    }

    protected static String componentName(String text) {
        return text.toLowerCase().replaceAll("\\s+", "_");
    }

    protected final class Pagination {
        public final int current;
        public final int totalPages;
        public final int start;
        public final int end;

        public Pagination(int size, int page) {
            int perPage = config.itemsPerPage();
            this.totalPages = Math.max(1, (int) Math.ceil((double) size / perPage));
            this.current = Math.max(1, Math.min(page, totalPages));
            this.start = (current - 1) * perPage;
            this.end = Math.min(start + perPage, size);
        }

        public void addNav(BedrockGUIApi.SimpleFormBuilder form, IntConsumer open) {
            if (current > 1) {
                form.button(text("common.previous-button"), fp -> open.accept(current - 1));
            }
            if (current < totalPages) {
                form.button(text("common.next-button"), fp -> open.accept(current + 1));
            }
        }
    }
}
