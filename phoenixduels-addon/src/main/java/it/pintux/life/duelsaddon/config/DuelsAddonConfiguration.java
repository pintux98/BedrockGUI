package it.pintux.life.duelsaddon.config;

import it.pintux.life.common.config.ConfigMigrator;
import it.pintux.life.duelsaddon.util.CommandAliases;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        plugin.getDataFolder().mkdirs();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(ConfigMigrator
                .of(plugin.getDataFolder(), FILE, () -> plugin.getResource(FILE),
                        plugin.getLogger()::info, plugin.getLogger()::warning)
                .migrate()
                .getFile());
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

    public CommandAliases commandAliases(String path, String... fallback) {
        if (!cfg.contains(path)) {
            return CommandAliases.of(fallback);
        }
        return CommandAliases.of(cfg.getStringList(path));
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
