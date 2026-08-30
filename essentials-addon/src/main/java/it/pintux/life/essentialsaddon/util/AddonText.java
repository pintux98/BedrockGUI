package it.pintux.life.essentialsaddon.util;

import it.pintux.life.common.api.BedrockGUIApi;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AddonText {

    private AddonText() {
    }

    public static String format(CommandSender receiver, String text) {
        if (text == null) {
            return null;
        }
        BedrockGUIApi api;
        try {
            api = BedrockGUIApi.getInstance();
        } catch (IllegalStateException ignored) {
            return text;
        }
        FormPlayer formPlayer = receiver instanceof Player ? new BukkitFormPlayer((Player) receiver) : null;
        return api.formatText(formPlayer, text);
    }

    public static void send(CommandSender receiver, String text) {
        if (receiver == null || text == null) {
            return;
        }
        receiver.sendMessage(format(receiver, text));
    }
}
