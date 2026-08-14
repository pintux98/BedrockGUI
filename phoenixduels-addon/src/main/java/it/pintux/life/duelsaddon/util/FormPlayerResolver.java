package it.pintux.life.duelsaddon.util;

import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Recovers the Bukkit player behind a BedrockGUI {@link FormPlayer}.
 *
 * <p>Actions can be fired from forms this addon did not build, so the wrapper is not guaranteed to
 * be {@link BukkitFormPlayer}. A {@code getBukkitPlayer()} accessor is tried reflectively for other
 * platform wrappers before falling back to a uuid lookup.</p>
 */
public final class FormPlayerResolver {
    private FormPlayerResolver() {}

    /**
     * @return the Bukkit player, or {@code null} when they are offline or unresolvable
     */
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
