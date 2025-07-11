package org.pintux.life.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.floodgate.api.FloodgateApi;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import org.pintux.life.velocity.utils.VelocityPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BedrockCommand implements SimpleCommand {

    private final BedrockGUI plugin;

    public BedrockCommand(BedrockGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(Component.text("Usage: /bedrockguivelocity reload", NamedTextColor.RED));
            source.sendMessage(Component.text("Usage: /bedrockguivelocity open <menu_name>", NamedTextColor.RED));
            source.sendMessage(Component.text("Usage: /bedrockguivelocity openfor <player> <menu_name> [arguments]", NamedTextColor.RED));
            return;
        }

        String arg = args[0];

        if (arg.equalsIgnoreCase("reload")) {
            if (!source.hasPermission("bedrockgui.admin")) {
                source.sendMessage(Component.text(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null), NamedTextColor.RED));
                return;
            }

            plugin.reloadData();
            source.sendMessage(Component.text("Reloaded BedrockGUI!", NamedTextColor.GREEN));
            return;
        }

        if (arg.equalsIgnoreCase("open")) {
            if (source instanceof Player) {
                Player player = (Player) source;
                if (args.length < 2) {
                    source.sendMessage(Component.text("Usage: /bedrockguivelocity open <menu_name> [arguments]", NamedTextColor.RED));
                    return;
                }

                try {
                    if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                        source.sendMessage(Component.text(plugin.getMessageData().getValue(MessageData.MENU_NOJAVA, null, null), NamedTextColor.RED));
                        return;
                    }
                } catch (Exception e) {
                    source.sendMessage(Component.text(plugin.getMessageData().getValue(MessageData.MENU_NOJAVA, null, null), NamedTextColor.RED));
                    return;
                }

                String menuName = args[1];
                String[] menuArgs = Arrays.copyOfRange(args, 2, args.length);
                String serverName = player.getCurrentServer().map(server -> server.getServerInfo().getName()).orElse("");
                
                // Server requirement check removed - not implemented in FormMenuUtil
                FormPlayer formPlayer = new VelocityPlayer(player, plugin.getServer());
                plugin.getFormMenuUtil().openForm(formPlayer, menuName, menuArgs);
            }
            return;
        }

        if (arg.equalsIgnoreCase("openfor")) {
            if (!source.hasPermission("bedrockgui.admin")) {
                source.sendMessage(Component.text(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null), NamedTextColor.RED));
                return;
            }
            if (args.length < 3) {
                source.sendMessage(Component.text("Usage: /bedrockguivelocity openfor <player> <menu_name> [arguments]", NamedTextColor.RED));
                return;
            }

            String playerName = args[1];
            String menuName = args[2];
            String[] menuArgs = Arrays.copyOfRange(args, 3, args.length);

            Player targetPlayer = plugin.getServer().getPlayer(playerName).orElse(null);
            if (targetPlayer == null) {
                source.sendMessage(Component.text("Player '" + playerName + "' is not online.", NamedTextColor.RED));
                return;
            }

            try {
                if (!FloodgateApi.getInstance().isFloodgatePlayer(targetPlayer.getUniqueId())) {
                    source.sendMessage(Component.text(plugin.getMessageData().getValue(MessageData.MENU_NOJAVA, null, null), NamedTextColor.RED));
                    return;
                }
            } catch (Exception e) {
                source.sendMessage(Component.text(plugin.getMessageData().getValue(MessageData.MENU_NOJAVA, null, null), NamedTextColor.RED));
                return;
            }
            
            String serverName = targetPlayer.getCurrentServer().map(server -> server.getServerInfo().getName()).orElse("");
            // Server requirement check removed - not implemented in FormMenuUtil
            FormPlayer formPlayer = new VelocityPlayer(targetPlayer, plugin.getServer());
            plugin.getFormMenuUtil().openForm(formPlayer, menuName, menuArgs);
            source.sendMessage(Component.text("Opened menu '" + menuName + "' for player '" + playerName + "'.", NamedTextColor.GREEN));
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        if (!source.hasPermission("bedrockgui.admin")) {
            if (args.length == 1) {
                List<String> commands = new ArrayList<>();
                commands.add("open");
                return CompletableFuture.completedFuture(commands);
            }
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            commands.add("reload");
            commands.add("open");
            commands.add("openfor");
            return CompletableFuture.completedFuture(commands);
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            return CompletableFuture.completedFuture(
                Stream.of(plugin.getFormMenuUtil().getFormMenus().keySet())
                    .flatMap(Set::stream)
                    .map(String::toLowerCase)
                    .filter(c -> c.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList())
            );
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("openfor")) {
            return CompletableFuture.completedFuture(
                plugin.getServer().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList())
            );
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("openfor")) {
            return CompletableFuture.completedFuture(
                Stream.of(plugin.getFormMenuUtil().getFormMenus().keySet())
                    .flatMap(Set::stream)
                    .map(String::toLowerCase)
                    .filter(c -> c.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList())
            );
        }

        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("bedrockgui.use");
    }
}