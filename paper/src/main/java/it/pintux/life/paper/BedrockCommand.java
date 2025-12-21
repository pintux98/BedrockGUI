package it.pintux.life.paper;

import it.pintux.life.paper.platform.PaperPlayerChecker;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.paper.utils.PaperPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BedrockCommand implements CommandExecutor, TabCompleter {

    private final BedrockGUI plugin;

    public BedrockCommand(BedrockGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /bgui reload");
            sender.sendMessage(ChatColor.RED + "Usage: /bgui open <menu_name>");
            return true;
        }
        String arg = args[0];
        if (arg.equalsIgnoreCase("reload")) {
            if (player != null && !player.hasPermission("bedrockgui.admin")) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null));
                return true;
            }
            plugin.reloadData();
            sender.sendMessage(ChatColor.GREEN + "Reloaded BedrockGUI and features!");
            return true;
        }

        if (arg.equalsIgnoreCase("open")) {
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Only players can use /bgui open");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /bgui open <menu_name> [arguments]");
                return true;
            }

            String menuName = args[1];
            String[] menuArgs = Arrays.copyOfRange(args, 2, args.length);
            PaperPlayer player1 = new PaperPlayer(player);

            plugin.getApi().openMenu(player1, menuName, menuArgs);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (!sender.hasPermission("bedrockgui.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            commands.add("reload");
            commands.add("open");
            return commands;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return Stream.of(plugin.getFormMenuUtil().getFormMenus().keySet())
                    .flatMap(Set::stream)
                    .map(String::toLowerCase)
                    .filter(c -> c.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
