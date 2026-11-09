package it.pintux.life.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import it.pintux.life.common.form.obj.FormMenu;
import it.pintux.life.common.utils.ConsoleTarget;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.velocity.utils.VelocityPlayer;

public class FormCommand implements SimpleCommand {
    private final BedrockGUI plugin;
    private final String menuKey;
    private final String permission;

    public FormCommand(BedrockGUI plugin, String menuKey, String permission) {
        this.plugin = plugin;
        this.menuKey = menuKey;
        this.permission = permission;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (source instanceof Player p) {
            open(source, new VelocityPlayer(p), args);
            return;
        }
        ConsoleTarget target = ConsoleTarget.find(args, name -> plugin.getServer().getPlayer(name).isPresent());
        if (target == null) {
            source.sendMessage(VelocityPlayer.text(plugin.getMessageData().getValue(MessageData.COMMAND_PLAYER_ONLY, null, null)));
            return;
        }
        plugin.getServer().getPlayer(target.player()).ifPresent(p -> open(source, new VelocityPlayer(p), target.args()));
    }

    private void open(CommandSource source, VelocityPlayer player, String[] args) {
        FormMenu menu = plugin.getFormMenuUtil().getFormMenus().get(menuKey);
        int required = menu == null ? 0 : menu.getRequiredArgs();
        if (args.length < required) {
            source.sendMessage(VelocityPlayer.text(plugin.getMessageData().getValue(MessageData.MENU_ARGS, java.util.Map.of("args", required), null)));
            return;
        }
        plugin.getApi().openMenu(player, menuKey, args);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return permission == null || !(invocation.source() instanceof Player) || invocation.source().hasPermission(permission);
    }
}
