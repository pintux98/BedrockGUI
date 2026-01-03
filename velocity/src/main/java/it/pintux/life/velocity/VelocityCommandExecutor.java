package it.pintux.life.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import it.pintux.life.velocity.utils.VelocityPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
            String msg = plugin.getMessageData().getValueNoPrefix("usage", null, null);
            source.sendMessage(Component.text(msg.isEmpty() ? "Usage: /bedrockgui <reload | open>" : msg, NamedTextColor.RED));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!source.hasPermission("bedrockgui.admin")) {
                    String msg = plugin.getMessageData().getValue("no-permission", null, null);
                    source.sendMessage(Component.text(msg.replace("§", ""), NamedTextColor.RED));
                    return;
                }
                
                plugin.getLogger().info("Reloading BedrockGUI...");
                plugin.reloadData();
                String msg = plugin.getMessageData().getValue("reload-success", null, null);
                source.sendMessage(Component.text(msg.replace("§", ""), NamedTextColor.GREEN));
                break;
                
            case "open":
                if (args.length < 2) {
                    String umsg = plugin.getMessageData().getValueNoPrefix("usage-open", null, null);
                    source.sendMessage(Component.text(umsg.isEmpty() ? "Usage: /bedrockgui open <menu> [player]" : umsg, NamedTextColor.RED));
                    return;
                }
                
                String menuName = args[1];
                String targetPlayer = args.length > 2 ? args[2] : null;
                
                // Handle menu opening logic
                if (targetPlayer != null) {
                    plugin.getServer().getPlayer(targetPlayer).ifPresentOrElse(
                        player -> {
                            VelocityPlayer vp = new VelocityPlayer(player);
                            plugin.getApi().openMenu(vp, menuName);
                            String ok = plugin.getMessageData().getValue("menu-opened", java.util.Map.of("menu", menuName), vp);
                            source.sendMessage(Component.text(ok.replace("§", ""), NamedTextColor.GREEN));
                        },
                        () -> {
                            String nf = plugin.getMessageData().getValue("player-not-found", java.util.Map.of("player", targetPlayer), null);
                            source.sendMessage(Component.text(nf.replace("§", ""), NamedTextColor.RED));
                        }
                    );
                } else {
                    if (source instanceof com.velocitypowered.api.proxy.Player) {
                        com.velocitypowered.api.proxy.Player player = (com.velocitypowered.api.proxy.Player) source;
                        VelocityPlayer vp = new VelocityPlayer(player);
                        plugin.getApi().openMenu(vp, menuName);
                        String ok = plugin.getMessageData().getValue("menu-opened", java.util.Map.of("menu", menuName), vp);
                        source.sendMessage(Component.text(ok.replace("§", ""), NamedTextColor.GREEN));
                    } else {
                        String cmsg = plugin.getMessageData().getValueNoPrefix("console-player-required", null, null);
                        source.sendMessage(Component.text(cmsg.isEmpty() ? "You must specify a player when running from console." : cmsg, NamedTextColor.RED));
                    }
                }
                break;
                
            default:
                String umsg2 = plugin.getMessageData().getValueNoPrefix("usage", null, null);
                source.sendMessage(Component.text(umsg2.isEmpty() ? "Unknown subcommand. Usage: /bedrockgui <reload | open>" : umsg2, NamedTextColor.RED));
                break;
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bedrockgui.admin");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        
        if (args.length == 0) {
            return List.of("reload", "open");
        }
        
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return List.of("reload", "open").stream()
                    .filter(cmd -> cmd.startsWith(prefix))
                    .toList();
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            // Suggest available menu names
            return plugin.getFormMenuUtil() != null ? 
                   List.copyOf(plugin.getFormMenuUtil().getFormMenus().keySet()) : 
                   List.of();
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("open")) {
            // Suggest online player names
            return plugin.getServer().getAllPlayers().stream()
                    .map(com.velocitypowered.api.proxy.Player::getUsername)
                    .toList();
        }
        
        return List.of();
    }
}
