package it.pintux.life.homesteadaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.homesteadaddon.config.HomesteadAddonConfiguration;
import it.pintux.life.homesteadaddon.gateway.HomesteadGateway;
import it.pintux.life.homesteadaddon.model.LevelView;
import it.pintux.life.homesteadaddon.model.RegionView;
import it.pintux.life.homesteadaddon.model.RewardsView;
import it.pintux.life.homesteadaddon.util.BukkitFormPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public final class BedrockLevelService {
    private final HomesteadAddonConfiguration config;
    private final HomesteadGateway gateway;

    public BedrockLevelService(HomesteadAddonConfiguration config, HomesteadGateway gateway) {
        this.config = config;
        this.gateway = gateway;
    }

    public void openLevels(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        Optional<LevelView> level = gateway.level(regionId);
        String content;
        if (level.isEmpty()) {
            content = config.text("levels.no-data");
        } else {
            LevelView view = level.get();
            content = config.apply(config.text("levels.content"), Map.of(
                    "level", String.valueOf(view.level()),
                    "xp", String.valueOf(view.xpProgress()),
                    "xp_next", String.valueOf(view.xpForNextLevel()),
                    "progress", String.format("%.1f", view.progressPercent()),
                    "total_xp", String.valueOf(view.totalXp()),
                    "rank", view.rank() >= 0 ? String.valueOf(view.rank()) : "?"));
        }
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("levels.title"), Map.of("name", region.name())));
        form.content(content);
        form.button(config.text("common.back-button"), fp -> navigate(fp, "hs_region_menu:" + regionId));
        form.send(new BukkitFormPlayer(player));
    }

    public void openRewards(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        RewardsView rewards = gateway.rewards(regionId, player);
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("rewards.title"), Map.of("name", region.name())));
        form.content(config.apply(config.text("rewards.content"), Map.of(
                "chunks_per_member", String.valueOf(rewards.chunksPerMember()),
                "subareas_per_member", String.valueOf(rewards.subAreasPerMember()),
                "chunks_playtime", String.valueOf(rewards.chunksByPlaytime()),
                "subareas_playtime", String.valueOf(rewards.subAreasByPlaytime()))));
        form.button(config.text("common.back-button"), fp -> navigate(fp, "hs_region_menu:" + regionId));
        form.send(new BukkitFormPlayer(player));
    }

    private void navigate(FormPlayer fp, String actionString) {
        try {
            BedrockGUIApi.getInstance().executeActionString(fp, actionString,
                    ActionSystem.ActionContext.builder().menuName("levels").formType("homestead").build());
        } catch (IllegalStateException ignored) {
        }
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
