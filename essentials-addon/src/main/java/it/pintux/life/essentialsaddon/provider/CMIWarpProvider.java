package it.pintux.life.essentialsaddon.provider;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Warps.CmiWarp;
import it.pintux.life.essentialsaddon.api.WarpProvider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class CMIWarpProvider implements WarpProvider {

    @Override
    public String getProviderId() {
        return "cmi";
    }

    @Override
    public boolean isReady() {
        return Bukkit.getPluginManager().isPluginEnabled("CMI")
                && CMI.getInstance() != null
                && CMI.getInstance().getWarpManager() != null;
    }

    @Override
    public Collection<String> getWarpNames() {
        if (!isReady()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (CmiWarp warp : CMI.getInstance().getWarpManager().getWarps().values()) {
            if (warp != null && !warp.isHidden()) {
                names.add(warp.getName());
            }
        }
        return names;
    }

    @Override
    public Location getWarpLocation(String warpName) {
        CmiWarp warp = warp(warpName);
        return warp == null ? null : warp.getLoc();
    }

    @Override
    public boolean hasAccess(Player player, String warpName) {
        CmiWarp warp = warp(warpName);
        if (warp == null) {
            return false;
        }
        return !warp.isReqPerm()
                || player.hasPermission("cmi.command.warp.*")
                || player.hasPermission("cmi.command.warp." + warp.getName());
    }

    @Override
    public boolean teleport(Player player, String warpName) {
        Location location = getWarpLocation(warpName);
        return location != null && player.teleport(location);
    }

    @Override
    public String getDisplayName(String warpName) {
        CmiWarp warp = warp(warpName);
        if (warp == null || warp.getShownName() == null || warp.getShownName().isBlank()) {
            return warpName;
        }
        return warp.getShownName();
    }

    private CmiWarp warp(String warpName) {
        if (warpName == null || !isReady()) {
            return null;
        }
        Map<String, CmiWarp> warps = CMI.getInstance().getWarpManager().getWarps();
        CmiWarp exact = CMI.getInstance().getWarpManager().getWarp(warpName);
        if (exact != null) {
            return exact;
        }
        for (CmiWarp warp : warps.values()) {
            if (warp != null && warp.getName().equalsIgnoreCase(warpName)) {
                return warp;
            }
        }
        return null;
    }
}
