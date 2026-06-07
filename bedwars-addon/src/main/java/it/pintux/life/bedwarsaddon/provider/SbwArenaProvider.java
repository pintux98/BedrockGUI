package it.pintux.life.bedwarsaddon.provider;

import it.pintux.life.bedwarsaddon.api.ArenaProvider;
import it.pintux.life.bedwarsaddon.model.ArenaInfo;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.screamingsandals.bedwars.api.BedwarsAPI;
import org.screamingsandals.bedwars.api.game.Game;
import org.screamingsandals.bedwars.api.game.GameStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** ScreamingBedWars implementation of {@link ArenaProvider}. The ONLY arena class touching org.screamingsandals.*. */
public final class SbwArenaProvider implements ArenaProvider {
    private final Logger logger;
    private final SbwApiAccess access;

    public SbwArenaProvider(Logger logger, SbwApiAccess access) {
        this.logger = logger;
        this.access = access;
    }

    @Override public String getProviderId() { return "ScreamingBedWars"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    @Override
    public List<ArenaInfo> getArenas() {
        BedwarsAPI api = access.get();
        if (api == null) return List.of();
        List<ArenaInfo> out = new ArrayList<>();
        for (Game game : api.getGames()) {
            if (game == null) continue;
            GameStatus status = game.getStatus();
            out.add(new ArenaInfo(
                    game.getName(),
                    game.getName(),
                    status != null ? status.toString() : "",
                    game.countConnectedPlayers(),
                    game.getMaxPlayers(),
                    ""));
        }
        return out;
    }

    @Override
    public boolean join(Player player, String arenaName) {
        BedwarsAPI api = access.get();
        if (api == null) return false;
        Game game = api.getGameByName(arenaName);
        if (game == null) return false;
        try {
            game.joinToGame(player); // void; SBW handles routing/messages
            return true;
        } catch (Exception e) {
            logger.warning("SBW join failed for " + arenaName + ": " + e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean ownsInventory(Inventory inventory) {
        // SBW has no public arena-selector holder; the form is opened via /bedwarsaddon arena.
        return false;
    }
}
