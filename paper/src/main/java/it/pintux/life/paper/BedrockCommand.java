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
        if (!(sender instanceof Player))
            return true;
        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /bgui reload");
            sender.sendMessage(ChatColor.RED + "Usage: /bgui open <menu_name>");
            //sender.sendMessage(ChatColor.RED + "Usage: /bgui pack <reload|list|send> [args]");
            //sender.sendMessage(ChatColor.RED + "Usage: /bgui themes [player]");
            return true;
        }
        String arg = args[0];
        if (arg.equalsIgnoreCase("reload")) {
            if (!player.hasPermission("bedrockgui.admin")) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null));
                return true;
            }
            plugin.reloadData();
            //plugin.reloadResourcePacks();
            sender.sendMessage(ChatColor.GREEN + "Reloaded BedrockGUI and features!");
            return true;
        }

        if (arg.equalsIgnoreCase("open")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /bgui open <menu_name> [arguments]");
                return true;
            }

            PaperPlayerChecker playerChecker = new PaperPlayerChecker();
            if (!playerChecker.isBedrockPlayer(player.getUniqueId())) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.MENU_NOJAVA, null, null));
                return true;
            }
            String menuName = args[1];
            String[] menuArgs = Arrays.copyOfRange(args, 2, args.length);
            PaperPlayer player1 = new PaperPlayer(player);

            // Open form using the form menu utility
            plugin.getFormMenuUtil().openForm(player1, menuName, menuArgs);
        }

        //if (arg.equalsIgnoreCase("pack")) {
        //    if (!player.hasPermission("bedrockgui.admin")) {
        //        sender.sendMessage(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null));
        //        return true;
        //    }
//
        //    if (args.length < 2) {
        //        sender.sendMessage(ChatColor.RED + "Usage: /bgui pack <reload|list|send> [args]");
        //        return true;
        //    }
//
        //    String packAction = args[1];
//
        //    if (packAction.equalsIgnoreCase("reload")) {
        //        plugin.reloadResourcePacks();
        //        sender.sendMessage(ChatColor.GREEN + "Resource packs reloaded!");
        //        return true;
        //    }
//
        //    if (packAction.equalsIgnoreCase("list")) {
        //        if (!plugin.getApi().isResourcePacksEnabled()) {
        //            sender.sendMessage(ChatColor.RED + "Resource packs are disabled!");
        //            return true;
        //        }
//
        //        Set<String> packs = plugin.getApi().getResourcePackManager().getLoadedPacks();
        //        sender.sendMessage(ChatColor.GREEN + "Loaded Resource Packs:");
        //        for (String pack : packs) {
        //            sender.sendMessage(ChatColor.YELLOW + "- " + pack);
        //        }
        //        return true;
        //    }
//
        //    if (packAction.equalsIgnoreCase("send")) {
        //        if (args.length < 4) {
        //            sender.sendMessage(ChatColor.RED + "Usage: /bgui pack send <player> <menu_name>");
        //            return true;
        //        }
//
        //        Player targetPlayer = plugin.getServer().getPlayer(args[2]);
        //        if (targetPlayer == null) {
        //            sender.sendMessage(ChatColor.RED + "Player not found!");
        //            return true;
        //        }
//
        //        PaperPlayerChecker playerChecker = new PaperPlayerChecker();
        //        PaperPlayerChecker targetPlayerChecker = new PaperPlayerChecker();
        //        if (!targetPlayerChecker.isBedrockPlayer(targetPlayer.getUniqueId())) {
        //            sender.sendMessage(ChatColor.RED + "Target player is not a Bedrock player!");
        //            return true;
        //        }
//
        //        String menuName = args[3];
        //        plugin.getApi().sendResourcePack(targetPlayer.getUniqueId(), menuName);
        //        sender.sendMessage(ChatColor.GREEN + "Sent resource pack for menu '" + menuName + "' to " + targetPlayer.getName());
        //        return true;
        //    }
        //}

        //if (arg.equalsIgnoreCase("themes")) {
        //    Player targetPlayer = player;
//
        //    if (args.length > 1 && player.hasPermission("bedrockgui.admin")) {
        //        targetPlayer = plugin.getServer().getPlayer(args[1]);
        //        if (targetPlayer == null) {
        //            sender.sendMessage(ChatColor.RED + "Player not found!");
        //            return true;
        //        }
        //    }
//
        //    PaperPlayerChecker themePlayerChecker = new PaperPlayerChecker();
        //    if (!themePlayerChecker.isBedrockPlayer(targetPlayer.getUniqueId())) {
        //        sender.sendMessage(ChatColor.RED + "Target player is not a Bedrock player!");
        //        return true;
        //    }
//
        //    PaperPlayer paperPlayer = new PaperPlayer(targetPlayer);
        //    Set<String> themes = plugin.getApi().getAvailableThemes(paperPlayer);
//
        //    if (themes.isEmpty()) {
        //        sender.sendMessage(ChatColor.YELLOW + "No themes available for " + targetPlayer.getName());
        //    } else {
        //        sender.sendMessage(ChatColor.GREEN + "Available themes for " + targetPlayer.getName() + ":");
        //        for (String theme : themes) {
        //            sender.sendMessage(ChatColor.YELLOW + "- " + theme);
        //        }
        //    }
        //    return true;
        //}

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
            //commands.add("pack");
            //commands.add("themes");
            return commands;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return Stream.of(plugin.getFormMenuUtil().getFormMenus().keySet())
                    .flatMap(Set::stream)
                    .map(String::toLowerCase)
                    .filter(c -> c.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        //if (args.length == 2 && args[0].equalsIgnoreCase("pack")) {
        //    List<String> packCommands = new ArrayList<>();
        //    packCommands.add("reload");
        //    packCommands.add("list");
        //    packCommands.add("send");
        //    return packCommands.stream()
        //            .filter(c -> c.startsWith(args[1].toLowerCase()))
        //            .collect(Collectors.toList());
        //}
//
        //if (args.length == 3 && args[0].equalsIgnoreCase("pack") && args[1].equalsIgnoreCase("send")) {
        //    return plugin.getServer().getOnlinePlayers().stream()
        //            .map(Player::getName)
        //            .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
        //            .collect(Collectors.toList());
        //}
//
        //if (args.length == 4 && args[0].equalsIgnoreCase("pack") && args[1].equalsIgnoreCase("send")) {
        //    return Stream.of(plugin.getFormMenuUtil().getFormMenus().keySet())
        //            .flatMap(Set::stream)
        //            .map(String::toLowerCase)
        //            .filter(c -> c.startsWith(args[3].toLowerCase()))
        //            .collect(Collectors.toList());
        //}
//
        //if (args.length == 2 && args[0].equalsIgnoreCase("themes")) {
        //    return plugin.getServer().getOnlinePlayers().stream()
        //            .map(Player::getName)
        //            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
        //            .collect(Collectors.toList());
        //}

        return new ArrayList<>();
    }
}
