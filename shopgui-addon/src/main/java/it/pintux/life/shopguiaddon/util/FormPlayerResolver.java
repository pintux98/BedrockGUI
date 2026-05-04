package it.pintux.life.shopguiaddon.util;

import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class FormPlayerResolver {
    private FormPlayerResolver() {
    }

    public static Player resolve(FormPlayer formPlayer) {
        if (formPlayer == null) {
            return null;
        }
        if (formPlayer instanceof BukkitFormPlayer bukkitFormPlayer) {
            return bukkitFormPlayer.getPlayer();
        }
        try {
            Method method = formPlayer.getClass().getMethod("getBukkitPlayer");
            Object value = method.invoke(formPlayer);
            if (value instanceof Player player) {
                return player;
            }
        } catch (Exception ignored) {
        }
        return Bukkit.getPlayer(formPlayer.getUniqueId());
    }
}
