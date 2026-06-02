package it.pintux.life.bedwarsaddon.provider;

import com.tomkeuper.bedwars.api.BedWars;
import com.tomkeuper.bedwars.api.stats.IPlayerStats;
import com.tomkeuper.bedwars.api.stats.IStatsManager;
import it.pintux.life.bedwarsaddon.api.StatsProvider;
import it.pintux.life.bedwarsaddon.model.PlayerStatsInfo;
import org.bukkit.entity.Player;

/** BedWars2023 implementation of {@link StatsProvider}. The ONLY stats class touching com.tomkeuper.*. */
public final class BedWars2023StatsProvider implements StatsProvider {
    private final BedWarsApiAccess access;

    public BedWars2023StatsProvider(BedWarsApiAccess access) {
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars2023"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    @Override
    public PlayerStatsInfo getStats(Player player) {
        BedWars api = access.get();
        if (api == null) return null;
        IStatsManager manager = api.getStatsManager();
        if (manager == null) return null;
        IPlayerStats s = manager.get(player.getUniqueId());
        if (s == null) s = manager.getUnsafe(player.getUniqueId());
        if (s == null) return null;
        return new PlayerStatsInfo(
                s.getName(),
                s.getWins(),
                s.getLosses(),
                s.getKills(),
                s.getFinalKills(),
                s.getDeaths(),
                s.getFinalDeaths(),
                s.getBedsDestroyed(),
                s.getGamesPlayed(),
                s.getTotalKills());
    }
}
