package it.pintux.life.bungee;

import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ConfigConverter;
import it.pintux.life.bungee.utils.BungeePlayer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BungeeCommand extends Command implements TabExecutor {
    private final BedrockGUI plugin;
    public BungeeCommand(BedrockGUI plugin) { super("bedrockgui", "bedrockgui.admin", new String[]{"bgui"}); this.plugin = plugin; }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> subCommands = new ArrayList<>();
            subCommands.add("open");
            if (sender.hasPermission("bedrockgui.admin")) {
                subCommands.add("reload");
                subCommands.add("openfor");
                subCommands.add("convert");
            }
            for (String cmd : subCommands) {
                if (cmd.startsWith(prefix)) {
                    suggestions.add(cmd);
                }
            }
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            String prefix = args[1].toLowerCase();
            if (plugin.getFormMenuUtil() != null) {
                for (String menuName : plugin.getFormMenuUtil().getFormMenus().keySet()) {
                    if (menuName.toLowerCase().startsWith(prefix)) {
                        suggestions.add(menuName);
                    }
                }
            }
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("openfor")) {
            String prefix = args[1].toLowerCase();
            for (net.md_5.bungee.api.connection.ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    suggestions.add(p.getName());
                }
            }
            return suggestions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("openfor")) {
            String prefix = args[2].toLowerCase();
            if (plugin.getFormMenuUtil() != null) {
                for (String menuName : plugin.getFormMenuUtil().getFormMenus().keySet()) {
                    if (menuName.toLowerCase().startsWith(prefix)) {
                        suggestions.add(menuName);
                    }
                }
            }
            return suggestions;
        }

        return suggestions;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = sender instanceof ProxiedPlayer ? (ProxiedPlayer) sender : null;
        if (args.length == 0) {
            if (player != null && player.hasPermission("bedrockgui.admin")) {
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_RELOAD, null, null)));
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPENFOR, null, null)));
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_CONVERT, null, null)));
            }
            sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPEN, null, null)));

            return;
        }
        String arg = args[0];
        
        if (arg.equalsIgnoreCase("reload")) {
            if (player != null && !player.hasPermission("bedrockgui.admin")) {
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null)));
                return;
            }
            plugin.reloadData();
            sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.COMMAND_RELOAD_SUCCESS, null, null)));
            return;
        }
        
        if (arg.equalsIgnoreCase("convert") || arg.equalsIgnoreCase("convertforms")) {
            if (player != null && !player.hasPermission("bedrockgui.admin")) {
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null)));
                return;
            }
            try {
                ConfigConverter converter = new ConfigConverter(plugin.getDataFolder(), plugin.getLogger());
                String backup = converter.backupConfig();
                
                int converted = converter.convert();
                plugin.reloadData(); // Reload config after conversion
                
                java.util.Map<String, Object> replacements = new java.util.HashMap<>();
                replacements.put("count", converted);
                replacements.put("backup", backup != null ? backup : "unknown");
                
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.FORMS_CONVERSION_SUCCESS, replacements, null)));
            } catch (Exception e) {
                java.util.Map<String, Object> replacements = new java.util.HashMap<>();
                replacements.put("error", e.getMessage());
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.FORMS_CONVERSION_FAILED, replacements, null)));
                plugin.getLogger().warning("Form conversion error: " + e.getMessage());
            }
            return;
        }

        if (arg.equalsIgnoreCase("open")) {
            if (player == null) {
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.COMMAND_PLAYER_ONLY, null, null)));
                return;
            }
            if (args.length < 2) {
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPEN, null, null)));
                return;
            }
            String menuName = args[1];
            String[] menuArgs = Arrays.copyOfRange(args, 2, args.length);
            plugin.getApi().openMenu(new BungeePlayer(player), menuName, menuArgs);
            return;
        }
        
        if (arg.equalsIgnoreCase("openfor")) {
            if (player != null && !player.hasPermission("bedrockgui.admin")) {
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null)));
                return;
            }
            if (args.length < 3) {
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPENFOR, null, null)));
                return;
            }

            String playerName = args[1];
            String menuName = args[2];
            String[] menuArgs = Arrays.copyOfRange(args, 3, args.length);
            
            ProxiedPlayer target = plugin.getProxy().getPlayer(playerName);
            if (target == null) {
                sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.PLAYER_NOT_FOUND, java.util.Map.of("player", playerName), null)));
                return;
            }
            
            plugin.getApi().openMenu(new BungeePlayer(target), menuName, menuArgs);
            sender.sendMessage(TextComponent.fromLegacyText(plugin.getMessageData().getValue(MessageData.COMMAND_OPENED_FOR, java.util.Map.of("menu", menuName, "player", target.getName()), null)));
            return;
        }
    }
}