package it.pintux.life.homesteadaddon.service;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.homesteadaddon.api.BedrockPlayerDetector;
import it.pintux.life.homesteadaddon.config.HomesteadAddonConfiguration;
import it.pintux.life.homesteadaddon.gateway.HomesteadGateway;
import it.pintux.life.homesteadaddon.model.RegionView;
import it.pintux.life.homesteadaddon.util.BukkitFormPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BedrockRegionService {
    public static final String ADMIN_PERMISSION = "homesteadaddon.admin";

    private final HomesteadAddonConfiguration config;
    private final HomesteadGateway gateway;
    private final BedrockPlayerDetector detector;

    public BedrockRegionService(HomesteadAddonConfiguration config,
                                HomesteadGateway gateway,
                                BedrockPlayerDetector detector) {
        this.config = config;
        this.gateway = gateway;
        this.detector = detector;
    }

    public boolean shouldHandle(Player player) {
        return player != null && detector.isBedrockPlayer(player);
    }


    public void openRegionList(Player player, boolean showAll, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        boolean canShowAll = player.hasPermission(ADMIN_PERMISSION)
                || player.hasPermission("homestead.commands.homesteadadmin");
        boolean all = showAll && canShowAll;

        List<RegionView> regions = all ? gateway.allRegions() : gateway.regionsFor(player);
        if (regions.isEmpty() && !all) {
            player.sendMessage(config.text("region.list.empty"));
            return;
        }

        int perPage = config.itemsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil((double) regions.size() / perPage));
        int current = Math.max(1, Math.min(page, totalPages));
        int start = (current - 1) * perPage;
        int end = Math.min(start + perPage, regions.size());

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                all ? config.text("region.list.all-title") : config.text("region.list.title"));
        form.content(config.text("region.list.content"));

        if (canShowAll) {
            boolean nextAll = !all;
            form.button(all ? config.text("region.list.show-all-on") : config.text("region.list.show-all-off"),
                    fp -> openRegionList(player, nextAll, 1));
        }
        form.button(config.text("region.list.button-top"), fp -> openTopRegions(player, "BANK"));
        form.button(config.text("region.list.button-welcome"), fp -> openWelcomeSigns(player, 1));

        for (int i = start; i < end; i++) {
            RegionView region = regions.get(i);
            form.button(config.apply(config.text("region.list.button"), placeholders(region)),
                    fp -> openRegionMenu(player, region.id()));
        }

        if (current > 1) {
            int prev = current - 1;
            form.button(config.text("common.previous-button"), fp -> openRegionList(player, all, prev));
        }
        if (current < totalPages) {
            int next = current + 1;
            form.button(config.text("common.next-button"), fp -> openRegionList(player, all, next));
        }

        form.send(new BukkitFormPlayer(player));
    }


    public void openRegionMenu(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)
                && !region.isOwnedBy(player.getUniqueId())
                && !gateway.isMember(regionId, player)) {
            player.sendMessage(config.text("messages.no-permission"));
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("region.menu.title"), placeholders(region)));
        form.content(config.apply(config.text("region.menu.content"), placeholders(region)));

        form.button(config.text("region.menu.button-players"), fp ->
                api.executeActionString(fp, "hs_players:" + regionId, menuContext()));
        form.button(config.text("region.menu.button-flags"), fp ->
                api.executeActionString(fp, "hs_flags:" + regionId, menuContext()));
        form.button(config.text("region.menu.button-subareas"), fp ->
                api.executeActionString(fp, "hs_subareas:" + regionId, menuContext()));
        form.button(config.text("region.menu.button-chunks"), fp ->
                api.executeActionString(fp, "hs_chunks:" + regionId, menuContext()));
        form.button(config.text("region.menu.button-weather"), fp -> openWeatherTime(player, regionId));
        form.button(config.text("region.menu.button-levels"), fp ->
                api.executeActionString(fp, "hs_levels:" + regionId, menuContext()));
        form.button(config.text("region.menu.button-rewards"), fp ->
                api.executeActionString(fp, "hs_rewards:" + regionId, menuContext()));
        form.button(config.text("region.menu.button-logs"), fp ->
                api.executeActionString(fp, "hs_logs:" + regionId, menuContext()));
        form.button(config.text("region.menu.button-misc"), fp ->
                api.executeActionString(fp, "hs_misc:" + regionId, menuContext()));

        form.button(config.text("region.menu.button-info"), fp -> openRegionInfo(player, regionId));
        form.button(config.text("region.menu.button-teleport"), fp -> teleport(player, regionId));

        boolean owner = region.isOwnedBy(player.getUniqueId());
        if (!owner && gateway.isMember(regionId, player)) {
            form.button(config.text("region.menu.button-leave"), fp -> confirmLeave(player, region));
        }

        form.button(config.text("common.regions-button"), fp -> openRegionList(player, false, 1));
        form.send(new BukkitFormPlayer(player));
    }


    public void openRegionInfo(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("region.info.title"), placeholders(region)));
        form.content(config.apply(config.text("region.info.content"), placeholders(region)));
        form.button(config.text("region.info.button-rate"), fp ->
                api.executeActionString(fp, "hs_rate:" + regionId, menuContext()));
        form.button(config.text("common.back-button"), fp -> openRegionMenu(player, regionId));
        form.send(new BukkitFormPlayer(player));
    }


    public void teleport(Player player, long regionId) {
        if (!ensureAvailable(player)) {
            return;
        }
        Optional<RegionView> region = gateway.region(regionId);
        if (region.isEmpty()) {
            player.sendMessage(config.text("messages.region-not-found"));
            return;
        }
        if (gateway.teleport(player, regionId)) {
            player.sendMessage(config.apply(config.text("messages.teleport-success"),
                    Map.of("name", region.get().name())));
        } else {
            player.sendMessage(config.text("messages.teleport-failed"));
        }
    }

    private void confirmLeave(Player player, RegionView region) {
        BedrockGUIApi api = requireApi(player);
        if (api == null) {
            return;
        }
        api.createModalForm(
                        config.apply(config.text("region.menu.leave-title"), placeholders(region)),
                        config.apply(config.text("region.menu.leave-content"), placeholders(region)))
                .button1(config.text("region.menu.leave-yes"), fp -> {
                    if (gateway.leaveRegion(player, region.id())) {
                        player.sendMessage(config.apply(config.text("messages.left-region"),
                                Map.of("name", region.name())));
                        openRegionList(player, false, 1);
                    } else {
                        player.sendMessage(config.text("messages.cannot-leave"));
                    }
                })
                .button2(config.text("region.menu.leave-no"), fp -> openRegionMenu(player, region.id()))
                .send(new BukkitFormPlayer(player));
    }


    public void openTopRegions(Player player, String sorting) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        int limit = Math.max(1, config.number("top.limit", 10));
        List<RegionView> top = gateway.topRegions(sorting, limit);

        StringBuilder content = new StringBuilder();
        if (top.isEmpty()) {
            content.append(config.text("top.empty"));
        } else {
            int rank = 1;
            for (RegionView region : top) {
                content.append(config.apply(config.text("top.line"), Map.of(
                        "rank", String.valueOf(rank++),
                        "name", region.name(),
                        "value", topValue(sorting, region)))).append('\n');
            }
        }

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.text("top.title"));
        form.content(content.toString());
        form.button(config.text("top.sort-bank"), fp -> openTopRegions(player, "BANK"));
        form.button(config.text("top.sort-chunks"), fp -> openTopRegions(player, "CHUNKS_COUNT"));
        form.button(config.text("top.sort-members"), fp -> openTopRegions(player, "MEMBERS_COUNT"));
        form.button(config.text("top.sort-rating"), fp -> openTopRegions(player, "RATING"));
        form.button(config.text("top.sort-created"), fp -> openTopRegions(player, "CREATION_DATE"));
        form.button(config.text("common.regions-button"), fp -> openRegionList(player, false, 1));
        form.send(new BukkitFormPlayer(player));
    }

    private String topValue(String sorting, RegionView region) {
        return switch (sorting) {
            case "CHUNKS_COUNT" -> region.chunkCount() + " chunks";
            case "MEMBERS_COUNT" -> region.memberCount() + " members";
            case "CREATION_DATE" -> it.pintux.life.homesteadaddon.util.Formatting.date(region.createdAt());
            case "BANK" -> "$" + formatBank(region.bank());
            default -> region.ownerName();
        };
    }


    public void openWelcomeSigns(Player player, int page) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        List<RegionView> regions = gateway.welcomeSignRegions();

        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(config.text("welcome.title"));
        form.content(regions.isEmpty() ? config.text("welcome.empty") : config.text("welcome.content"));

        int perPage = config.itemsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil((double) regions.size() / perPage));
        int current = Math.max(1, Math.min(page, totalPages));
        int start = (current - 1) * perPage;
        int end = Math.min(start + perPage, regions.size());

        for (int i = start; i < end; i++) {
            RegionView region = regions.get(i);
            form.button(config.apply(config.text("welcome.entry-button"), placeholders(region)), fp -> {
                if (gateway.teleportToWelcomeSign(player, region.id())) {
                    player.sendMessage(config.apply(config.text("messages.teleport-success"), Map.of("name", region.name())));
                } else {
                    player.sendMessage(config.text("messages.teleport-failed"));
                }
            });
        }
        if (current > 1) {
            int prev = current - 1;
            form.button(config.text("common.previous-button"), fp -> openWelcomeSigns(player, prev));
        }
        if (current < totalPages) {
            int next = current + 1;
            form.button(config.text("common.next-button"), fp -> openWelcomeSigns(player, next));
        }
        form.button(config.text("common.regions-button"), fp -> openRegionList(player, false, 1));
        form.send(new BukkitFormPlayer(player));
    }


    public void openWeatherTime(Player player, long regionId) {
        BedrockGUIApi api = requireApi(player);
        if (api == null || !ensureAvailable(player)) {
            return;
        }
        RegionView region = requireRegion(player, regionId);
        if (region == null) {
            return;
        }
        boolean manage = player.hasPermission(ADMIN_PERMISSION)
                || gateway.isOwner(regionId, player)
                || gateway.hasControlFlag(regionId, player, "SET_WEATHER_AND_TIME");

        Map<String, String> ph = Map.of(
                "weather", gateway.weatherName(region.weather()),
                "time", gateway.timeName(region.time()));
        BedrockGUIApi.SimpleFormBuilder form = api.createSimpleForm(
                config.apply(config.text("weather.title"), Map.of("name", region.name())));
        form.content(config.apply(config.text("weather.content"), ph));
        if (manage) {
            form.button(config.text("weather.button-cycle-weather"), fp -> {
                gateway.cycleWeather(regionId);
                openWeatherTime(player, regionId);
            });
            form.button(config.text("weather.button-cycle-time"), fp -> {
                gateway.cycleTime(regionId);
                openWeatherTime(player, regionId);
            });
        }
        form.button(config.text("common.back-button"), fp -> openRegionMenu(player, regionId));
        form.send(new BukkitFormPlayer(player));
    }


    private Map<String, String> placeholders(RegionView region) {
        return Map.ofEntries(
                Map.entry("name", region.name()),
                Map.entry("owner", region.ownerName()),
                Map.entry("bank", formatBank(region.bank())),
                Map.entry("members", String.valueOf(region.memberCount())),
                Map.entry("chunks", String.valueOf(region.chunkCount())),
                Map.entry("subareas", String.valueOf(region.subAreaCount())),
                Map.entry("rank", region.globalRank() >= 0 ? String.valueOf(region.globalRank()) : "?"),
                Map.entry("public", region.publicRegion() ? "Yes" : "No"),
                Map.entry("weather", String.valueOf(region.weather())),
                Map.entry("time", String.valueOf(region.time()))
        );
    }

    private static String formatBank(double bank) {
        return String.format("%,.2f", bank);
    }

    private static ActionSystem.ActionContext menuContext() {
        return ActionSystem.ActionContext.builder()
                .menuName("region-menu")
                .formType("homestead")
                .build();
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
