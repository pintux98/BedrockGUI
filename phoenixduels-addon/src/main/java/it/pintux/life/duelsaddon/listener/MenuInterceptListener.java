package it.pintux.life.duelsaddon.listener;

import com.phoenixplugins.phoenixduels.lib.common.uicomponents.newest.container.holders.ContainerView;
import com.phoenixplugins.phoenixduels.lib.common.uicomponents.newest.layout.ContainerLayout;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.StatsKind;
import it.pintux.life.duelsaddon.service.BedrockDuelService;
import it.pintux.life.duelsaddon.service.BedrockKitService;
import it.pintux.life.duelsaddon.service.BedrockPartyService;
import it.pintux.life.duelsaddon.service.BedrockQueueService;
import it.pintux.life.duelsaddon.service.BedrockSettingsService;
import it.pintux.life.duelsaddon.service.BedrockSpectatorService;
import it.pintux.life.duelsaddon.service.BedrockStatsService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MenuInterceptListener implements Listener {

    @FunctionalInterface
    public interface Handler {
        void open(Player player, ContainerView view);
    }

    public static final Set<String> JAVA_ONLY_MENUS = DuelsMenus.JAVA_ONLY;

    public static final Set<String> CONTEXT_DEPENDENT_MENUS = DuelsMenus.CONTEXT_DEPENDENT;

    private final Plugin plugin;
    private final BedrockPlayerDetector detector;
    private final DuelsGateway gateway;
    private final Map<String, Handler> handlers = new HashMap<>();

    public MenuInterceptListener(Plugin plugin,
                                 DuelsAddonConfiguration config,
                                 BedrockPlayerDetector detector,
                                 DuelsGateway gateway,
                                 BedrockQueueService queueService,
                                 BedrockDuelService duelService,
                                 BedrockPartyService partyService,
                                 BedrockSettingsService settingsService,
                                 BedrockStatsService statsService,
                                 BedrockSpectatorService spectatorService,
                                 BedrockKitService kitService) {
        this.plugin = plugin;
        this.detector = detector;
        this.gateway = gateway;

        if (config.menuEnabled("queue")) {
            handlers.put("queue", (player, view) -> queueService.openMain(player));
            handlers.put("unranked_select_player_mode", (player, view) -> queueService.openPlayerModes(player, false));
            handlers.put("unranked_select_arena_mode", (player, view) -> queueService.openPlayerModes(player, false));
            handlers.put("ranked_select_player_mode", (player, view) -> queueService.openPlayerModes(player, true));
            handlers.put("ranked_select_arena_mode", (player, view) -> queueService.openPlayerModes(player, true));
        }
        if (config.menuEnabled("duel")) {
            handlers.put("duel_player", (player, view) -> duelService.openDuelPlayer(player, null));
            handlers.put("create_match", (player, view) -> duelService.openTargetPicker(player));
            handlers.put("custom_select_arena_mode", (player, view) -> duelService.openTargetPicker(player));
            handlers.put("lost_items", (player, view) -> duelService.openLostItems(player));
        }
        if (config.menuEnabled("party")) {
            handlers.put("party", (player, view) -> partyService.openMain(player));
            handlers.put("party_info", (player, view) -> partyService.openInfo(player, 1));
            handlers.put("party_invite_player", (player, view) -> partyService.openInvitePicker(player));
            handlers.put("party_manage_member", (player, view) -> partyService.openInfo(player, 1));
            handlers.put("party_ffa", (player, view) -> partyService.openFfa(player));
            handlers.put("party_teamfight", (player, view) -> partyService.openTeamFight(player));
            handlers.put("party_multiteam", (player, view) -> partyService.openMultiTeam(player));
            handlers.put("party_multiteam_spectators", (player, view) -> partyService.openSpectators(player));
            handlers.put("custom_challenge_opponent", (player, view) -> partyService.openChallengeOpponent(player, 1));
        }
        if (config.menuEnabled("settings")) {
            handlers.put("settings", (player, view) -> settingsService.openSettings(player));
        }
        if (config.menuEnabled("stats")) {
            handlers.put("stats", (player, view) -> statsService.openMain(player));
            handlers.put("leaderboard", (player, view) ->
                    statsService.openLeaderboard(player, StatsKind.UNRANKED, "wins", 1));
        }
        if (config.menuEnabled("spectator")) {
            handlers.put("ongoing_matches", (player, view) -> spectatorService.openMatches(player, 1));
            handlers.put("spectator_players", (player, view) -> spectatorService.openMatches(player, 1));
        }
        if (config.menuEnabled("kit")) {
            handlers.put("kit_preview", (player, view) -> kitService.openKitList(player));
        }
    }

    public Map<String, Handler> handlers() {
        return Map.copyOf(handlers);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!detector.isBedrockPlayer(player) || !gateway.isAvailable()) {
            return;
        }
        String id = menuId(event);
        if (id == null) {
            return;
        }
        Handler handler = handlers.get(id);
        if (handler == null) {
            return;
        }
        ContainerView view = (ContainerView) event.getInventory().getHolder();
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> handler.open(player, view));
    }

    private String menuId(InventoryOpenEvent event) {
        try {
            if (!(event.getInventory().getHolder() instanceof ContainerView view)) {
                return null;
            }
            if (!(view.getContainer() instanceof ContainerLayout layout)) {
                return null;
            }
            return layout.getId();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
