package it.pintux.life.essentialsaddon.provider;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Warps;
import it.pintux.life.essentialsaddon.api.WarpProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * EssentialsX implementation of WarpProvider.
 */
public final class EssentialsXWarpProvider implements WarpProvider {

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
            return essentials.getWarps() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Collection<String> getWarpNames() {
        if (!isReady()) {
            return List.of();
        }
        Warps warps = essentials.getWarps();
        return new ArrayList<>(warps.getList());
    }

    @Override
    public Location getWarpLocation(String warpName) {
        if (!isReady()) {
            return null;
        }
        try {
            return essentials.getWarps().getWarp(warpName);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean hasAccess(Player player, String warpName) {
        if (!isReady()) {
            return false;
        }
        // EssentialsX warp permission: essentials.warps.<warpname> or essentials.warp.*
        if (player.hasPermission("essentials.warps.*") || player.hasPermission("essentials.warps." + warpName)) {
            return true;
        }
        // If no per-warp permission is required, allow by default
        return true;
    }

    @Override
    public boolean teleport(Player player, String warpName) {
        if (!isReady()) {
            return false;
        }
        try {
            Location location = getWarpLocation(warpName);
            if (location == null) {
                return false;
            }
            return player.teleport(location);
        } catch (Exception e) {
            return false;
        }
    }
}
