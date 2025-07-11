package it.pintux.life.bungee;

import it.pintux.life.bungee.utils.BungeePlayer;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.utils.MessageData;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BedrockCommand extends Command implements TabExecutor {

    private final BedrockGUI plugin;

    public BedrockCommand(BedrockGUI plugin, String name) {
        super(name, "bedrockgui.admin", "bguiproxy");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer))
            return;
        ProxiedPlayer player = (ProxiedPlayer) commandSender;
        if (strings.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /bgui reload");
            player.sendMessage(ChatColor.RED + "Usage: /bgui open <menu_name>");
            return;
        }
        String arg = strings[0];
        if (arg.equalsIgnoreCase("reload")) {
            if (!player.hasPermission("bedrockgui.admin")) {
                player.sendMessage(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null));
                return;
            }
            plugin.reloadData();
            player.sendMessage(ChatColor.GREEN + "Reloaded BedrockGUI!");
            return;
        }
        if (arg.equalsIgnoreCase("open")) {
            if (strings.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /bgui open <menu_name> [arguments]");
                return;
            }

            try {
                if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                    player.sendMessage(plugin.getMessageData().getValue(MessageData.MENU_NOJAVA, null, null));
                    return;
                }
            } catch (Exception e) {
                player.sendMessage(plugin.getMessageData().getValue(MessageData.MENU_NOJAVA, null, null));
                return;
            }
            String menuName = strings[1];
            String[] menuArgs = Arrays.copyOfRange(strings, 2, strings.length);
            BungeePlayer player1 = new BungeePlayer(player);
            plugin.getFormMenuUtil().openForm(player1, menuName, menuArgs);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof ProxiedPlayer)) {
            return new ArrayList<>();
        }

        if (!commandSender.hasPermission("bedrockgui.admin")) {
            return new ArrayList<>();
        }

        if (strings.length == 1) {
            List<String> commands = new ArrayList<>();
            commands.add("reload");
            commands.add("open");
            return commands;
        }
        if (strings.length == 2 && strings[0].equalsIgnoreCase("open")) {
            return Stream.of(plugin.getFormMenuUtil().getFormMenus().keySet())
                    .flatMap(Set::stream)
                    .map(String::toLowerCase)
                    .filter(c -> c.startsWith(strings[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
