package it.pintux.life.bungee;

import it.pintux.life.common.form.obj.FormMenu;
import it.pintux.life.common.utils.ConsoleTarget;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.bungee.utils.BungeePlayer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class BungeeFormCommand extends Command {
    private final BedrockGUI plugin;
    private final String menuKey;

    public BungeeFormCommand(BedrockGUI plugin, String commandName, String menuKey, String permission) {
        super(commandName, permission);
        this.plugin = plugin;
        this.menuKey = menuKey;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer player) {
            open(sender, new BungeePlayer(player), args);
            return;
        }
        ConsoleTarget target = ConsoleTarget.find(args, name -> plugin.getProxy().getPlayer(name) != null);
        if (target == null) {
            sender.sendMessage(BungeePlayer.text(plugin.getMessageData().getValue(MessageData.COMMAND_PLAYER_ONLY, null, null)));
            return;
        }
        open(sender, new BungeePlayer(plugin.getProxy().getPlayer(target.player())), target.args());
    }

    private void open(CommandSender sender, BungeePlayer player, String[] args) {
        FormMenu menu = plugin.getFormMenuUtil().getFormMenus().get(menuKey);
        int required = menu == null ? 0 : menu.getRequiredArgs();
        if (args.length < required) {
            sender.sendMessage(BungeePlayer.text(plugin.getMessageData().getValue(MessageData.MENU_ARGS, java.util.Map.of("args", required), null)));
            return;
        }
        plugin.getApi().openMenu(player, menuKey, args);
    }
}
