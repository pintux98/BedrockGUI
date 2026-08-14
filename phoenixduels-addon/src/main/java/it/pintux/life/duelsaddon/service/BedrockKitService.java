package it.pintux.life.duelsaddon.service;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.KitView;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only kit previews.
 *
 * <p>Editing a kit layout is drag-and-drop, which a Bedrock form cannot express, so
 * {@code kit_items_editor} and {@code player_kit_layout} are left to open as Java inventories and
 * only the contents are mirrored here.</p>
 */
public final class BedrockKitService extends BedrockServiceSupport {

    public BedrockKitService(DuelsAddonConfiguration config, DuelsGateway gateway,
                             BedrockPlayerDetector detector) {
        super(config, gateway, detector);
    }

    public void openKitList(Player player) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        List<KitView> kits = gateway.kits();
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(text("kit.list-title"));
        form.content(text("kit.list-content"));
        for (KitView kit : kits) {
            form.button(kit.displayName(), fp -> openPreview(player, kit.id()));
        }
        form.button(text("common.close-button"), fp -> {
        });
        form.send(wrap(player));
    }

    public void openPreview(Player player, String kitId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        Optional<KitView> kit = kitId == null || kitId.isBlank() ? Optional.empty() : gateway.kit(kitId);
        if (kit.isEmpty()) {
            openKitList(player);
            return;
        }
        KitView view = kit.get();

        StringBuilder content = new StringBuilder();
        if (view.items().isEmpty()) {
            content.append(text("kit.preview-empty"));
        } else {
            content.append(text("kit.preview-content"));
            for (KitView.KitItem item : view.items()) {
                content.append(render("kit.preview-line", Map.of(
                        "amount", String.valueOf(item.amount()),
                        "item", item.name()))).append('\n');
            }
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                render("kit.preview-title", Map.of("kit", view.displayName())));
        form.content(content.toString());
        form.button(text("common.back-button"), fp -> openKitList(player));
        form.send(wrap(player));
    }
}
