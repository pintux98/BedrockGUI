package it.pintux.life.bedwarsaddon.provider;

import it.pintux.life.bedwarsaddon.api.SpectatorProvider;
import it.pintux.life.bedwarsaddon.model.SpectateTarget;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.screamingsandals.bedwars.api.BedwarsAPI;
import org.screamingsandals.bedwars.api.game.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/** ScreamingBedWars implementation of {@link SpectatorProvider}. */
public final class SbwSpectatorProvider implements SpectatorProvider {
    private final Logger logger;
    private final SbwApiAccess access;

    public SbwSpectatorProvider(Logger logger, SbwApiAccess access) {
        this.logger = logger;
        this.access = access;
    }

    @Override public String getProviderId() { return "ScreamingBedWars"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    @Override
    public List<SpectateTarget> getTargets(Player spectator) {
        BedwarsAPI api = access.get();
        if (api == null) return List.of();
        Game game = api.getGameOfPlayer(spectator);
        if (game == null) return List.of();
        List<SpectateTarget> out = new ArrayList<>();
        for (Player p : game.getConnectedPlayers()) {
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
            logger.warning("SBW spectator teleport failed: " + e.getClass().getSimpleName());
            return false;
        }
    }
}
