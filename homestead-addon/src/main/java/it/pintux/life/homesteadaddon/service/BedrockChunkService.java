package it.pintux.life.homesteadaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.homesteadaddon.config.HomesteadAddonConfiguration;
import it.pintux.life.homesteadaddon.gateway.HomesteadGateway;
import it.pintux.life.homesteadaddon.model.ChunkView;
import it.pintux.life.homesteadaddon.model.RegionView;
import it.pintux.life.homesteadaddon.util.BukkitFormPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BedrockChunkService {
    private final HomesteadAddonConfiguration config;
    private final HomesteadGateway gateway;

    public BedrockChunkService(HomesteadAddonConfiguration config, HomesteadGateway gateway) {
        this.config = config;
        this.gateway = gateway;
    }


    public void openClaimedChunks(Player player, long regionId, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        List<ChunkView> chunks = gateway.chunksOf(regionId);

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("chunks.title"), Map.of("name", region.name())));
        if (chunks.isEmpty()) {
            form.content(config.text("chunks.empty"));
        } else {
            form.content(config.text("chunks.content"));
        }

        int perPage = config.itemsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil((double) chunks.size() / perPage));
        int current = Math.max(1, Math.min(page, totalPages));
        int start = (current - 1) * perPage;
        int end = Math.min(start + perPage, chunks.size());

        for (int i = start; i < end; i++) {
            ChunkView chunk = chunks.get(i);
            form.button(config.apply(config.text("chunks.entry-button"), Map.of(
                            "world", chunk.worldName(),
                            "x", String.valueOf(chunk.x()),
                            "z", String.valueOf(chunk.z()))),
                    fp -> openChunkActions(player, regionId, chunk));
        }
        if (current > 1) {
            int prev = current - 1;
            form.button(config.text("common.previous-button"), fp -> openClaimedChunks(player, regionId, prev));
        }
        if (current < totalPages) {
            int next = current + 1;
            form.button(config.text("common.next-button"), fp -> openClaimedChunks(player, regionId, next));
        }
        form.button(config.text("common.back-button"), fp -> navigate(fp, "hs_region_menu:" + regionId));
        form.send(new BukkitFormPlayer(player));
    }

    private void openChunkActions(Player player, long regionId, ChunkView chunk) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        Map<String, String> ph = Map.of(
                "world", chunk.worldName(), "x", String.valueOf(chunk.x()), "z", String.valueOf(chunk.z()));
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("chunks.actions-title"), ph));
        form.content(config.apply(config.text("chunks.actions-content"), ph));
        form.button(config.text("chunks.button-teleport"), fp -> {
            if (gateway.teleportToChunk(player, chunk.worldId(), chunk.x(), chunk.z())) {
                player.sendMessage(config.text("chunks.teleport-success"));
            } else {
                player.sendMessage(config.text("chunks.teleport-failed"));
            }
        });
        if (gateway.canManageChunks(regionId, player) || player.hasPermission(BedrockRegionService.ADMIN_PERMISSION)) {
            form.button(config.text("chunks.button-unclaim"), fp -> confirmUnclaim(player, regionId, chunk));
        }
        form.button(config.text("common.back-button"), fp -> openClaimedChunks(player, regionId, 1));
        form.send(new BukkitFormPlayer(player));
    }

    private void confirmUnclaim(Player player, long regionId, ChunkView chunk) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        Map<String, String> ph = Map.of(
                "world", chunk.worldName(), "x", String.valueOf(chunk.x()), "z", String.valueOf(chunk.z()));
        api.createModalForm(config.apply(config.text("chunks.unclaim-title"), ph),
                        config.apply(config.text("chunks.unclaim-content"), ph))
                .button1(config.text("chunks.button-unclaim"), fp -> {
                    boolean allowed = gateway.canManageChunks(regionId, player)
                            || player.hasPermission(BedrockRegionService.ADMIN_PERMISSION);
                    if (allowed && gateway.unclaimChunk(regionId, chunk.worldId(), chunk.x(), chunk.z())) {
                        player.sendMessage(config.text("chunks.unclaim-success"));
                    }
                    openClaimedChunks(player, regionId, 1);
                })
                .button2(config.text("chunks.confirm-no"), fp -> openChunkActions(player, regionId, chunk))
                .send(new BukkitFormPlayer(player));
    }


    public void openMapColor(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null || !requireManage(player, regionId)) {
            return;
        }
        List<String> colors = gateway.mapColors();
        if (colors.isEmpty()) {
            player.sendMessage(config.text("map.unavailable"));
            return;
        }
        String currentName = gateway.mapColorName(region.mapColor());
        int def = Math.max(0, colors.indexOf(currentName));
        String label = config.text("map.color-label");
        api.createCustomForm(config.apply(config.text("map.color-title"), Map.of("name", region.name())))
                .dropdown(label, colors, def)
                .onSubmit(results -> {
                    Object value = results.get(componentName(label));
                    if (value != null && gateway.setMapColor(regionId, value.toString())) {
                        player.sendMessage(config.apply(config.text("map.color-success"), Map.of("value", value.toString())));
                    }
                    navigate(new BukkitFormPlayer(player), "hs_misc:" + regionId);
                })
                .send(new BukkitFormPlayer(player));
    }

    public void openMapIcon(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null || !requireManage(player, regionId)) {
            return;
        }
        List<String> icons = gateway.mapIcons();
        if (icons.isEmpty()) {
            player.sendMessage(config.text("map.unavailable"));
            return;
        }
        int def = region.mapIcon() != null ? Math.max(0, icons.indexOf(region.mapIcon())) : 0;
        String label = config.text("map.icon-label");
        api.createCustomForm(config.apply(config.text("map.icon-title"), Map.of("name", region.name())))
                .dropdown(label, icons, def)
                .onSubmit(results -> {
                    Object value = results.get(componentName(label));
                    if (value != null && gateway.setMapIcon(regionId, value.toString())) {
                        player.sendMessage(config.apply(config.text("map.icon-success"), Map.of("value", value.toString())));
                    }
                    navigate(new BukkitFormPlayer(player), "hs_misc:" + regionId);
                })
                .send(new BukkitFormPlayer(player));
    }


    private boolean requireManage(Player player, long regionId) {
        if (player.hasPermission(BedrockRegionService.ADMIN_PERMISSION) || gateway.isOwner(regionId, player)) {
            return true;
        }
        player.sendMessage(config.text("messages.no-permission"));
        return false;
    }

    private void navigate(FormPlayer fp, String actionString) {
        try {
            BedrockGUIApi.getInstance().executeActionString(fp, actionString,
                    ActionSystem.ActionContext.builder().menuName("chunks").formType("homestead").build());
        } catch (IllegalStateException ignored) {
        }
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
