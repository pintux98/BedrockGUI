package it.pintux.life.homesteadaddon;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.homesteadaddon.action.HomesteadFormAction;
import it.pintux.life.homesteadaddon.api.BedrockPlayerDetector;
import it.pintux.life.homesteadaddon.command.HomesteadAddonCommand;
import it.pintux.life.homesteadaddon.config.HomesteadAddonConfiguration;
import it.pintux.life.homesteadaddon.gateway.HomesteadGateway;
import it.pintux.life.homesteadaddon.gateway.HomesteadGatewayImpl;
import it.pintux.life.homesteadaddon.listener.HomesteadCommandListener;
import it.pintux.life.homesteadaddon.service.BedrockChunkService;
import it.pintux.life.homesteadaddon.service.BedrockFlagService;
import it.pintux.life.homesteadaddon.service.BedrockLevelService;
import it.pintux.life.homesteadaddon.service.BedrockLogService;
import it.pintux.life.homesteadaddon.service.BedrockMemberService;
import it.pintux.life.homesteadaddon.service.BedrockMiscService;
import it.pintux.life.homesteadaddon.service.BedrockRegionService;
import it.pintux.life.homesteadaddon.service.BedrockSubAreaService;
import it.pintux.life.homesteadaddon.service.FloodgateBedrockPlayerDetector;
import it.pintux.life.homesteadaddon.util.HomesteadActionPayloads;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class HomesteadAddonPlugin extends JavaPlugin {
    private HomesteadAddonConfiguration configuration;
    private BedrockPlayerDetector detector;
    private HomesteadGateway gateway;
    private BedrockRegionService regionService;
    private BedrockMemberService memberService;
    private BedrockFlagService flagService;
    private BedrockSubAreaService subAreaService;
    private BedrockLevelService levelService;
    private BedrockLogService logService;
    private BedrockMiscService miscService;
    private BedrockChunkService chunkService;

    @Override
    public void onEnable() {
        configuration = HomesteadAddonConfiguration.load(this);
        detector = new FloodgateBedrockPlayerDetector();
        gateway = new HomesteadGatewayImpl(getLogger());

        PluginCommand command = getCommand("homesteadaddon");
        if (command != null) {
            HomesteadAddonCommand executor = new HomesteadAddonCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        setupModules();

        if (!gateway.isAvailable()) {
            getLogger().warning("Homestead is not installed (or its API changed). "
                    + "Forms will report 'Homestead unavailable' until it is present.");
        }
    }

    @Override
    public void onDisable() {
        regionService = null;
        memberService = null;
        flagService = null;
        subAreaService = null;
        levelService = null;
        logService = null;
        miscService = null;
        chunkService = null;
    }

    private void setupModules() {
        regionService = new BedrockRegionService(configuration, gateway, detector);
        memberService = new BedrockMemberService(configuration, gateway);
        flagService = new BedrockFlagService(configuration, gateway);
        subAreaService = new BedrockSubAreaService(configuration, gateway);
        levelService = new BedrockLevelService(configuration, gateway);
        logService = new BedrockLogService(configuration, gateway);
        miscService = new BedrockMiscService(configuration, gateway);
        chunkService = new BedrockChunkService(configuration, gateway);

        boolean integratedGui = configuration.integratedGuiEnabled();

        if (integratedGui) {
            Bukkit.getPluginManager().registerEvents(new HomesteadCommandListener(regionService), this);
        } else {
            getLogger().info("Integrated GUI disabled: not intercepting Homestead commands. "
                    + "Use the hs_* actions from your own forms.");
        }

        BedrockGUIApi api = getApiSafely();
        if (api != null && (integratedGui || configuration.registerActionsEnabled())) {
            registerActions(api);
        }
    }

    private void registerActions(BedrockGUIApi api) {
        register(api, "hs_regions", "Open the region list",
                (p, v) -> regionService.openRegionList(p, false, page(v)));
        register(api, "hs_region_menu", "Open a region",
                (p, v) -> regionService.openRegionMenu(p, HomesteadActionPayloads.regionId(v)));
        register(api, "hs_region_info", "Open region info",
                (p, v) -> regionService.openRegionInfo(p, HomesteadActionPayloads.regionId(v)));

        register(api, "hs_players", "Open players management",
                (p, v) -> memberService.openPlayersManagement(p, HomesteadActionPayloads.regionId(v)));
        register(api, "hs_player_info", "Open member detail",
                (p, v) -> memberService.openPlayerInfo(p, HomesteadActionPayloads.regionId(v), HomesteadActionPayloads.member(v)));

        register(api, "hs_flags", "Open flags chooser",
                (p, v) -> flagService.openFlagsChooser(p, HomesteadActionPayloads.regionId(v)));
        register(api, "hs_member_flags", "Open member flags",
                (p, v) -> flagService.openMemberFlags(p, HomesteadActionPayloads.regionId(v), HomesteadActionPayloads.member(v)));
        register(api, "hs_control_flags", "Open control flags",
                (p, v) -> flagService.openMemberControlFlags(p, HomesteadActionPayloads.regionId(v), HomesteadActionPayloads.member(v)));

        register(api, "hs_subareas", "Open sub-areas list",
                (p, v) -> subAreaService.openSubAreasList(p, HomesteadActionPayloads.regionId(v), 1));
        register(api, "hs_subarea_menu", "Open a sub-area",
                (p, v) -> subAreaService.openSubAreaMenu(p, HomesteadActionPayloads.regionId(v)));
        register(api, "hs_subarea_members", "Open sub-area members",
                (p, v) -> subAreaService.openSubAreaMembers(p, HomesteadActionPayloads.regionId(v), 1));
        register(api, "hs_subarea_member", "Open sub-area member actions",
                (p, v) -> subAreaService.openSubAreaMemberActions(p, HomesteadActionPayloads.regionId(v), HomesteadActionPayloads.member(v)));
        register(api, "hs_subarea_flags", "Open sub-area flags",
                (p, v) -> flagService.openSubAreaFlags(p, HomesteadActionPayloads.regionId(v)));
        register(api, "hs_subarea_member_flags", "Open sub-area member flags",
                (p, v) -> flagService.openSubAreaMemberFlags(p, HomesteadActionPayloads.regionId(v), HomesteadActionPayloads.member(v)));

        register(api, "hs_levels", "Open region levels",
                (p, v) -> levelService.openLevels(p, HomesteadActionPayloads.regionId(v)));
        register(api, "hs_rewards", "Open region rewards",
                (p, v) -> levelService.openRewards(p, HomesteadActionPayloads.regionId(v)));
        register(api, "hs_logs", "Open region logs",
                (p, v) -> logService.openLogs(p, HomesteadActionPayloads.regionId(v), 1));
        register(api, "hs_misc", "Open region settings",
                (p, v) -> miscService.openMiscSettings(p, HomesteadActionPayloads.regionId(v)));
        register(api, "hs_rate", "Open region rating",
                (p, v) -> miscService.openRating(p, HomesteadActionPayloads.regionId(v)));

        register(api, "hs_chunks", "Open claimed chunks",
                (p, v) -> chunkService.openClaimedChunks(p, HomesteadActionPayloads.regionId(v), 1));
        register(api, "hs_map_color", "Open map color picker",
                (p, v) -> chunkService.openMapColor(p, HomesteadActionPayloads.regionId(v)));
        register(api, "hs_map_icon", "Open map icon picker",
                (p, v) -> chunkService.openMapIcon(p, HomesteadActionPayloads.regionId(v)));
        register(api, "hs_top", "Open top regions",
                (p, v) -> regionService.openTopRegions(p, v == null || v.isBlank() ? "BANK" : v));
        register(api, "hs_welcome", "Open welcome-sign regions",
                (p, v) -> regionService.openWelcomeSigns(p, 1));
        register(api, "hs_weather_time", "Open weather/time",
                (p, v) -> regionService.openWeatherTime(p, HomesteadActionPayloads.regionId(v)));

        getLogger().info("Registered Homestead addon actions with BedrockGUI API");
    }

    private void register(BedrockGUIApi api, String type, String description, HomesteadFormAction.Callback callback) {
        api.registerActionHandler(new HomesteadFormAction(type, description, callback));
    }

    private static int page(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private BedrockGUIApi getApiSafely() {
        try {
            return BedrockGUIApi.getInstance();
        } catch (IllegalStateException e) {
            getLogger().warning("BedrockGUI API not found yet. Actions will be unavailable until it loads.");
            return null;
        }
    }

    public void reloadConfiguration() {
        HandlerList.unregisterAll(this);
        configuration = HomesteadAddonConfiguration.load(this);
        setupModules();
    }

    public HomesteadAddonConfiguration getConfiguration() {
        return configuration;
    }

    public HomesteadGateway getGateway() {
        return gateway;
    }

    public BedrockRegionService getRegionService() {
        return regionService;
    }

    public BedrockMemberService getMemberService() {
        return memberService;
    }

    public BedrockFlagService getFlagService() {
        return flagService;
    }

    public BedrockSubAreaService getSubAreaService() {
        return subAreaService;
    }

    public BedrockLevelService getLevelService() {
        return levelService;
    }

    public BedrockLogService getLogService() {
        return logService;
    }

    public BedrockMiscService getMiscService() {
        return miscService;
    }

    public BedrockChunkService getChunkService() {
        return chunkService;
    }
}
