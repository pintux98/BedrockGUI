package it.pintux.life.paper.addon;

import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.paper.utils.PaperPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class FormPlayerResolver {
    private FormPlayerResolver() {
    }

    public static Player resolve(FormPlayer formPlayer) {
        if (formPlayer == null) {
            return null;
        }
        if (formPlayer instanceof PaperPlayer paperPlayer) {
            return paperPlayer.getBukkitPlayer();
        }
        return Bukkit.getPlayer(formPlayer.getUniqueId());
    }
}
