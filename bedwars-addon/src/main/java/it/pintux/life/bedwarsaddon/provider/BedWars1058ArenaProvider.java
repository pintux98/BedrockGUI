package it.pintux.life.bedwarsaddon.provider;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import it.pintux.life.bedwarsaddon.api.ArenaProvider;
import it.pintux.life.bedwarsaddon.model.ArenaInfo;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** BedWars1058 implementation of {@link ArenaProvider}. The ONLY arena class touching com.andrei1058.*. */
public final class BedWars1058ArenaProvider implements ArenaProvider {
    private final Logger logger;
    private final BedWars1058ApiAccess access;

    public BedWars1058ArenaProvider(Logger logger, BedWars1058ApiAccess access) {
        this.logger = logger;
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars1058"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    @Override
    public List<ArenaInfo> getArenas() {
        BedWars api = access.get();
        if (api == null) return List.of();
        List<ArenaInfo> out = new ArrayList<>();
        for (IArena arena : api.getArenaUtil().getArenas()) {
            if (arena == null) continue;
            GameState status = arena.getStatus();
            String display = arena.getDisplayName() != null ? ChatColor.stripColor(arena.getDisplayName()) : arena.getArenaName();
            int players = arena.getPlayers() != null ? arena.getPlayers().size() : 0;
            out.add(new ArenaInfo(
                    arena.getArenaName(),
                    display,
                    status != null ? status.toString() : "",
                    players,
                    arena.getMaxPlayers(),
                    arena.getGroup() != null ? arena.getGroup() : ""));
        }
        return out;
    }

    @Override
    public boolean join(Player player, String arenaName) {
        BedWars api = access.get();
        if (api == null) return false;
        IArena arena = api.getArenaUtil().getArenaByName(arenaName);
        if (arena == null) return false;
        try {
            return arena.addPlayer(player, false);
        } catch (Exception e) {
            logger.warning("Join failed for arena " + arenaName + ": " + e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean ownsInventory(Inventory inventory) {
        if (inventory == null) return false;
        InventoryHolder holder = inventory.getHolder();
        return holder != null && holder.getClass().getName().contains("ArenaSelectorHolder");
    }
}
