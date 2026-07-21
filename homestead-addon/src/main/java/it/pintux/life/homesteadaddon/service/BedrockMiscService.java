package it.pintux.life.homesteadaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.homesteadaddon.config.HomesteadAddonConfiguration;
import it.pintux.life.homesteadaddon.gateway.HomesteadGateway;
import it.pintux.life.homesteadaddon.model.RatingView;
import it.pintux.life.homesteadaddon.model.RegionView;
import it.pintux.life.homesteadaddon.util.BukkitFormPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public final class BedrockMiscService {
    private final HomesteadAddonConfiguration config;
    private final HomesteadGateway gateway;

    public BedrockMiscService(HomesteadAddonConfiguration config, HomesteadGateway gateway) {
        this.config = config;
        this.gateway = gateway;
    }


    public void openMiscSettings(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        if (!canManage(player, regionId)) {
            player.sendMessage(config.text("messages.no-permission"));
            return;
        }
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("misc.title"), Map.of("name", region.name())));
        form.content(config.text("misc.content"));
        form.button(config.text("misc.button-rename"), fp -> showTextForm(player, regionId,
                "misc.rename-title", "misc.rename-label", region.name(), (id, value) -> {
                    gateway.renameRegion(id, value);
                    player.sendMessage(config.apply(config.text("misc.rename-success"), Map.of("name", value)));
                }));
        form.button(config.text("misc.button-displayname"), fp -> showTextForm(player, regionId,
                "misc.displayname-title", "misc.displayname-label", "", (id, value) -> {
                    gateway.setDisplayName(id, value);
                    player.sendMessage(config.text("misc.displayname-success"));
                }));
        form.button(config.text("misc.button-description"), fp -> showTextForm(player, regionId,
                "misc.description-title", "misc.description-label", "", (id, value) -> {
                    gateway.setDescription(id, value);
                    player.sendMessage(config.text("misc.description-success"));
                }));
        form.button(config.text("misc.button-set-spawn"), fp -> {
            gateway.setRegionSpawn(regionId, player);
            player.sendMessage(config.text("misc.set-spawn-success"));
            openMiscSettings(player, regionId);
        });
        form.button(config.text("misc.button-transfer"), fp -> showTransferForm(player, regionId));
        form.button(config.text("misc.button-map-color"), fp -> navigate(fp, "hs_map_color:" + regionId));
        form.button(config.text("misc.button-map-icon"), fp -> navigate(fp, "hs_map_icon:" + regionId));
        form.button(config.text("misc.button-delete"), fp -> confirmDelete(player, region));
        form.button(config.text("common.back-button"), fp -> navigate(fp, "hs_region_menu:" + regionId));
        form.send(new BukkitFormPlayer(player));
    }

    private void showTextForm(Player player, long regionId, String titleKey, String labelKey, String current, Applier applier) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !requireManage(player, regionId)) {
            return;
        }
        String label = config.text(labelKey);
        api.createCustomForm(config.text(titleKey))
                .input(label, "", current)
                .onSubmit(results -> {
                    String value = string(results, label);
                    if (value.isBlank()) {
                        player.sendMessage(config.text("misc.invalid-input"));
                    } else {
                        applier.apply(regionId, value);
                    }
                    openMiscSettings(player, regionId);
                })
                .send(new BukkitFormPlayer(player));
    }

    private void showTransferForm(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !requireManage(player, regionId)) {
            return;
        }
        String label = config.text("misc.transfer-label");
        api.createCustomForm(config.text("misc.transfer-title"))
                .input(label, config.text("misc.transfer-placeholder"), "")
                .onSubmit(results -> {
                    String name = string(results, label);
                    if (name.isBlank()) {
                        player.sendMessage(config.text("misc.invalid-input"));
                        openMiscSettings(player, regionId);
                        return;
                    }
                    if (gateway.transferOwnership(regionId, name)) {
                        player.sendMessage(config.apply(config.text("misc.transfer-success"), Map.of("player", name)));
                    } else {
                        player.sendMessage(config.text("misc.transfer-failed"));
                        openMiscSettings(player, regionId);
                    }
                })
                .send(new BukkitFormPlayer(player));
    }

    private void confirmDelete(Player player, RegionView region) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        Map<String, String> ph = Map.of("name", region.name());
        api.createModalForm(config.apply(config.text("misc.delete-title"), ph),
                        config.apply(config.text("misc.delete-content"), ph))
                .button1(config.text("misc.button-delete"), fp -> {
                    if (canManage(player, region.id()) && gateway.deleteRegion(region.id(), player)) {
                        player.sendMessage(config.apply(config.text("misc.delete-success"), ph));
                        navigate(new BukkitFormPlayer(player), "hs_regions:");
                    } else {
                        openMiscSettings(player, region.id());
                    }
                })
                .button2(config.text("misc.confirm-no"), fp -> openMiscSettings(player, region.id()))
                .send(new BukkitFormPlayer(player));
    }


    public void openRating(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        RatingView rating = gateway.rating(regionId, player);
        int def = rating.playerScore() >= 1 && rating.playerScore() <= 5 ? rating.playerScore() : 3;
        String label = config.apply(config.text("rating.slider-label"), Map.of(
                "average", String.format("%.1f", rating.average()),
                "count", String.valueOf(rating.count())));
        api.createCustomForm(config.apply(config.text("rating.title"), Map.of("name", region.name())))
                .slider(label, 1, 5, 1, def)
                .onSubmit(results -> {
                    Object value = results.get(componentName(label));
                    int score = value instanceof Number n ? n.intValue() : def;
                    if (gateway.rateRegion(regionId, player, score)) {
                        player.sendMessage(config.apply(config.text("rating.success"), Map.of("score", String.valueOf(score))));
                    }
                })
                .send(new BukkitFormPlayer(player));
    }


    @FunctionalInterface
    private interface Applier {
        void apply(long regionId, String value);
    }

    private boolean canManage(Player player, long regionId) {
        return player.hasPermission(BedrockRegionService.ADMIN_PERMISSION) || gateway.isOwner(regionId, player);
    }

    private boolean requireManage(Player player, long regionId) {
        if (!canManage(player, regionId)) {
            player.sendMessage(config.text("messages.no-permission"));
            return false;
        }
        return true;
    }

    private void navigate(FormPlayer fp, String actionString) {
        try {
            BedrockGUIApi.getInstance().executeActionString(fp, actionString,
                    ActionSystem.ActionContext.builder().menuName("misc").formType("homestead").build());
        } catch (IllegalStateException ignored) {
        }
    }

    private String string(Map<String, Object> results, String label) {
        Object value = results.get(componentName(label));
        return value == null ? "" : value.toString().trim();
    }

    private static String componentName(String text) {
        return text.toLowerCase().replaceAll("\\s+", "_");
    }

    private RegionView requireRegion(Player player, long regionId) {
        Optional<RegionView> region = gateway.region(regionId);
        if (region.isEmpty()) {
            player.sendMessage(config.text("messages.region-not-found"));
            return null;
        }
        return region.get();
    }

    private boolean ensureAvailable(Player player) {
        if (!gateway.isAvailable()) {
            player.sendMessage(config.text("messages.homestead-unavailable"));
            return false;
        }
        return true;
    }

    private BedrockGUIApi requireApi(Player player) {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            player.sendMessage(config.text("messages.homestead-unavailable"));
            return null;
        }
    }
}
