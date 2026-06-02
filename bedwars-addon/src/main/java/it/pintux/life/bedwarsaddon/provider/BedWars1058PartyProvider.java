package it.pintux.life.bedwarsaddon.provider;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.party.Party;
import it.pintux.life.bedwarsaddon.api.PartyProvider;
import it.pintux.life.bedwarsaddon.model.PartyInfo;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** BedWars1058 implementation of {@link PartyProvider}. The ONLY party class touching com.andrei1058.*. */
public final class BedWars1058PartyProvider implements PartyProvider {
    private final BedWars1058ApiAccess access;

    public BedWars1058PartyProvider(BedWars1058ApiAccess access) {
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars1058"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    private Party party() {
        BedWars api = access.get();
        return api == null ? null : api.getPartyUtil();
    }

    @Override
    public PartyInfo getParty(Player player) {
        Party party = party();
        if (party == null || !party.hasParty(player)) return PartyInfo.none();
        List<String> names = new ArrayList<>();
        for (Player m : party.getMembers(player)) {
            if (m != null) names.add(m.getName());
        }
        Player owner = party.getOwner(player);
        return new PartyInfo(true, party.isOwner(player), owner != null ? owner.getName() : "", names);
    }

    @Override
    public boolean add(Player requester, String targetName) {
        Party party = party();
        if (party == null) return false;
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) return false;
        if (!party.hasParty(requester)) {
            party.createParty(requester, target);
        } else {
            party.addMember(requester, target);
        }
        return true;
    }

    @Override
    public void leave(Player player) {
        Party party = party();
        if (party != null && party.hasParty(player)) {
            party.removeFromParty(player);
        }
    }

    @Override
    public void disband(Player player) {
        Party party = party();
        if (party != null && party.hasParty(player) && party.isOwner(player)) {
            party.disband(player);
        }
    }

    @Override
    public boolean kick(Player owner, String targetName) {
        Party party = party();
        if (party == null) return false;
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) return false;
        party.removePlayer(owner, target);
        return true;
    }
}
