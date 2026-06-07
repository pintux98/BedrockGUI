package it.pintux.life.bedwarsaddon.provider;

import com.andrei1058.bedwars.api.BedWars;
import it.pintux.life.bedwarsaddon.api.StatsProvider;
import it.pintux.life.bedwarsaddon.model.PlayerStatsInfo;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * BedWars1058 implementation of {@link StatsProvider}. The ONLY 1058 stats class touching com.andrei1058.*.
 * <p>
 * BedWars1058 exposes persistent stats via {@code BedWars.IStats} (by UUID) rather than the
 * IStatsManager/IPlayerStats objects used by BedWars2023.
 */
public final class BedWars1058StatsProvider implements StatsProvider {
    private final BedWars1058ApiAccess access;

    public BedWars1058StatsProvider(BedWars1058ApiAccess access) {
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars1058"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    @Override
    public PlayerStatsInfo getStats(Player player) {
        BedWars api = access.get();
        if (api == null) return null;
        BedWars.IStats stats = api.getStatsUtil();
        if (stats == null) return null;
        UUID id = player.getUniqueId();
        return new PlayerStatsInfo(
                player.getName(),
                stats.getPlayerWins(id),
                stats.getPlayerLoses(id),
                stats.getPlayerKills(id),
                stats.getPlayerFinalKills(id),
                stats.getPlayerDeaths(id),
                stats.getPlayerFinalDeaths(id),
                stats.getPlayerBedsDestroyed(id),
                stats.getPlayerGamesPlayed(id),
                stats.getPlayerTotalKills(id));
    }
}
