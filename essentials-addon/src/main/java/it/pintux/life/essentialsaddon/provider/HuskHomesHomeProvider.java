package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.HomeProvider;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.position.Home;
import net.william278.huskhomes.teleport.TeleportBuilder;
import net.william278.huskhomes.user.OnlineUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Homes served by HuskHomes through its published API.
 *
 * <p>Nothing here waits for a result. HuskHomes completes the futures it returns on the server
 * thread, so blocking that thread for an answer deadlocks until the timeout expires — which is
 * exactly why the home menu used to come up empty.</p>
 */
public final class HuskHomesHomeProvider implements HomeProvider {
    private final Logger logger;
    private volatile String lastFailure;

    public HuskHomesHomeProvider(Logger logger) {
        this.logger = logger;
        if (Bukkit.getPluginManager().getPlugin("HuskHomes") == null) {
            throw new IllegalStateException("HuskHomes not found");
        }
    }

    @Override
    public String getProviderId() {
        return "HuskHomes";
    }

    @Override
    public boolean isReady() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            HuskHomesAPI.getInstance();
            return true;
        } catch (Throwable notReady) {
            return false;
        }
    }

    @Override
    public void homeNames(Player player, Consumer<List<String>> callback) {
        HuskHomesAPI api = apiOrNull();
        if (api == null) {
            callback.accept(List.of());
            return;
        }
        try {
            api.getUserHomes(api.adaptUser(player)).whenComplete((homes, failure) -> {
                if (failure != null || homes == null) {
                    report("Could not read homes for " + player.getName(), failure);
                    callback.accept(List.of());
                    return;
                }
                List<String> names = new ArrayList<>();
                for (Home home : homes) {
                    names.add(home.getName());
                }
                callback.accept(names);
            });
        } catch (Throwable failure) {
            report("Could not read homes for " + player.getName(), failure);
            callback.accept(List.of());
        }
    }

    @Override
    public void homeLimit(Player player, IntConsumer callback) {
        HuskHomesAPI api = apiOrNull();
        if (api == null) {
            callback.accept(0);
            return;
        }
        try {
            callback.accept(api.getMaxHomeSlots(api.adaptUser(player)));
        } catch (Throwable failure) {
            report("Could not read the home limit for " + player.getName(), failure);
            callback.accept(0);
        }
    }

    @Override
    public void teleportHome(Player player, String homeName, Consumer<Boolean> callback) {
        HuskHomesAPI api = apiOrNull();
        if (api == null) {
            callback.accept(false);
            return;
        }
        try {
            OnlineUser user = api.adaptUser(player);
            api.getHome(user, homeName).whenComplete((home, failure) -> {
                if (failure != null) {
                    report("Could not look up home '" + homeName + "' for " + player.getName(), failure);
                    callback.accept(false);
                    return;
                }
                if (home == null || home.isEmpty()) {
                    callback.accept(false);
                    return;
                }
                callback.accept(teleport(api, user, home.get(), player, homeName));
            });
        } catch (Throwable failure) {
            report("Could not teleport " + player.getName() + " to home '" + homeName + "'", failure);
            callback.accept(false);
        }
    }

    @Override
    public void setHome(Player player, String homeName, Consumer<Boolean> callback) {
        HuskHomesAPI api = apiOrNull();
        if (api == null) {
            callback.accept(false);
            return;
        }
        try {
            OnlineUser user = api.adaptUser(player);
            api.createHome(user, homeName, user.getPosition()).whenComplete((home, failure) -> {
                if (failure != null) {
                    // HuskHomes tells the player why itself (name taken, slots used up, bad name).
                    report("Could not set home '" + homeName + "' for " + player.getName(), failure);
                    callback.accept(false);
                    return;
                }
                callback.accept(true);
            });
        } catch (Throwable failure) {
            report("Could not set home '" + homeName + "' for " + player.getName(), failure);
            callback.accept(false);
        }
    }

    @Override
    public void deleteHome(Player player, String homeName, Consumer<Boolean> callback) {
        HuskHomesAPI api = apiOrNull();
        if (api == null) {
            callback.accept(false);
            return;
        }
        try {
            api.deleteHome(api.adaptUser(player), homeName);
            callback.accept(true);
        } catch (Throwable failure) {
            report("Could not delete home '" + homeName + "' for " + player.getName(), failure);
            callback.accept(false);
        }
    }

    private boolean teleport(HuskHomesAPI api, OnlineUser user, Home home, Player player, String homeName) {
        try {
            TeleportBuilder builder = api.teleportBuilder(user).teleporter(user).target(home);
            try {
                // Honours the server's warmup, the same as HuskHomes' own /home.
                builder.toTimedTeleport().execute();
            } catch (IllegalStateException noWarmup) {
                builder.toTeleport().execute();
            }
            return true;
        } catch (Throwable failure) {
            report("Could not teleport " + player.getName() + " to home '" + homeName + "'", failure);
            return false;
        }
    }

    private HuskHomesAPI apiOrNull() {
        try {
            return HuskHomesAPI.getInstance();
        } catch (Throwable notRegistered) {
            report("HuskHomes' API is not registered, so no home operation can run", notRegistered);
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
            logger.warning("HuskHomes home provider: " + detail);
        } else {
            logger.log(Level.WARNING, "HuskHomes home provider: " + detail, failure);
        }
    }
}
