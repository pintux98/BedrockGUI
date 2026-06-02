package it.pintux.life.bedwarsaddon.provider;

import it.pintux.life.bedwarsaddon.api.StatsProvider;
import it.pintux.life.bedwarsaddon.model.PlayerStatsInfo;
import org.bukkit.entity.Player;
import org.screamingsandals.bedwars.api.BedwarsAPI;
import org.screamingsandals.bedwars.api.statistics.PlayerStatistic;
import org.screamingsandals.bedwars.api.statistics.PlayerStatisticsManager;

import java.util.UUID;

/**
 * ScreamingBedWars stats provider. SBW tracks kills/deaths/wins/loses/beds/games but has no
 * final-kills/final-deaths/total-kills, which are reported as 0.
 */
public final class SbwStatsProvider implements StatsProvider {
    private final SbwApiAccess access;

    public SbwStatsProvider(SbwApiAccess access) {
        this.access = access;
    }

    @Override public String getProviderId() { return "ScreamingBedWars"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    @Override
    public PlayerStatsInfo getStats(Player player) {
        BedwarsAPI api = access.get();
        if (api == null) return null;
        PlayerStatisticsManager manager = api.getStatisticsManager();
        if (manager == null) return null;
        UUID id = player.getUniqueId();
        PlayerStatistic s = manager.getStatistic(id);
        if (s == null) s = manager.loadStatistic(id);
        if (s == null) return null;
        return new PlayerStatsInfo(
                player.getName(),
                s.getWins(),
                s.getLoses(),
                s.getKills(),
                0,               // SBW has no final kills
                s.getDeaths(),
                0,               // SBW has no final deaths
                s.getDestroyedBeds(),
                s.getGames(),
                0);              // SBW has no total kills
    }
}
