package it.pintux.life.bedwarsaddon.provider;

import com.andrei1058.bedwars.api.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Resolves and caches the BedWars1058 (andrei1058) API service. Null-safe. */
public final class BedWars1058ApiAccess {
    private BedWars api;

    public BedWars get() {
        if (api != null) return api;
        RegisteredServiceProvider<BedWars> rsp =
                Bukkit.getServicesManager().getRegistration(BedWars.class);
        if (rsp != null) {
            api = rsp.getProvider();
        }
        return api;
    }

    public boolean isAvailable() {
        return get() != null;
    }
}
