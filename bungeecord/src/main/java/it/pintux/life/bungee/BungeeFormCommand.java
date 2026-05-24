package it.pintux.life.bungee;

import it.pintux.life.common.utils.MessageData;
import it.pintux.life.bungee.utils.BungeePlayer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.chat.TextComponent;

public class BungeeFormCommand extends Command {
    private final BedrockGUI plugin;
    private final String menuKey;

    public BungeeFormCommand(BedrockGUI plugin, String menuKey, String permission) {
        super(menuKey, permission);
        this.plugin = plugin;
        this.menuKey = menuKey;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(TextComponent.fromLegacyText(
                    plugin.getMessageData().getValue(MessageData.COMMAND_PLAYER_ONLY, null, null)));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) sender;
        plugin.getApi().openMenu(new BungeePlayer(player), menuKey, args);
    }
}
