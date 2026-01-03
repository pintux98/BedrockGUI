package it.pintux.life.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import it.pintux.life.velocity.utils.VelocityPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class FormCommand implements SimpleCommand {
    private final BedrockGUI plugin;
    private final String menuKey;

    public FormCommand(BedrockGUI plugin, String menuKey) {
        this.plugin = plugin;
        this.menuKey = menuKey;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (source instanceof com.velocitypowered.api.proxy.Player p) {
            VelocityPlayer vp = new VelocityPlayer(p);
            plugin.getApi().openMenu(vp, menuKey, args);
            String ok = plugin.getMessageData().getValue("menu-opened", java.util.Map.of("menu", menuKey), vp);
            source.sendMessage(Component.text(ok.replace("§", ""), NamedTextColor.GREEN));
        } else {
            String cmsg = plugin.getMessageData().getValueNoPrefix("console-player-required", null, null);
            source.sendMessage(Component.text(cmsg.isEmpty() ? "You must specify a player when running from console." : cmsg, NamedTextColor.RED));
        }
    }
}

