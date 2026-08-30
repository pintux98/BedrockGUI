package it.pintux.life.paper.placeholders;

import it.pintux.life.paper.BedrockGUI;
import it.pintux.life.paper.utils.PaperPlayer;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.PlaceholderRegistry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class BedrockGUIExpansion extends PlaceholderExpansion {

    private final BedrockGUI plugin;

    public BedrockGUIExpansion(BedrockGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bgui";
    }

    @Override
    public @NotNull String getAuthor() {
        return "pintux";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        Player online = player == null ? null : player.getPlayer();
        FormPlayer formPlayer = online == null ? null : new PaperPlayer(online);
        return PlaceholderRegistry.shared().resolve(formPlayer, CorePlaceholders.IDENTIFIER, params);
    }
}
