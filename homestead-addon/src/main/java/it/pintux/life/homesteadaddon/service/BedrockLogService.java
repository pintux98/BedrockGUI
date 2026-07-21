package it.pintux.life.homesteadaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.homesteadaddon.config.HomesteadAddonConfiguration;
import it.pintux.life.homesteadaddon.gateway.HomesteadGateway;
import it.pintux.life.homesteadaddon.model.LogView;
import it.pintux.life.homesteadaddon.model.RegionView;
import it.pintux.life.homesteadaddon.util.BukkitFormPlayer;
import it.pintux.life.homesteadaddon.util.Formatting;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BedrockLogService {
    private final HomesteadAddonConfiguration config;
    private final HomesteadGateway gateway;

    public BedrockLogService(HomesteadAddonConfiguration config, HomesteadGateway gateway) {
        this.config = config;
        this.gateway = gateway;
    }

    public void openLogs(Player player, long regionId, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        List<LogView> logs = gateway.logs(regionId);
        boolean manage = canManage(player, regionId);

        int perPage = config.itemsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil((double) logs.size() / perPage));
        int current = Math.max(1, Math.min(page, totalPages));
        int start = (current - 1) * perPage;
        int end = Math.min(start + perPage, logs.size());

        StringBuilder content = new StringBuilder();
        if (logs.isEmpty()) {
            content.append(config.text("logs.empty"));
        } else {
            for (int i = start; i < end; i++) {
                LogView log = logs.get(i);
                content.append(config.apply(config.text("logs.line"), Map.of(
                        "date", Formatting.date(log.sentAt()),
                        "author", log.author() == null ? "?" : log.author(),
                        "message", log.message() == null ? "" : log.message()))).append('\n');
            }
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("logs.title"), Map.of("name", region.name())));
        form.content(content.toString());

        if (current > 1) {
            int prev = current - 1;
            form.button(config.text("common.previous-button"), fp -> openLogs(player, regionId, prev));
        }
        if (current < totalPages) {
            int next = current + 1;
            form.button(config.text("common.next-button"), fp -> openLogs(player, regionId, next));
        }
        if (manage && !logs.isEmpty()) {
            form.button(config.text("logs.button-mark-read"), fp -> {
                gateway.markLogsRead(regionId);
                player.sendMessage(config.text("logs.marked-read"));
                openLogs(player, regionId, 1);
            });
            form.button(config.text("logs.button-clear"), fp -> confirmClear(player, region));
        }
        form.button(config.text("common.back-button"), fp -> navigate(fp, "hs_region_menu:" + regionId));
        form.send(new BukkitFormPlayer(player));
    }

    private void confirmClear(Player player, RegionView region) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        api.createModalForm(config.apply(config.text("logs.clear-title"), Map.of("name", region.name())),
                        config.text("logs.clear-content"))
                .button1(config.text("logs.button-clear"), fp -> {
                    if (canManage(player, region.id()) && gateway.clearLogs(region.id())) {
                        player.sendMessage(config.text("logs.cleared"));
                    }
                    openLogs(player, region.id(), 1);
                })
                .button2(config.text("logs.confirm-no"), fp -> openLogs(player, region.id(), 1))
                .send(new BukkitFormPlayer(player));
    }

    private boolean canManage(Player player, long regionId) {
        return player.hasPermission(BedrockRegionService.ADMIN_PERMISSION)
                || gateway.canManageLogs(regionId, player);
    }

    private void navigate(FormPlayer fp, String actionString) {
        try {
            BedrockGUIApi.getInstance().executeActionString(fp, actionString,
                    ActionSystem.ActionContext.builder().menuName("logs").formType("homestead").build());
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
