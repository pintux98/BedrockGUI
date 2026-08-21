package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.HomeProvider;
import it.pintux.life.essentialsaddon.model.HomeView;
import it.pintux.life.essentialsaddon.model.HomeWriteResult;
import net.william278.huskhomes.HuskHomes;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.position.Home;
import net.william278.huskhomes.teleport.TeleportBuilder;
import net.william278.huskhomes.user.OnlineUser;
import net.william278.huskhomes.util.ValidationException;
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
    private static final String PUBLIC_HOME_PERMISSION = "huskhomes.command.phome";

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
    public void setHome(Player player, String homeName, Consumer<HomeWriteResult> callback) {
        HuskHomesAPI api = apiOrNull();
        if (api == null) {
            callback.accept(HomeWriteResult.failed("HuskHomes API unavailable"));
            return;
        }
        try {
            OnlineUser user = api.adaptUser(player);
            api.createHome(user, homeName, user.getPosition()).whenComplete((home, failure) ->
                    callback.accept(failure == null
                            ? HomeWriteResult.ok()
                            : refusal(player, user, homeName, failure)));
        } catch (Throwable failure) {
            callback.accept(refusal(player, api.adaptUser(player), homeName, failure));
        }
    }

    /**
     * A refused write is the player's own input more often than a fault: an invalid name, a name
     * already taken, no slots left. HuskHomes words those better than this addon can, so it is
     * asked to tell the player, and only a genuine fault is logged.
     */
    private HomeWriteResult refusal(Player player, OnlineUser user, String homeName, Throwable failure) {
        Throwable cause = failure instanceof java.util.concurrent.CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;
        if (cause instanceof ValidationException validation) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
            if (plugin instanceof HuskHomes huskHomes) {
                try {
                    validation.dispatchHomeError(user, false, huskHomes, homeName);
                    return HomeWriteResult.reportedToPlayer(validation.getType().name());
                } catch (Throwable ignored) {
                    // Fall through to this addon's own message.
                }
            }
            return HomeWriteResult.failed(validation.getType().name());
        }
        report("Could not set home '" + homeName + "' for " + player.getName(), cause);
        return HomeWriteResult.failed(cause.getClass().getSimpleName());
    }

    @Override
    public void deleteHome(Player player, String homeName, Consumer<Boolean> callback) {
        HuskHomesAPI api = apiOrNull();
        if (api == null) {
            callback.accept(false);
            return;
        }
        HuskHomes huskHomes = huskHomesOrNull();
        if (huskHomes == null) {
            callback.accept(false);
            return;
        }
        OnlineUser user = api.adaptUser(player);
        // Same reason as setHomePrivacy: the API's own method swallows the refusal in an async task.
        huskHomes.runAsync(() -> {
            try {
                huskHomes.getManager().homes().deleteHome(user, homeName);
                callback.accept(true);
            } catch (ValidationException validation) {
                callback.accept(false);
            } catch (Throwable failure) {
                report("Could not delete home '" + homeName + "' for " + player.getName(), failure);
                callback.accept(false);
            }
        });
    }

    @Override
    public void homeDetails(Player player, Consumer<List<HomeView>> callback) {
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
                List<HomeView> views = new ArrayList<>();
                for (Home home : homes) {
                    views.add(new HomeView(home.getName(), home.isPublic()));
                }
                callback.accept(views);
            });
        } catch (Throwable failure) {
            report("Could not read homes for " + player.getName(), failure);
            callback.accept(List.of());
        }
    }

    @Override
    public void setHomePrivacy(Player player, String homeName, boolean isPublic,
                               Consumer<HomeWriteResult> callback) {
        HuskHomesAPI api = apiOrNull();
        HuskHomes huskHomes = huskHomesOrNull();
        if (api == null || huskHomes == null) {
            callback.accept(HomeWriteResult.failed("HuskHomes API unavailable"));
            return;
        }
        OnlineUser user = api.adaptUser(player);
        // HuskHomesAPI#setHomePrivacy hands the work to its own async task and returns void, so a
        // refusal such as REACHED_MAX_PUBLIC_HOMES would be lost and reported here as success.
        // Calling the manager on that same scheduler keeps the write off the server thread while
        // making the outcome visible.
        huskHomes.runAsync(() -> {
            try {
                huskHomes.getManager().homes().setHomePrivacy(user, homeName, isPublic);
                callback.accept(HomeWriteResult.ok());
            } catch (ValidationException validation) {
                try {
                    validation.dispatchHomeError(user, isPublic, huskHomes, homeName);
                    callback.accept(HomeWriteResult.reportedToPlayer(validation.getType().name()));
                } catch (Throwable ignored) {
                    callback.accept(HomeWriteResult.failed(validation.getType().name()));
                }
            } catch (Throwable failure) {
                report("Could not change the privacy of home '" + homeName + "' for " + player.getName(), failure);
                callback.accept(HomeWriteResult.failed(failure.getClass().getSimpleName()));
            }
        });
    }

    private HuskHomes huskHomesOrNull() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("HuskHomes");
        return plugin instanceof HuskHomes huskHomes && plugin.isEnabled() ? huskHomes : null;
    }

    @Override
    public boolean supportsPublicHomes() {
        return true;
    }

    @Override
    public void publicHomes(Player player, Consumer<List<String>> callback) {
        HuskHomesAPI api = apiOrNull();
        if (api == null || !player.hasPermission(PUBLIC_HOME_PERMISSION)) {
            callback.accept(List.of());
            return;
        }
        try {
            api.getPublicHomes().whenComplete((homes, failure) -> {
                if (failure != null || homes == null) {
                    report("Could not read the public home list", failure);
                    callback.accept(List.of());
                    return;
                }
                List<String> identifiers = new ArrayList<>();
                for (Home home : homes) {
                    identifiers.add(home.getIdentifier());
                }
                callback.accept(identifiers);
            });
        } catch (Throwable failure) {
            report("Could not read the public home list", failure);
            callback.accept(List.of());
        }
    }

    @Override
    public void teleportPublicHome(Player player, String identifier, Consumer<Boolean> callback) {
        HuskHomesAPI api = apiOrNull();
        if (api == null || !player.hasPermission(PUBLIC_HOME_PERMISSION)) {
            callback.accept(false);
            return;
        }
        try {
            OnlineUser user = api.adaptUser(player);
            // A public home is addressed as owner.name, and only its owner's list can resolve it,
            // so the public list is the lookup.
            api.getPublicHomes().whenComplete((homes, failure) -> {
                if (failure != null || homes == null) {
                    report("Could not read the public home list", failure);
                    callback.accept(false);
                    return;
                }
                for (Home home : homes) {
                    if (home.getIdentifier().equalsIgnoreCase(identifier)) {
                        callback.accept(teleport(api, user, home, player, identifier));
                        return;
                    }
                }
                callback.accept(false);
            });
        } catch (Throwable failure) {
            report("Could not teleport " + player.getName() + " to public home '" + identifier + "'", failure);
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
