package it.pintux.life.bedwarsaddon.provider;

import com.tomkeuper.bedwars.api.BedWars;
import com.tomkeuper.bedwars.api.arena.IArena;
import it.pintux.life.bedwarsaddon.api.SpectatorProvider;
import it.pintux.life.bedwarsaddon.model.SpectateTarget;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/** BedWars2023 implementation of {@link SpectatorProvider}. The ONLY spectator class touching com.tomkeuper.*. */
public final class BedWars2023SpectatorProvider implements SpectatorProvider {
    private final Logger logger;
    private final BedWarsApiAccess access;

    public BedWars2023SpectatorProvider(Logger logger, BedWarsApiAccess access) {
        this.logger = logger;
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars2023"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    @Override
    public List<SpectateTarget> getTargets(Player spectator) {
        BedWars api = access.get();
        if (api == null) return List.of();
        IArena arena = api.getArenaUtil().getArenaByPlayer(spectator);
        if (arena == null) return List.of();
        List<SpectateTarget> out = new ArrayList<>();
        for (Player p : arena.getPlayers()) { // alive players (spectators excluded)
            if (p == null || p.equals(spectator)) continue;
            out.add(new SpectateTarget(p.getUniqueId().toString(), p.getName()));
        }
        return out;
    }

    @Override
    public boolean teleport(Player spectator, String targetUuid) {
        try {
            Player target = Bukkit.getPlayer(UUID.fromString(targetUuid));
            if (target == null || !target.isOnline()) return false;
            spectator.teleport(target.getLocation());
            return true;
        } catch (Exception e) {
            logger.warning("Spectator teleport failed: " + e.getClass().getSimpleName());
            return false;
        }
    }
}
