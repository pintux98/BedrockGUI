package it.pintux.life.bungee;

import it.pintux.life.common.utils.MessageData;
import it.pintux.life.common.utils.ConfigConverter;
import it.pintux.life.bungee.utils.BungeePlayer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Arrays;

public class BungeeCommand extends Command {
    private final BedrockGUI plugin;
    public BungeeCommand(BedrockGUI plugin) { super("bedrockgui", "bedrockgui.admin", new String[]{"bgui"}); this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = sender instanceof ProxiedPlayer ? (ProxiedPlayer) sender : null;
        if (args.length == 0) {
            if(player.hasPermission("bedrockgui.admin")) {
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