package it.pintux.life.bedwarsaddon.provider;

import com.tomkeuper.bedwars.api.BedWars;
import com.tomkeuper.bedwars.api.arena.GameState;
import com.tomkeuper.bedwars.api.arena.IArena;
import it.pintux.life.bedwarsaddon.api.ArenaProvider;
import it.pintux.life.bedwarsaddon.model.ArenaInfo;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * BedWars2023 implementation of {@link ArenaProvider}. The ONLY arena class touching com.tomkeuper.*.
 * <p>
 * The native arena-selector GUI uses an inventory holder {@code ArenaGUI.ArenaSelectorHolder}, which
 * is plugin-internal (not in the API). We detect it by holder class name to keep the API dependency clean.
 */
public final class BedWars2023ArenaProvider implements ArenaProvider {
    private final Logger logger;
    private final BedWarsApiAccess access;

    public BedWars2023ArenaProvider(Logger logger, BedWarsApiAccess access) {
        this.logger = logger;
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars2023"; }

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
