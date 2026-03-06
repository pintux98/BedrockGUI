package it.pintux.life.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import it.pintux.life.velocity.utils.VelocityPlayer;

import it.pintux.life.common.utils.ConfigConverter;
import it.pintux.life.common.utils.MessageData;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class VelocityCommandExecutor implements SimpleCommand {

    private final BedrockGUI plugin;

    public VelocityCommandExecutor(BedrockGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            if(source.hasPermission("bedrockgui.admin")) {
                source.sendMessage(Component.text(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_RELOAD, null, null)));
                source.sendMessage(Component.text(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPENFOR, null, null)));
                source.sendMessage(Component.text(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_CONVERT, null, null)));
            }
            source.sendMessage(Component.text(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPEN, null, null)));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload": {
                if (!source.hasPermission("bedrockgui.admin")) {
                    String msg = plugin.getMessageData().getValue(MessageData.COMMAND_NO_PERMISSION, null, null);
                    source.sendMessage(Component.text(msg.replace("§", ""), NamedTextColor.RED));
                    return;
                }
                
                plugin.getLogger().info("Reloading BedrockGUI...");
                plugin.reloadData();
                String msg = plugin.getMessageData().getValue(MessageData.COMMAND_RELOAD_SUCCESS, null, null);
                source.sendMessage(Component.text(msg.replace("§", ""), NamedTextColor.GREEN));
                break;
            }
            
            case "convert":
            case "convertforms": {
                if (!source.hasPermission("bedrockgui.admin")) {
                    String msg = plugin.getMessageData().getValue(MessageData.COMMAND_NO_PERMISSION, null, null);
                    source.sendMessage(Component.text(msg.replace("§", ""), NamedTextColor.RED));
                    return;
                }
                try {
                    ConfigConverter converter = new ConfigConverter(plugin.getDataDirectory().toFile(), Logger.getLogger("BedrockGUI"));
                    String backup = converter.backupConfig();
                    int converted = converter.convert();
                    plugin.reloadData(); // Reload to pick up changes
                    
                    java.util.Map<String, Object> replacements = new java.util.HashMap<>();
                    replacements.put("count", converted);
                    replacements.put("backup", backup != null ? backup : "unknown");
                    String msg = plugin.getMessageData().getValue(MessageData.FORMS_CONVERSION_SUCCESS, replacements, null);
                    source.sendMessage(Component.text(msg.replace("§", ""), NamedTextColor.GREEN));
                } catch (Exception e) {
                    java.util.Map<String, Object> replacements = new java.util.HashMap<>();
                    replacements.put("error", e.getMessage());
                    String msg = plugin.getMessageData().getValue(MessageData.FORMS_CONVERSION_FAILED, replacements, null);
                    source.sendMessage(Component.text(msg.replace("§", ""), NamedTextColor.RED));
                    plugin.getLogger().error("Form conversion error: " + e.getMessage(), e);
                }
                break;
            }
                
            case "open": {
                if (args.length < 2) {
                    String umsg = plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPEN, null, null);
                    source.sendMessage(Component.text(umsg.isEmpty() ? "Usage: /bedrockgui open <menu> [args...]" : umsg, NamedTextColor.RED));
                    return;
                }
                
                String menuName = args[1];
                // args[2...] are menu args
                String[] menuArgs = args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : new String[0];
                
                if (source instanceof com.velocitypowered.api.proxy.Player) {
                    com.velocitypowered.api.proxy.Player player = (com.velocitypowered.api.proxy.Player) source;
                    VelocityPlayer vp = new VelocityPlayer(player);
                    plugin.getApi().openMenu(vp, menuName, menuArgs);
                    // Message handled by API usually, or we can send success
                } else {
                    String cmsg = plugin.getMessageData().getValue(MessageData.COMMAND_PLAYER_ONLY, null, null);
                    source.sendMessage(Component.text(cmsg.isEmpty() ? "You must specify a player when running from console." : cmsg, NamedTextColor.RED));
                }
                break;
            }
                
            case "openfor": {
                if (!source.hasPermission("bedrockgui.admin")) {
                    String msg = plugin.getMessageData().getValue(MessageData.COMMAND_NO_PERMISSION, null, null);
                    source.sendMessage(Component.text(msg.replace("§", ""), NamedTextColor.RED));
                    return;
                }
                if (args.length < 3) {
                    String msg = plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPENFOR, null, null);
                    source.sendMessage(Component.text(msg.replace("§", ""), NamedTextColor.RED));
                    return;
                }
                
                String targetName = args[1];
                String targetMenu = args[2];
                String[] targetArgs = args.length > 3 ? Arrays.copyOfRange(args, 3, args.length) : new String[0];
                
                plugin.getServer().getPlayer(targetName).ifPresentOrElse(
                    player -> {
                        VelocityPlayer vp = new VelocityPlayer(player);
                        plugin.getApi().openMenu(vp, targetMenu, targetArgs);
                        java.util.Map<String, Object> replacements = new java.util.HashMap<>();
                        replacements.put("menu", targetMenu);
                        replacements.put("player", player.getUsername());
                        String msg = plugin.getMessageData().getValue(MessageData.COMMAND_OPENED_FOR, replacements, null);
                        source.sendMessage(Component.text(msg.replace("§", ""), NamedTextColor.GREEN));
                    },
                    () -> {
                        String nf = plugin.getMessageData().getValue(MessageData.PLAYER_NOT_FOUND, java.util.Map.of("player", targetName), null);
                        source.sendMessage(Component.text(nf.replace("§", ""), NamedTextColor.RED));
                    }
                );
                break;
            }
                
            default:
                String umsg2 = plugin.getMessageData().getValue("usage", null, null);
                source.sendMessage(Component.text(umsg2.isEmpty() ? "Unknown subcommand. Usage: /bedrockgui <reload | open | openfor | convert>" : umsg2, NamedTextColor.RED));
                break;
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // Allow basic access, check permissions in subcommands
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        
        if (args.length == 0) {
            return List.of("reload", "open", "openfor", "convert");
        }
        
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return List.of("reload", "open", "openfor", "convert").stream()
                    .filter(cmd -> cmd.startsWith(prefix))
                    .toList();
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return plugin.getFormMenuUtil() != null ? 
                   List.copyOf(plugin.getFormMenuUtil().getFormMenus().keySet()) : 
                   List.of();
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("openfor")) {
            return plugin.getServer().getAllPlayers().stream()
                    .map(com.velocitypowered.api.proxy.Player::getUsername)
                    .toList();
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("openfor")) {
             return plugin.getFormMenuUtil() != null ? 
                   List.copyOf(plugin.getFormMenuUtil().getFormMenus().keySet()) : 
                   List.of();
        }
        
        return List.of();
    }
}
