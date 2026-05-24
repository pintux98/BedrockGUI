package it.pintux.life.essentialsaddon.provider;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import it.pintux.life.essentialsaddon.api.HomeProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

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
            return essentials.getUser(Bukkit.getOnlinePlayers().iterator().next()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> getHomeNames(Player player) {
        if (!isReady()) return List.of();
        try {
            User user = essentials.getUser(player);
            if (user == null) return List.of();
            return new ArrayList<>(user.getHomes());
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public Location getHomeLocation(Player player, String homeName) {
        if (!isReady()) return null;
        try {
            User user = essentials.getUser(player);
            if (user == null) return null;
            return user.getHome(homeName);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean teleportHome(Player player, String homeName) {
        if (!isReady()) return false;
        try {
            Location loc = getHomeLocation(player, homeName);
            if (loc == null) return false;
            return player.teleport(loc);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean setHome(Player player, String homeName) {
        if (!isReady()) return false;
        try {
            User user = essentials.getUser(player);
            if (user == null) return false;
            user.setHome(homeName, player.getLocation());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean deleteHome(Player player, String homeName) {
        if (!isReady()) return false;
        try {
            User user = essentials.getUser(player);
            if (user == null) return false;
            user.delHome(homeName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getMaxHomes(Player player) {
        if (!isReady()) return 0;
        try {
            User user = essentials.getUser(player);
            if (user == null) return 0;
            return essentials.getSettings().getHomeLimit(user);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getHomeCount(Player player) {
        return getHomeNames(player).size();
    }
}
