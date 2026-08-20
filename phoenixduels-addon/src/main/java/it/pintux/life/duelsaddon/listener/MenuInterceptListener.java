package it.pintux.life.duelsaddon.listener;

import com.phoenixplugins.phoenixduels.lib.common.uicomponents.newest.container.holders.ContainerView;
import com.phoenixplugins.phoenixduels.lib.common.uicomponents.newest.layout.ContainerLayout;
import it.pintux.life.duelsaddon.api.BedrockPlayerDetector;
import it.pintux.life.duelsaddon.config.DuelsAddonConfiguration;
import it.pintux.life.duelsaddon.gateway.DuelsGateway;
import it.pintux.life.duelsaddon.model.StatsKind;
import it.pintux.life.duelsaddon.service.BedrockConfirmationService;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Swaps PhoenixDuels' chest menus for Bedrock forms.
 *
 * <p>Identification is exact rather than title-based. Every PhoenixDuels menu is a
 * {@link ContainerLayout} opened through a {@link ContainerView}, and {@code ContainerView} is the
 * inventory's holder, so the menu is identifiable straight off {@link InventoryOpenEvent} before a
 * single slot renders. That avoids the chest flash and the next-tick flag polling that title
 * matching forces.</p>
 *
 * <p>The layout is resolved to a registry key through {@link PhoenixMenuResolver}, <em>not</em>
 * through {@code ContainerLayout.getId()} — that method returns an incrementing counter, not the
 * key.</p>
 *
 * <p>Only ids in {@link DuelsMenus#GROUPS} are cancelled. Anything else — the drag-and-drop
 * inventories, the generic pickers, the admin editor — falls through untouched so the Bedrock
 * player gets the real Java UI instead of a broken form.</p>
 */
public final class MenuInterceptListener implements Listener {

    /**
     * Opens the Bedrock form that replaces one PhoenixDuels menu.
     */
    @FunctionalInterface
    public interface Handler {
        /**
         * @param player the Bedrock player the menu was opening for
         * @param view   the cancelled PhoenixDuels view, whose metadata carries the flow's context
         */
        void open(Player player, ContainerView view);
    }

    /**
     * The services a handler can route to, so {@link #handlerFor} stays testable without Bukkit.
     */
    public record Services(BedrockQueueService queue,
                           BedrockDuelService duel,
                           BedrockPartyService party,
                           BedrockSettingsService settings,
                           BedrockStatsService stats,
                           BedrockSpectatorService spectator,
                           BedrockKitService kit,
                           BedrockConfirmationService confirmation) {
    }

    private final Plugin plugin;
    private final DuelsAddonConfiguration config;
    private final BedrockPlayerDetector detector;
    private final DuelsGateway gateway;
    private final Map<String, Handler> handlers;
    private final PhoenixMenuResolver resolver = new PhoenixMenuResolver();

    public MenuInterceptListener(Plugin plugin,
                                 DuelsAddonConfiguration config,
                                 BedrockPlayerDetector detector,
                                 DuelsGateway gateway,
                                 Services services) {
        this.plugin = plugin;
        this.config = config;
        this.detector = detector;
        this.gateway = gateway;
        this.handlers = buildHandlers(config, services);
    }

    /**
     * @return the shared registry resolver, so the admin command can report how many menus
     *         PhoenixDuels currently resolves
     */
    public PhoenixMenuResolver resolver() {
        return resolver;
    }

    /**
     * Walks {@link DuelsMenus#GROUPS} so the id list lives in exactly one place.
     *
     * @throws IllegalStateException if an id is declared without a matching handler, which means
     *                               {@link DuelsMenus} and {@link #handlerFor} have drifted apart
     */
    private static Map<String, Handler> buildHandlers(DuelsAddonConfiguration config, Services services) {
        Map<String, Handler> handlers = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : DuelsMenus.GROUPS.entrySet()) {
            Handler handler = handlerFor(entry.getKey(), services);
            if (handler == null) {
                throw new IllegalStateException("No handler for declared PhoenixDuels menu id: " + entry.getKey());
            }
            if (config.menuEnabled(entry.getValue())) {
                handlers.put(entry.getKey(), handler);
            }
        }
        return Map.copyOf(handlers);
    }

    /**
     * Maps a PhoenixDuels menu id to the form that replaces it.
     *
     * <p>{@code services} is only captured by the returned lambda, never dereferenced here, so
     * tests can pass a record of nulls to assert every declared id resolves.</p>
     *
     * @return the handler, or {@code null} if this addon does not replace that id
     */
    public static Handler handlerFor(String id, Services services) {
        return switch (id) {
            case "queue" -> (player, view) -> services.queue().openMain(player);
            case "unranked_select_player_mode", "unranked_select_arena_mode" ->
                    (player, view) -> services.queue().openPlayerModes(player, false);
            case "ranked_select_player_mode", "ranked_select_arena_mode" ->
                    (player, view) -> services.queue().openPlayerModes(player, true);
            case "duel_player" -> (player, view) -> services.duel().openFromMenu(player, view);
            case "create_match" -> (player, view) -> services.duel().openTargetPicker(player);
            case "custom_select_arena_mode" -> (player, view) -> services.duel().openModeSelection(player);
            case "lost_items" -> (player, view) -> services.duel().openLostItems(player);
            case "party" -> (player, view) -> services.party().openMain(player);
            case "party_info", "party_manage_member" -> (player, view) -> services.party().openInfo(player, 1);
            case "party_invite_player" -> (player, view) -> services.party().openInvitePicker(player);
            case "party_ffa" -> (player, view) -> services.party().openFfa(player);
            case "party_teamfight" -> (player, view) -> services.party().openTeamFight(player);
            case "party_multiteam" -> (player, view) -> services.party().openMultiTeam(player);
            case "party_multiteam_spectators" -> (player, view) -> services.party().openSpectators(player);
            case "custom_challenge_opponent" -> (player, view) -> services.party().openChallengeOpponent(player, 1);
            case "settings" -> (player, view) -> services.settings().openSettings(player);
            case "stats" -> (player, view) -> services.stats().openMain(player);
            case "leaderboard" ->
                    (player, view) -> services.stats().openLeaderboard(player, StatsKind.UNRANKED, "wins", 1);
            case "ongoing_matches", "spectator_players" ->
                    (player, view) -> services.spectator().openMatches(player, 1);
            case "kit_preview" -> (player, view) -> services.kit().openKitList(player);
            case "confirmation_menu" -> (player, view) -> {
                if (!services.confirmation().open(player, view)) {
                    view.getViewer().openInventory(view.getInventory());
                }
            };
            default -> null;
        };
    }

    /**
     * @return the menu ids currently being served as forms, after the {@code menus.*} toggles
     */
    public Map<String, Handler> handlers() {
        return handlers;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!detector.isBedrockPlayer(player) || !gateway.isAvailable()) {
            return;
        }
        ContainerView view = phoenixView(event);
        if (view == null) {
            return;
        }
        ContainerLayout layout = layoutOf(view);
        String key = resolver.keyFor(layout);
        Handler handler = key == null ? null : handlers.get(key);
        if (handler == null) {
            if (config.debugEnabled()) {
                plugin.getLogger().info("Not intercepting PhoenixDuels menu for " + player.getName()
                        + ": layoutClass=" + PhoenixMenuResolver.describe(layout)
                        + " key=" + key + " title=" + event.getView().getTitle());
            }
            return;
        }
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> handler.open(player, view));
    }

    /**
     * @return the PhoenixDuels view behind this inventory, or {@code null} if the inventory is not
     *         one of theirs. Throwables are swallowed so a PhoenixDuels internal change degrades to
     *         "not ours" instead of breaking every inventory on the server.
     */
    private static ContainerView phoenixView(InventoryOpenEvent event) {
        try {
            return event.getInventory().getHolder() instanceof ContainerView view ? view : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ContainerLayout layoutOf(ContainerView view) {
        try {
            return view.getContainer() instanceof ContainerLayout layout ? layout : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
