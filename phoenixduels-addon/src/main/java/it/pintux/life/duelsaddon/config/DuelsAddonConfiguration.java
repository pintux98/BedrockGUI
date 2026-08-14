package it.pintux.life.duelsaddon.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public final class DuelsAddonConfiguration {
    public static final String FILE = "config.yml";

    private final YamlConfiguration cfg;

    private DuelsAddonConfiguration(YamlConfiguration cfg) {
        this.cfg = cfg;
    }

    public static DuelsAddonConfiguration load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE);
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource(FILE, false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(FILE)) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                cfg.setDefaults(defaults);
                cfg.options().copyDefaults(true);
            }
        } catch (IOException ignored) {
        }
        return new DuelsAddonConfiguration(cfg);
    }

    public String text(String path) {
        return color(cfg.getString(path, path));
    }

    public String text(String path, String def) {
        return color(cfg.getString(path, def));
    }

    public String raw(String path, String def) {
        return cfg.getString(path, def);
    }

    public boolean flag(String path, boolean def) {
        return cfg.getBoolean(path, def);
    }

    public int number(String path, int def) {
        return cfg.getInt(path, def);
    }

    public String render(String path, Map<String, String> placeholders) {
        return applyPlaceholders(text(path), placeholders);
    }

    public String apply(String template, Map<String, String> placeholders) {
        return applyPlaceholders(template, placeholders);
    }

    private static String applyPlaceholders(String template, Map<String, String> placeholders) {
        if (template == null) {
            return "";
        }
        String out = template;
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                out = out.replace("%" + e.getKey().toLowerCase(Locale.ROOT) + "%", e.getValue());
            }
        }
        return out;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public int itemsPerPage() {
        return Math.max(1, number("general.items-per-page", 18));
    }

    public int defaultRounds() {
        return Math.max(1, number("general.default-rounds", 1));
    }

    public boolean integratedGuiEnabled() {
        return flag("integrated-gui", true);
    }

    public boolean registerActionsEnabled() {
        return flag("register-actions", true);
    }

    public boolean menuEnabled(String group) {
        return flag("menus." + group, true);
    }

    public boolean partyInviteFormsEnabled() {
        return flag("invitations.party-forms", true);
    }

    public boolean duelInviteFormsEnabled() {
        return flag("invitations.duel-forms", true);
    }
}
