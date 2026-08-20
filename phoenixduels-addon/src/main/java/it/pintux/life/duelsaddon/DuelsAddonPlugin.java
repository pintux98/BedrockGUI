package it.pintux.life.duelsaddon;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.duelsaddon.action.DuelsFormAction;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.command.DuelsAddonCommand;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.gateway.DuelsGatewayImpl;
import it.pintux.life.duelsaddon.listener.InvitationListener;
import it.pintux.life.duelsaddon.listener.MenuInterceptListener;
import it.pintux.life.duelsaddon.model.StatsKind;
import it.pintux.life.duelsaddon.model.TeamSize;
import it.pintux.life.duelsaddon.service.BedrockConfirmationService;
import it.pintux.life.duelsaddon.service.BedrockDuelService;
import it.pintux.life.duelsaddon.service.BedrockInvitationService;
import it.pintux.life.duelsaddon.service.BedrockKitService;
import it.pintux.life.duelsaddon.service.BedrockPartyService;
import it.pintux.life.duelsaddon.service.BedrockQueueService;
import it.pintux.life.duelsaddon.service.BedrockSettingsService;
import it.pintux.life.duelsaddon.service.BedrockSpectatorService;
import it.pintux.life.duelsaddon.service.BedrockStatsService;
import it.pintux.life.duelsaddon.service.FloodgateBedrockPlayerDetector;
import it.pintux.life.duelsaddon.util.DuelsActionPayloads;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Serves PhoenixDuels' UI as Bedrock forms.
 *
 * <p>Everything is wired in {@link #setupModules()} rather than in {@code onEnable}, so
 * {@link #reloadConfiguration()} can unregister the listeners and rebuild the whole graph. That is
 * what makes enabling a menu group in {@code config.yml} take effect on reload instead of needing a
 * restart.</p>
 */
public final class DuelsAddonPlugin extends JavaPlugin {
    private DuelsAddonConfiguration configuration;
    private BedrockPlayerDetector detector;
    private DuelsGateway gateway;

    private BedrockQueueService queueService;
    private BedrockDuelService duelService;
    private BedrockPartyService partyService;
    private BedrockSettingsService settingsService;
    private BedrockStatsService statsService;
    private BedrockSpectatorService spectatorService;
    private BedrockKitService kitService;
    private BedrockConfirmationService confirmationService;
    private BedrockInvitationService invitationService;
    private MenuInterceptListener menuInterceptListener;
    private final java.util.List<String> registeredActions = new java.util.ArrayList<>();

    @Override
    public void onEnable() {
        configuration = DuelsAddonConfiguration.load(this);
        detector = new FloodgateBedrockPlayerDetector();
        gateway = new DuelsGatewayImpl(getLogger());

        PluginCommand command = getCommand("duelsaddon");
        if (command != null) {
            DuelsAddonCommand executor = new DuelsAddonCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        setupModules();

        if (!gateway.isAvailable()) {
            getLogger().warning("PhoenixDuels is not installed (or its internals changed). "
                    + "Bedrock forms stay disabled and PhoenixDuels' own Java menus are left untouched.");
        } else {
            int resolved = menuInterceptListener.resolver().refresh();
            getLogger().info("Hooked into PhoenixDuels (" + gateway.edition() + "). "
                    + menuInterceptListener.handlers().size() + " menus served as Bedrock forms, "
                    + resolved + " of " + it.pintux.life.duelsaddon.listener.DuelsMenus.ALL.size()
                    + " registry keys resolved.");
            if (resolved == 0) {
                getLogger().warning("PhoenixDuels' MenuRegistry resolved no menus. Interception will "
                        + "do nothing, so Bedrock players keep the Java chest menus. Set debug: true "
                        + "in config.yml to log every menu open.");
            }
        }
    }

    @Override
    public void onDisable() {
        unregisterActions();
        if (invitationService != null) {
            invitationService.clear();
        }
        queueService = null;
        duelService = null;
        partyService = null;
        settingsService = null;
        statsService = null;
        spectatorService = null;
        kitService = null;
        confirmationService = null;
        invitationService = null;
        menuInterceptListener = null;
    }

    private void setupModules() {
        statsService = new BedrockStatsService(configuration, gateway, detector);
        partyService = new BedrockPartyService(configuration, gateway, detector);
        queueService = new BedrockQueueService(configuration, gateway, detector, partyService, statsService);
        duelService = new BedrockDuelService(configuration, gateway, detector);
        settingsService = new BedrockSettingsService(configuration, gateway, detector);
        spectatorService = new BedrockSpectatorService(configuration, gateway, detector);
        kitService = new BedrockKitService(configuration, gateway, detector);
        confirmationService = new BedrockConfirmationService(this, configuration, gateway, detector);
        invitationService = new BedrockInvitationService(configuration, gateway, detector);
        duelService.setInvitationService(invitationService);

        menuInterceptListener = new MenuInterceptListener(this, configuration, detector, gateway,
                new MenuInterceptListener.Services(queueService, duelService, partyService,
                        settingsService, statsService, spectatorService, kitService,
                        confirmationService));

        boolean integratedGui = configuration.integratedGuiEnabled();

        if (integratedGui) {
            Bukkit.getPluginManager().registerEvents(menuInterceptListener, this);
        } else {
            getLogger().info("Integrated GUI disabled: PhoenixDuels menus are not intercepted. "
                    + "Use the pd_* actions from your own forms.");
        }

        if (configuration.partyInviteFormsEnabled() || configuration.duelInviteFormsEnabled()) {
            Bukkit.getPluginManager().registerEvents(
                    new InvitationListener(this, configuration, gateway, invitationService, duelService),
                    this);
        }

        BedrockGUIApi api = getApiSafely();
        if (api != null && (integratedGui || configuration.registerActionsEnabled())) {
            registerActions(api);
        }
    }

    private void registerActions(BedrockGUIApi api) {
        register(api, "pd_queue", "Open the duels queue menu",
                (p, v) -> queueService.openMain(p));
        register(api, "pd_queue_sizes", "Open the team size picker",
                (p, v) -> queueService.openPlayerModes(p, DuelsActionPayloads.ranked(v)));
        register(api, "pd_queue_modes", "Open the mode list for a team size",
                (p, v) -> queueService.openModes(p, DuelsActionPayloads.ranked(v),
                        TeamSize.parse(DuelsActionPayloads.size(v)), 1));
        register(api, "pd_queue_join", "Join a queue directly",
                (p, v) -> queueService.joinById(p, DuelsActionPayloads.ranked(v),
                        TeamSize.parse(DuelsActionPayloads.size(v)), DuelsActionPayloads.queueMode(v)));

        register(api, "pd_duel", "Open the duel form for a player",
                (p, v) -> duelService.openDuelPlayer(p, DuelsActionPayloads.playerName(v)));
        register(api, "pd_duel_targets", "Open the duel target picker",
                (p, v) -> duelService.openTargetPicker(p));
        register(api, "pd_lost_items", "Open the lost items form",
                (p, v) -> duelService.openLostItems(p));

        register(api, "pd_party", "Open the party menu",
                (p, v) -> partyService.openMain(p));
        register(api, "pd_party_info", "Open the party member list",
                (p, v) -> partyService.openInfo(p, DuelsActionPayloads.page(v, 1)));
        register(api, "pd_party_invite", "Open the party invite picker",
                (p, v) -> partyService.openInvitePicker(p));
        register(api, "pd_party_member", "Open a party member's actions",
                (p, v) -> partyService.openManageMember(p, DuelsActionPayloads.uuid(v)));
        register(api, "pd_party_ffa", "Open the party free-for-all picker",
                (p, v) -> partyService.openFfa(p));
        register(api, "pd_party_teamfight", "Open the party team fight picker",
                (p, v) -> partyService.openTeamFight(p));
        register(api, "pd_party_multiteam", "Open the party multi-team picker",
                (p, v) -> partyService.openMultiTeam(p));
        register(api, "pd_party_challenge", "Open the opponent party picker",
                (p, v) -> partyService.openChallengeOpponent(p, DuelsActionPayloads.page(v, 1)));

        register(api, "pd_settings", "Open the duel settings form",
                (p, v) -> settingsService.openSettings(p));

        register(api, "pd_stats", "Open a player's stats",
                (p, v) -> statsService.openStats(p, StatsKind.parse(DuelsActionPayloads.first(v))));
        register(api, "pd_leaderboard", "Open the leaderboard",
                (p, v) -> statsService.openLeaderboard(p, StatsKind.parse(DuelsActionPayloads.first(v)),
                        "wins", 1));

        register(api, "pd_matches", "Open the ongoing matches list",
                (p, v) -> spectatorService.openMatches(p, DuelsActionPayloads.page(v, 1)));
        register(api, "pd_spectate", "Spectate the match of a player uuid",
                (p, v) -> spectatorService.spectate(p, DuelsActionPayloads.uuid(v)));

        register(api, "pd_kits", "Open the kit list",
                (p, v) -> kitService.openKitList(p));
        register(api, "pd_kit_preview", "Preview a kit",
                (p, v) -> kitService.openPreview(p, DuelsActionPayloads.first(v)));

        getLogger().info("Registered PhoenixDuels addon actions with BedrockGUI API");
    }

    private void register(BedrockGUIApi api, String type, String description, DuelsFormAction.Callback callback) {
        api.registerActionHandler(new DuelsFormAction(type, description, callback));
        registeredActions.add(type);
    }

    /**
     * Hands the {@code pd_*} action types back to BedrockGUI.
     *
     * <p>Without this the handlers outlive the plugin: BedrockGUI's registry belongs to BedrockGUI,
     * not to this jar, so unloading through PlugMan or a hot swap would leave stale handlers behind
     * that still point at services from the discarded classloader.</p>
     */
    private void unregisterActions() {
        if (registeredActions.isEmpty()) {
            return;
        }
        try {
            BedrockGUIApi api = BedrockGUIApi.getInstance();
            for (String type : registeredActions) {
                api.getActionRegistry().unregisterHandler(type);
            }
        } catch (Throwable ignored) {
        }
        registeredActions.clear();
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
        unregisterActions();
        configuration = DuelsAddonConfiguration.load(this);
        setupModules();
    }

    public DuelsAddonConfiguration getConfiguration() {
        return configuration;
    }

    public DuelsGateway getGateway() {
        return gateway;
    }

    public MenuInterceptListener getMenuInterceptListener() {
        return menuInterceptListener;
    }

    public BedrockQueueService getQueueService() {
        return queueService;
    }

    public BedrockDuelService getDuelService() {
        return duelService;
    }

    public BedrockPartyService getPartyService() {
        return partyService;
    }

    public BedrockSettingsService getSettingsService() {
        return settingsService;
    }

    public BedrockStatsService getStatsService() {
        return statsService;
    }

    public BedrockSpectatorService getSpectatorService() {
        return spectatorService;
    }

    public BedrockKitService getKitService() {
        return kitService;
    }
}
