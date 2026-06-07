package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.PlayerStatsInfo;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Pure: renders the stats form content from a DTO + config. No Bukkit / no BedrockGUIApi. */
public final class StatsMenuModel {
    private final BedwarsAddonConfiguration config;

    public StatsMenuModel(BedwarsAddonConfiguration config) {
        this.config = config;
    }

    public String content(PlayerStatsInfo s) {
        Map<String, String> values = new HashMap<>();
        values.put("name", s.name() == null ? "" : s.name());
        values.put("wins", String.valueOf(s.wins()));
        values.put("losses", String.valueOf(s.losses()));
        values.put("kills", String.valueOf(s.kills()));
        values.put("final_kills", String.valueOf(s.finalKills()));
        values.put("deaths", String.valueOf(s.deaths()));
        values.put("final_deaths", String.valueOf(s.finalDeaths()));
        values.put("beds", String.valueOf(s.bedsDestroyed()));
        values.put("games", String.valueOf(s.gamesPlayed()));
        values.put("total_kills", String.valueOf(s.totalKills()));
        values.put("kd", ratio(s.kills(), s.deaths()));
        values.put("wl", ratio(s.wins(), s.losses()));
        return config.render(config.statsContent(), values);
    }

    /** num/den to 2 decimals; when den is 0, treat as num/1 (avoids divide-by-zero). */
    public static String ratio(int num, int den) {
        double r = den == 0 ? num : (double) num / den;
        return String.format(Locale.US, "%.2f", r);
    }
}
