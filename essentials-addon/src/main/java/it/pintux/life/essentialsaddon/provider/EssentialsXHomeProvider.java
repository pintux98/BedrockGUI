package it.pintux.life.essentialsaddon.provider;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import it.pintux.life.essentialsaddon.api.HomeProvider;
import it.pintux.life.essentialsaddon.util.MainThread;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class EssentialsXHomeProvider implements HomeProvider {

    private Essentials essentials;

    @Override
    public String getProviderId() {
        return "essentialsx";
    }

    @Override
    public boolean isReady() {
        if (essentials != null && essentials.isEnabled()) {
            return true;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            essentials = (Essentials) plugin;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void homeNames(Player player, Consumer<List<String>> callback) {
        User user = user(player);
        if (user == null) {
            callback.accept(List.of());
            return;
        }
        try {
            callback.accept(new ArrayList<>(user.getHomes()));
        } catch (Exception e) {
            callback.accept(List.of());
        }
    }

    @Override
    public void homeLimit(Player player, IntConsumer callback) {
        User user = user(player);
        if (user == null) {
            callback.accept(0);
            return;
        }
        try {
            callback.accept(essentials.getSettings().getHomeLimit(user));
        } catch (Exception e) {
            callback.accept(0);
        }
    }

    @Override
    public void teleportHome(Player player, String homeName, Consumer<Boolean> callback) {
        User user = user(player);
        if (user == null) {
            callback.accept(false);
            return;
        }
        // EssentialsX teleports on the server thread.
        MainThread.run(() -> {
            try {
                Location home = user.getHome(homeName);
                callback.accept(home != null && player.teleport(home));
            } catch (Exception e) {
                callback.accept(false);
            }
        });
    }

    @Override
    public void setHome(Player player, String homeName, Consumer<Boolean> callback) {
        User user = user(player);
        if (user == null) {
            callback.accept(false);
            return;
        }
        MainThread.run(() -> {
            try {
                user.setHome(homeName, player.getLocation());
                callback.accept(true);
            } catch (Exception e) {
                callback.accept(false);
            }
        });
    }

    @Override
    public void deleteHome(Player player, String homeName, Consumer<Boolean> callback) {
        User user = user(player);
        if (user == null) {
            callback.accept(false);
            return;
        }
        MainThread.run(() -> {
            try {
                user.delHome(homeName);
                callback.accept(true);
            } catch (Exception e) {
                callback.accept(false);
            }
        });
    }

    private User user(Player player) {
        if (!isReady()) {
            return null;
        }
        try {
            return essentials.getUser(player);
        } catch (Exception e) {
            return null;
        }
    }
}
