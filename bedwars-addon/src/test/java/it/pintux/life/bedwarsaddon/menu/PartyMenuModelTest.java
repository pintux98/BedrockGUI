package it.pintux.life.bedwarsaddon.menu;

import it.pintux.life.bedwarsaddon.config.BedwarsAddonConfiguration;
import it.pintux.life.bedwarsaddon.model.PartyInfo;
import it.pintux.life.bedwarsaddon.util.BedwarsActionPayloads;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartyMenuModelTest {

    private BedwarsAddonConfiguration config() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("party.add-button", "Add");
        yaml.set("party.leave-button", "Leave");
        yaml.set("party.disband-button", "Disband");
        yaml.set("party.kick-button", "Kick");
        yaml.set("party.kick-entry-button", "K:{player}");
        yaml.set("party.back-button", "Back");
        yaml.set("party.close-button", "Close");
        Constructor<BedwarsAddonConfiguration> ctor =
                BedwarsAddonConfiguration.class.getDeclaredConstructor(YamlConfiguration.class);
        ctor.setAccessible(true);
        return ctor.newInstance(yaml);
    }

    @Test
    void ownerSeesAllActions() throws Exception {
        PartyMenuModel model = new PartyMenuModel(config());
        List<MenuButton> b = model.mainButtons(new PartyInfo(true, true, "Steve", List.of("Steve", "Alex")));
        // Add, Leave, Disband, Kick, Close
        assertEquals(5, b.size());
        assertEquals("Add", b.get(0).label());
        assertEquals("Disband", b.get(2).label());
        assertEquals("Kick", b.get(3).label());
        assertEquals("bw_party_close:", b.get(4).actionString());
    }

    @Test
    void memberSeesAddLeaveCloseOnly() throws Exception {
        PartyMenuModel model = new PartyMenuModel(config());
        List<MenuButton> b = model.mainButtons(new PartyInfo(true, false, "Steve", List.of("Steve", "Alex")));
        assertEquals(3, b.size()); // Add, Leave, Close
        assertEquals("Leave", b.get(1).label());
    }

    @Test
    void noPartyShowsAddAndCloseOnly() throws Exception {
        PartyMenuModel model = new PartyMenuModel(config());
        List<MenuButton> b = model.mainButtons(PartyInfo.none());
        assertEquals(2, b.size()); // Add, Close
    }

    @Test
    void kickButtonsPerMemberThenBack() throws Exception {
        PartyMenuModel model = new PartyMenuModel(config());
        List<MenuButton> b = model.kickButtons(List.of("Alex"));
        assertEquals(2, b.size());
        assertEquals("K:Alex", b.get(0).label());
        assertEquals("bw_party_kickdo:" + BedwarsActionPayloads.encode("Alex"), b.get(0).actionString());
        assertEquals("bw_party_main:", b.get(1).actionString());
    }
}
