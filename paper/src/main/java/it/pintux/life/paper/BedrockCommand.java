package it.pintux.life.paper;

import it.pintux.life.paper.platform.PaperPlayerChecker;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.paper.utils.PaperPlayer;
import org.bukkit.Bukkit;
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

    private void copySection(org.bukkit.configuration.file.FileConfiguration src, String srcPath,
                              org.bukkit.configuration.file.YamlConfiguration dest, String destPath) {
        org.bukkit.configuration.ConfigurationSection section = src.getConfigurationSection(srcPath);
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            Object val = section.get(key);
            String destKey = destPath + "." + key;
            if (val instanceof org.bukkit.configuration.ConfigurationSection) {
                copySection(src, srcPath + "." + key, dest, destPath + "." + key);
            } else {
                if ("onClick".equalsIgnoreCase(key) && val instanceof java.util.List) {
                    java.util.List<?> list = (java.util.List<?>) val;
                    java.util.List<String> flattened = new java.util.ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof String) {
                            String s = (String) item;
                            String noNewlines = s.replaceAll("\\r?\\n", " ");
                            String normalized = noNewlines.replaceAll("-\\s*\\|", "- ");
                            normalized = normalized.replaceAll("\\s+", " ").trim();
                            flattened.add(normalized);
                        }
                    }
                    dest.set(destKey, flattened);
                } else {
                    dest.set(destKey, val);
                }
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /bgui reload");
            sender.sendMessage(ChatColor.RED + "Usage: /bgui open <menu_name>");
            sender.sendMessage(ChatColor.RED + "Usage: /bgui openfor <player_name> <menu_name>");
            sender.sendMessage(ChatColor.RED + "Usage: /bgui convert");
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

        if (arg.equalsIgnoreCase("convert") || arg.equalsIgnoreCase("convertforms")) {
            if (player != null && !player.hasPermission("bedrockgui.admin")) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null));
                return true;
            }
            try {
                java.io.File dataFolder = plugin.getDataFolder();
                org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
                // Backup config
                java.io.File configFile = new java.io.File(dataFolder, "config.yml");
                String backupName = "config_backup_" + System.currentTimeMillis() + ".yml";
                java.nio.file.Files.copy(configFile.toPath(), new java.io.File(dataFolder, backupName).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Ensure forms directory
                java.io.File formsDir = new java.io.File(dataFolder, "forms");
                formsDir.mkdirs();

                org.bukkit.configuration.ConfigurationSection formsSec = cfg.getConfigurationSection("forms");
                if (formsSec == null) {
                    sender.sendMessage(ChatColor.YELLOW + "No inline forms found to convert.");
                    return true;
                }

                int converted = 0;
                for (String key : formsSec.getKeys(false)) {
                    String existingFile = cfg.getString("forms." + key + ".file");
                    if (existingFile != null && !existingFile.trim().isEmpty()) {
                        continue;
                    }
                    String bedrockBase = "forms." + key;
                    String javaBase = bedrockBase + ".java";

                    org.bukkit.configuration.file.YamlConfiguration out = new org.bukkit.configuration.file.YamlConfiguration();

                    // Copy bedrock block
                    copySection(cfg, bedrockBase, out, "bedrock");
                    // Copy java block if present
                    if (cfg.getConfigurationSection(javaBase) != null) {
                        copySection(cfg, javaBase, out, "java");
                    }

                    // Save to file
                    String rel = key + ".yml";
                    java.io.File outFile = new java.io.File(formsDir, rel);
                    out.save(outFile);

                    // Rewrite main config entry to file-only
                    for (String child : cfg.getConfigurationSection(bedrockBase).getKeys(true)) {
                        cfg.set(bedrockBase + "." + child, null);
                    }
                    // Remove java sub-tree
                    cfg.set(javaBase, null);
                    // Set file property
                    cfg.set("forms." + key + ".file", rel);
                    converted++;
                }

                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Converted " + converted + " forms to external files. Backup saved as " + backupName);
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Conversion failed: " + e.getMessage());
                plugin.getLogger().warning("Form conversion error: " + e.getMessage());
            }
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

        if (arg.equalsIgnoreCase("openfor")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /bgui openfor <player_name> <menu_name> [arguments]");
                return true;
            }

            String playerName = args[1];
            String menuName = args[2];
            String[] menuArgs = Arrays.copyOfRange(args, 3, args.length);
            Player openPlayer = Bukkit.getPlayer(playerName);
            if (openPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
            PaperPlayer player1 = new PaperPlayer(openPlayer);

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
            commands.add("openfor");
            commands.add("convert");
            return commands.stream()
                    .filter(c -> c.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("open")) {
                return plugin.getFormMenuUtil().getFormMenus().keySet().stream()
                        .filter(c -> c.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("openfor")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("openfor")) {
            return plugin.getFormMenuUtil().getFormMenus().keySet().stream()
                    .filter(c -> c.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
