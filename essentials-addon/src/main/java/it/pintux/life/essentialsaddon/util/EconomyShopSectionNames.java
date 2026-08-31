package it.pintux.life.essentialsaddon.util;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Reads the display names EconomyShopGUI configures for its sections.
 * <p>
 * {@link me.gypopo.economyshopgui.objects.shops.ShopSection#getSection()} only exposes the internal name, which is the
 * section file name, so the configured {@code item.displayname} and {@code title} are read straight from
 * {@code sections/} instead. Sub sections live in nested folders, hence the recursive walk.
 */
public final class EconomyShopSectionNames {

    public record Names(String menuName, String title, int slot) {
    }

    private EconomyShopSectionNames() {
    }

    public static Map<String, Names> load() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("EconomyShopGUI");
        if (plugin == null) {
            plugin = Bukkit.getPluginManager().getPlugin("EconomyShopGUI-Premium");
        }
        return plugin == null ? Map.of() : load(plugin.getDataFolder());
    }

    public static Map<String, Names> load(File dataFolder) {
        Map<String, Names> names = new HashMap<>();
        if (dataFolder != null) {
            collect(new File(dataFolder, "sections"), names);
        }
        return names;
    }

    private static void collect(File directory, Map<String, Names> names) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, names);
                continue;
            }
            String fileName = child.getName();
            if (!fileName.toLowerCase(Locale.ROOT).endsWith(".yml")) {
                continue;
            }
            YamlConfiguration section = YamlConfiguration.loadConfiguration(child);
            String id = fileName.substring(0, fileName.length() - ".yml".length()).toLowerCase(Locale.ROOT);
            names.put(id, new Names(menuName(section), section.getString("title"),
                    section.getInt("slot", Integer.MAX_VALUE)));
        }
    }

    private static String menuName(YamlConfiguration section) {
        for (String key : new String[]{"item.displayname", "item.display-name", "item.name"}) {
            String value = section.getString(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
