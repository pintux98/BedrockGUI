package it.pintux.life.bungee;

import it.pintux.life.common.utils.MessageData;
import it.pintux.life.bungee.utils.BungeePlayer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BungeeCommand extends Command {
    private final BedrockGUI plugin;
    public BungeeCommand(BedrockGUI plugin) { super("bedrockgui", "bedrockgui.admin", new String[]{"bgui"}); this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = sender instanceof ProxiedPlayer ? (ProxiedPlayer) sender : null;
        if (args.length == 0) {
            sender.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("Usage: /bgui reload"));
            sender.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("Usage: /bgui open <menu_name>"));
            return;
        }
        String arg = args[0];
        if (arg.equalsIgnoreCase("reload")) {
            if (player != null && !player.hasPermission("bedrockgui.admin")) {
                sender.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null)));
                return;
            }
            plugin.reloadData();
            sender.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("Reloaded BedrockGUI and features!"));
            return;
        }
        if (arg.equalsIgnoreCase("open")) {
            if (player == null) {
                sender.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("Only players can use /bgui open"));
                return;
            }
            if (args.length < 2) {
                sender.sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText("Usage: /bgui open <menu_name> [arguments]"));
                return;
            }
            String menuName = args[1];
            String[] menuArgs = java.util.Arrays.copyOfRange(args, 2, args.length);
            plugin.getApi().openMenu(new BungeePlayer(player), menuName, menuArgs);
        }
    }
}