package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.WarpProvider;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.event.WarpCreateEvent;
import net.william278.huskhomes.event.WarpDeleteEvent;
import net.william278.huskhomes.event.WarpEditEvent;
import net.william278.huskhomes.position.Warp;
import net.william278.huskhomes.teleport.TeleportBuilder;
import net.william278.huskhomes.user.OnlineUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Warps served by HuskHomes through its published API.
 *
 * <p>HuskHomes answers {@code getWarps()} with a future it completes on the server thread, so the
 * list is loaded in the background and served from a cache. HuskHomes' own warp events refresh
 * that cache, so a warp created or removed in game shows up without a reload.</p>
 */
public final class HuskHomesWarpProvider implements WarpProvider {
    private final Logger logger;
    private final Runnable onWarpsChanged;
    private final AtomicBoolean loading = new AtomicBoolean();

    private static final long RETRY_BACKOFF_MILLIS = 30_000L;

    private volatile Map<String, Warp> warps = Map.of();
    private volatile boolean loaded;
    private volatile long retryAfter;
    private volatile String lastFailure;

    public HuskHomesWarpProvider(Plugin plugin, Logger logger, Runnable onWarpsChanged) {
        this.logger = logger;
        this.onWarpsChanged = onWarpsChanged;
        if (Bukkit.getPluginManager().getPlugin("HuskHomes") == null) {
            throw new IllegalStateException("HuskHomes not found");
        }
        hookWarpEvents(plugin);
        load();
    }

    @Override
    public String getProviderId() {
        return "HuskHomes";
    }

    @Override
    public boolean isReady() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
        if (plugin == null || !plugin.isEnabled() || apiOrNull() == null) {
            return false;
        }
        if (!loaded) {
            load();
        }
        return true;
    }

    @Override
    public Collection<String> getWarpNames() {
        if (!loaded) {
            load();
        }
        return new ArrayList<>(warps.keySet());
    }

    @Override
    public Location getWarpLocation(String warpName) {
        HuskHomesAPI api = apiOrNull();
        Warp warp = warp(warpName);
        if (api == null || warp == null) {
            return null;
        }
        try {
            return api.getLocation(warp);
        } catch (Throwable failure) {
            report("Could not read the location of warp '" + warpName + "'", failure);
            return null;
        }
    }

    @Override
    public boolean hasAccess(Player player, String warpName) {
        HuskHomesAPI api = apiOrNull();
        Warp warp = warp(warpName);
        if (api == null || warp == null) {
            return false;
        }
        try {
            // Answers with HuskHomes' own rule, so a server that does not restrict warps by
            // permission keeps showing all of them.
            return warp.hasPermission(api.adaptUser(player));
        } catch (Throwable failure) {
            report("Could not check access to warp '" + warpName + "' for " + player.getName(), failure);
            return false;
        }
    }

    @Override
    public boolean teleport(Player player, String warpName) {
        HuskHomesAPI api = apiOrNull();
        Warp warp = warp(warpName);
        if (api == null || warp == null) {
            return false;
        }
        try {
            OnlineUser user = api.adaptUser(player);
            TeleportBuilder builder = api.teleportBuilder(user).teleporter(user).target(warp);
            try {
                // Honours the server's warmup, the same as HuskHomes' own /warp.
                builder.toTimedTeleport().execute();
            } catch (IllegalStateException noWarmup) {
                builder.toTeleport().execute();
            }
            return true;
        } catch (Throwable failure) {
            report("Could not teleport " + player.getName() + " to warp '" + warpName + "'", failure);
            return false;
        }
    }

    @Override
    public String getDisplayName(String warpName) {
        Warp warp = warp(warpName);
        return warp == null ? warpName : warp.getName();
    }

    private Warp warp(String warpName) {
        if (warpName == null) {
            return null;
        }
        Map<String, Warp> snapshot = warps;
        Warp exact = snapshot.get(warpName);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Warp> entry : snapshot.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(warpName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void load() {
        HuskHomesAPI api = apiOrNull();
        if (api == null || System.currentTimeMillis() < retryAfter || !loading.compareAndSet(false, true)) {
            return;
        }
        try {
            api.getWarps().whenComplete((list, failure) -> {
                boolean reloaded = false;
                try {
                    if (failure != null || list == null) {
                        // Backed off, because the refresh below asks for this list again: retrying
                        // straight away would turn one failure into a loop.
                        retryAfter = System.currentTimeMillis() + RETRY_BACKOFF_MILLIS;
                        report("Could not read the warp list", failure);
                        return;
                    }
                    Map<String, Warp> loadedWarps = new LinkedHashMap<>();
                    for (Warp warp : list) {
                        loadedWarps.put(warp.getName(), warp);
                    }
                    warps = Map.copyOf(loadedWarps);
                    loaded = true;
                    retryAfter = 0L;
                    reloaded = true;
                } finally {
                    loading.set(false);
                }
                if (reloaded && onWarpsChanged != null) {
                    onWarpsChanged.run();
                }
            });
        } catch (Throwable failure) {
            loading.set(false);
            retryAfter = System.currentTimeMillis() + RETRY_BACKOFF_MILLIS;
            report("Could not read the warp list", failure);
        }
    }

    private void hookWarpEvents(Plugin plugin) {
        try {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                public void onCreate(WarpCreateEvent event) {
                    reload();
                }

                @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                public void onDelete(WarpDeleteEvent event) {
                    reload();
                }

                @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                public void onEdit(WarpEditEvent event) {
                    reload();
                }
            }, plugin);
        } catch (Exception | LinkageError failure) {
            // A HuskHomes build without these events just means the list refreshes on reload.
            report("This HuskHomes build exposes no warp events, so the warp list refreshes only on reload", failure);
        }
    }

    private void reload() {
        loaded = false;
        retryAfter = 0L;
        load();
    }

    private HuskHomesAPI apiOrNull() {
        try {
            return HuskHomesAPI.getInstance();
        } catch (Throwable notRegistered) {
            return null;
        }
    }

    /** Logs each distinct failure once, so a broken call is visible without spamming console. */
    private void report(String message, Throwable failure) {
        String detail = failure == null ? message
                : message + ": " + failure.getClass().getSimpleName()
                        + (failure.getMessage() == null ? "" : " - " + failure.getMessage());
        String key = detail.toLowerCase(Locale.ROOT);
        if (key.equals(lastFailure)) {
            return;
        }
        lastFailure = key;
        if (failure == null) {
            logger.warning("HuskHomes warp provider: " + detail);
        } else {
            logger.log(Level.WARNING, "HuskHomes warp provider: " + detail, failure);
        }
    }
}
