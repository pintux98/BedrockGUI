package it.pintux.life.essentialsaddon.provider;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.Modules.Homes.CmiHome;
import it.pintux.life.essentialsaddon.api.HomeProvider;
import it.pintux.life.essentialsaddon.util.MainThread;
import net.Zrips.CMILib.Container.CMILocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class CMIHomeProvider implements HomeProvider {

    @Override
    public String getProviderId() {
        return "cmi";
    }

    @Override
    public boolean isReady() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CMI");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            return CMI.getInstance() != null && CMI.getInstance().getPlayerManager() != null;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public void homeNames(Player player, Consumer<List<String>> callback) {
        CMIUser user = user(player);
        if (user == null) {
            callback.accept(List.of());
            return;
        }
        try {
            callback.accept(new ArrayList<>(user.getHomes().keySet()));
        } catch (Throwable e) {
            callback.accept(List.of());
        }
    }

    @Override
    public void homeLimit(Player player, IntConsumer callback) {
        if (!isReady()) {
            callback.accept(0);
            return;
        }
        try {
            callback.accept(CMI.getInstance().getHomeManager().getMaxHomes(player));
        } catch (Throwable e) {
            callback.accept(0);
        }
    }

    @Override
    public void teleportHome(Player player, String homeName, Consumer<Boolean> callback) {
        CMIUser user = user(player);
        if (user == null) {
            callback.accept(false);
            return;
        }
        // CMI teleports on the server thread.
        MainThread.run(() -> {
            try {
                CmiHome home = user.getHome(homeName);
                Location target = home == null ? null : bukkitLocation(home);
                callback.accept(target != null && player.teleport(target));
            } catch (Throwable e) {
                callback.accept(false);
            }
        });
    }

    @Override
    public void setHome(Player player, String homeName, Consumer<Boolean> callback) {
        CMIUser user = user(player);
        if (user == null) {
            callback.accept(false);
            return;
        }
        MainThread.run(() -> {
            try {
                user.addHome(new CmiHome(homeName, new CMILocation(player.getLocation())), true);
                callback.accept(true);
            } catch (Throwable e) {
                callback.accept(false);
            }
        });
    }

    @Override
    public void deleteHome(Player player, String homeName, Consumer<Boolean> callback) {
        CMIUser user = user(player);
        if (user == null) {
            callback.accept(false);
            return;
        }
        MainThread.run(() -> {
            try {
                user.removeHome(homeName);
                callback.accept(true);
            } catch (Throwable e) {
                callback.accept(false);
            }
        });
    }

    private Location bukkitLocation(CmiHome home) {
        CMILocation location = home.getLoc();
        return location == null ? null : location.getBukkitLoc();
    }

    private CMIUser user(Player player) {
        if (!isReady()) {
            return null;
        }
        try {
            return CMI.getInstance().getPlayerManager().getUser(player);
        } catch (Throwable e) {
            return null;
        }
    }
}
