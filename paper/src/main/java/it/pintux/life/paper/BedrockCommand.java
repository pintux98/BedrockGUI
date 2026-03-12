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

    private static String convertLegacyPrefixedActionIfNeeded(String raw) {
        if (raw == null) {
            return null;
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return raw;
        }

        if (trimmed.startsWith("[") || trimmed.contains("{") || trimmed.contains(":")) {
            return raw;
        }

        String[] parts = trimmed.split("\\s+", 2);
        if (parts.length < 2) {
            return raw;
        }

        String type = parts[0].trim().toLowerCase();
        String value = parts[1].trim();
        if (value.isEmpty()) {
            return raw;
        }

        if ("openform".equals(type) || "open_form".equals(type)) {
            type = "open";
        }

        java.util.Set<String> supported = java.util.Set.of(
            "command",
            "open",
            "message",
            "server",
            "broadcast",
            "inventory",
            "sound",
            "economy",
            "title",
            "actionbar",
            "bungee",
            "delay",
            "conditional",
            "random"
        );

        if (!supported.contains(type)) {
            return raw;
        }

        if ("open".equals(type)) {
            java.util.List<String> tokens = tokenizeByWhitespace(value);
            if (tokens.isEmpty()) {
                return raw;
            }
            StringBuilder b = new StringBuilder();
            b.append("open {");
            for (String token : tokens) {
                b.append(" - \"").append(escapeActionValue(token)).append("\"");
            }
            b.append(" }");
            return b.toString();
        }

        return type + " { - \"" + escapeActionValue(value) + "\" }";
    }

    private static String escapeActionValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static java.util.List<String> tokenizeByWhitespace(String input) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (input == null) {
            return out;
        }
        String s = input.trim();
        if (s.isEmpty()) {
            return out;
        }
        for (String part : s.split("\\s+")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
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
                if ("onClick".equalsIgnoreCase(key)) {
                    if (val instanceof java.util.List) {
                        java.util.List<?> list = (java.util.List<?>) val;
                        java.util.List<String> flattened = new java.util.ArrayList<>();
                        for (Object item : list) {
                            if (item instanceof String) {
                                String s = (String) item;
                                String noNewlines = s.replaceAll("\\r?\\n", " ");
                                String normalized = noNewlines.replaceAll("-\\s*\\|", "- ");
                                normalized = normalized.replaceAll("\\s+", " ").trim();
                                flattened.add(convertLegacyPrefixedActionIfNeeded(normalized));
                            }
                        }
                        dest.set(destKey, flattened);
                    } else if (val instanceof String) {
                        dest.set(destKey, convertLegacyPrefixedActionIfNeeded((String) val));
                    } else {
                        dest.set(destKey, val);
                    }
                } else if ("action".equalsIgnoreCase(key) && val instanceof String) {
                    dest.set(destKey, convertLegacyPrefixedActionIfNeeded((String) val));
                } else if ("global_actions".equalsIgnoreCase(key)) {
                    if (val instanceof java.util.List) {
                        java.util.List<?> list = (java.util.List<?>) val;
                        java.util.List<String> converted = new java.util.ArrayList<>();
                        for (Object item : list) {
                            if (item instanceof String) {
                                converted.add(convertLegacyPrefixedActionIfNeeded(((String) item).trim()));
                            }
                        }
                        dest.set(destKey, converted);
                    } else if (val instanceof String) {
                        dest.set(destKey, java.util.List.of(convertLegacyPrefixedActionIfNeeded(((String) val).trim())));
                    } else {
                        dest.set(destKey, val);
                    }
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
            if(player.hasPermission("bedrockgui.admin")){
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_RELOAD, null, null));
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPENFOR, null, null));
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_CONVERT, null, null));
            }
            sender.sendMessage(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPEN, null, null));
            return true;
        }
        String arg = args[0];
        if (arg.equalsIgnoreCase("reload")) {
            if (player != null && !player.hasPermission("bedrockgui.admin")) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null));
                return true;
            }
            plugin.reloadData();
            sender.sendMessage(plugin.getMessageData().getValue(MessageData.COMMAND_RELOAD_SUCCESS, null, null));
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
                org.bukkit.configuration.ConfigurationSection menuSec = cfg.getConfigurationSection("menu");

                java.util.List<String> rootsToConvert = new java.util.ArrayList<>();
                boolean hasInlineForms = formsSec != null && !formsSec.getKeys(false).isEmpty();
                boolean hasLegacyMenu = menuSec != null && !menuSec.getKeys(false).isEmpty();

                if (hasInlineForms) {
                    rootsToConvert.add("forms");
                }
                if (hasLegacyMenu) {
                    rootsToConvert.add("menu");
                }

                if (rootsToConvert.isEmpty()) {
                    sender.sendMessage(plugin.getMessageData().getValue(MessageData.FORMS_NO_INLINE_FORMS, null, null));
                    return true;
                }

                int converted = 0;
                java.util.Set<String> processedKeys = new java.util.HashSet<>();
                for (String root : rootsToConvert) {
                    org.bukkit.configuration.ConfigurationSection rootSec = cfg.getConfigurationSection(root);
                    if (rootSec == null) {
                        continue;
                    }
                    for (String key : rootSec.getKeys(false)) {
                        if (!processedKeys.add(key)) {
                            continue;
                        }

                        String existingFile = cfg.getString("forms." + key + ".file");
                        if (existingFile != null && !existingFile.trim().isEmpty()) {
                            continue;
                        }

                        String bedrockBase = root + "." + key;
                        String javaBase = bedrockBase + ".java";

                        org.bukkit.configuration.file.YamlConfiguration out = new org.bukkit.configuration.file.YamlConfiguration();

                        copySection(cfg, bedrockBase, out, "bedrock");
                        if (cfg.getConfigurationSection(javaBase) != null) {
                            copySection(cfg, javaBase, out, "java");
                        }

                        String rel = key + ".yml";
                        java.io.File outFile = new java.io.File(formsDir, rel);
                        out.save(outFile);

                        if ("menu".equals(root)) {
                            cfg.set(bedrockBase, null);
                        } else {
                            if (cfg.getConfigurationSection(bedrockBase) != null) {
                                for (String child : cfg.getConfigurationSection(bedrockBase).getKeys(true)) {
                                    cfg.set(bedrockBase + "." + child, null);
                                }
                            }
                        }

                        cfg.set(javaBase, null);
                        cfg.set("forms." + key + ".file", rel);
                        converted++;
                    }
                }

                if (hasLegacyMenu) {
                    cfg.set("menu", null);
                }

                plugin.saveConfig();
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.FORMS_CONVERSION_SUCCESS, java.util.Map.of("count", converted, "backup", backupName), null));
            } catch (Exception e) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.FORMS_CONVERSION_FAILED, java.util.Map.of("error", e.getMessage()), null));
                plugin.getLogger().warning("Form conversion error: " + e.getMessage());
            }
            return true;
        }

        if (arg.equalsIgnoreCase("open")) {
            if (player == null) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.COMMAND_PLAYER_ONLY, null, null));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPEN, null, null));
                return true;
            }

            String menuName = args[1];
            String[] menuArgs = Arrays.copyOfRange(args, 2, args.length);
            PaperPlayer player1 = new PaperPlayer(player);

            plugin.getApi().openMenu(player1, menuName, menuArgs);
        }

        if (arg.equalsIgnoreCase("openfor")) {
            if (player != null && !player.hasPermission("bedrockgui.openfor")) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.NO_PEX, null, null));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.COMMAND_USAGE_OPENFOR, null, null));
                return true;
            }

            String playerName = args[1];
            String menuName = args[2];
            String[] menuArgs = Arrays.copyOfRange(args, 3, args.length);
            Player openPlayer = Bukkit.getPlayerExact(playerName);
            if (openPlayer == null) {
                sender.sendMessage(plugin.getMessageData().getValue(MessageData.PLAYER_NOT_FOUND, java.util.Map.of("player", playerName), null));
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
