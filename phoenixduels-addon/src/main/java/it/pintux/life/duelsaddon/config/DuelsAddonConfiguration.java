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

/**
 * This addon's own {@code config.yml}, holding the feature toggles and every string the Bedrock
 * forms display.
 *
 * <p>The shipped resource is layered underneath the file on disk as defaults, so a config written
 * by an older version keeps working and newly added keys resolve without the admin editing
 * anything.</p>
 */
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
                writeMissingKeys(plugin, cfg, defaults, file);
            }
        } catch (IOException ignored) {
        }
        return new DuelsAddonConfiguration(cfg);
    }

    /**
     * Persists keys that exist in the shipped defaults but not in the file on disk.
     *
     * <p>{@code copyDefaults} only affects the in-memory view, so a key added in a new release stays
     * invisible in the admin's file and cannot be edited. That is how {@code debug} shipped without
     * ever appearing in a live config.</p>
     */
    private static void writeMissingKeys(JavaPlugin plugin, YamlConfiguration cfg,
                                         YamlConfiguration defaults, File file) {
        int added = 0;
        for (String key : defaults.getKeys(true)) {
            if (defaults.isConfigurationSection(key) || cfg.contains(key, true)) {
                continue;
            }
            cfg.set(key, defaults.get(key));
            added++;
        }
        if (added == 0) {
            return;
        }
        try {
            cfg.save(file);
            plugin.getLogger().info("Added " + added + " new config key(s) to " + FILE + ".");
        } catch (IOException e) {
            plugin.getLogger().warning("Could not write new config keys to " + FILE + ": " + e.getMessage());
        }
    }

    public String text(String path) {
        return color(cfg.getString(path, path));
    }

    public String text(String path, String def) {
        return color(cfg.getString(path, def));
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

    /**
     * @return whether to log why a PhoenixDuels menu was not intercepted, which is the only way to
     *         tell a menu-key mismatch apart from a player who simply is not on Bedrock
     */
    public boolean debugEnabled() {
        return flag("debug", false);
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
