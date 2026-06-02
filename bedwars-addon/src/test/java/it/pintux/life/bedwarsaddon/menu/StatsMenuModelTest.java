package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.PlayerStatsInfo;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsMenuModelTest {

    private BedwarsAddonConfiguration config() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("stats.content", "W{wins} L{losses} K{kills} D{deaths} KD{kd} WL{wl}");
        Constructor<BedwarsAddonConfiguration> ctor =
                BedwarsAddonConfiguration.class.getDeclaredConstructor(YamlConfiguration.class);
        ctor.setAccessible(true);
        return ctor.newInstance(yaml);
    }

    @Test
    void ratioHandlesZeroDenominator() {
        assertEquals("5.00", StatsMenuModel.ratio(10, 2));
        assertEquals("7.00", StatsMenuModel.ratio(7, 0));
        assertEquals("0.50", StatsMenuModel.ratio(1, 2));
    }

    @Test
    void contentSubstitutesStatsAndRatios() throws Exception {
        StatsMenuModel model = new StatsMenuModel(config());
        String out = model.content(new PlayerStatsInfo("Steve", 10, 5, 20, 3, 4, 1, 8, 15, 23));
        assertEquals("W10 L5 K20 D4 KD5.00 WL2.00", out);
    }
}
