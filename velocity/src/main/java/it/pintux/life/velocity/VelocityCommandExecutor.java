package it.pintux.life.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
            source.sendMessage(Component.text("Usage: /bedrockgui <reload | open>", NamedTextColor.RED));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!source.hasPermission("bedrockgui.admin")) {
                    source.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
                    return;
                }
                
                plugin.getLogger().info("Reloading BedrockGUI...");
                plugin.reloadData();
                source.sendMessage(Component.text("BedrockGUI reloaded successfully!", NamedTextColor.GREEN));
                break;
                
            case "open":
                if (args.length < 2) {
                    source.sendMessage(Component.text("Usage: /bedrockgui open <menu> [player]", NamedTextColor.RED));
                    return;
                }
                
                String menuName = args[1];
                String targetPlayer = args.length > 2 ? args[2] : null;
                
                // Handle menu opening logic
                if (targetPlayer != null) {
                    plugin.getServer().getPlayer(targetPlayer).ifPresentOrElse(
                        player -> {
                            // Open menu for specific player
                            plugin.getLogger().info("Opening menu " + menuName + " for player " + targetPlayer);
                            source.sendMessage(Component.text("Menu " + menuName + " opened for " + targetPlayer, NamedTextColor.GREEN));
                        },
                        () -> source.sendMessage(Component.text("Player not found: " + targetPlayer, NamedTextColor.RED))
                    );
                } else {
                    if (source instanceof com.velocitypowered.api.proxy.Player) {
                        com.velocitypowered.api.proxy.Player player = (com.velocitypowered.api.proxy.Player) source;
                        plugin.getLogger().info("Opening menu " + menuName + " for " + player.getUsername());
                        source.sendMessage(Component.text("Menu " + menuName + " opened!", NamedTextColor.GREEN));
                    } else {
                        source.sendMessage(Component.text("You must specify a player when running from console.", NamedTextColor.RED));
                    }
                }
                break;
                
            default:
                source.sendMessage(Component.text("Unknown subcommand. Usage: /bedrockgui <reload | open>", NamedTextColor.RED));
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