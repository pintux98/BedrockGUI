package it.pintux.life.bedwarsaddon.provider;

import com.tomkeuper.bedwars.api.BedWars;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Resolves and caches the BedWars2023 API service. Null-safe. */
public final class BedWarsApiAccess {
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
