package it.pintux.life.bedwarsaddon.provider;

import com.tomkeuper.bedwars.api.BedWars;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.upgrades.MenuContent;
import com.tomkeuper.bedwars.api.upgrades.UpgradesIndex;
import it.pintux.life.bedwarsaddon.api.UpgradeProvider;
import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.UpgradeContent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * BedWars2023 implementation of {@link UpgradeProvider}. The ONLY upgrade class touching com.tomkeuper.*.
 * <p>
 * Buyable leaves are MenuUpgrade entries (top level) and MenuUpgrade/MenuBaseTrap entries nested inside
 * categories (e.g. the "Traps" category); category children are read from the private
 * {@code menuContentBySlot} map via reflection. Decorative entries (trap-queue slots, separators,
 * back arrows inside sub-menus) are excluded.
 * <p>
 * Cost of the NEXT tier comes from MenuUpgrade's private {@code tiers} list
 * (mirroring its onClick: {@code tiers.get(team.getTeamUpgradeTiers().get(name) + 1)}), with
 * affordability via {@code ShopUtil.calculateMoney}. Purchasing calls
 * {@code onClick(player, LEFT, team, forFree=false, announce=true, announceUnlocked=true, openInv=false)}
 * — openInv=false skips the chest refresh that would otherwise throw with no chest open.
 */
public final class BedWars2023UpgradeProvider implements UpgradeProvider {
    private final Logger logger;
    private final BedWarsApiAccess access;

    public BedWars2023UpgradeProvider(Logger logger, BedWarsApiAccess access) {
        this.logger = logger;
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars2023"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    @Override
    public boolean isWatchingUpgradeGui(Player player) {
        BedWars api = access.get();
        return api != null && api.getTeamUpgradesUtil().isWatchingGUI(player);
    }

    @Override
    public void stopWatching(Player player) {
        BedWars api = access.get();
        if (api != null) {
            api.getTeamUpgradesUtil().removeWatchingUpgrades(player.getUniqueId());
        }
    }

    @Override
    public List<UpgradeContent> getUpgrades(Player player) {
        BedWars api = access.get();
        if (api == null) return List.of();
        IArena arena = api.getArenaUtil().getArenaByPlayer(player);
        if (arena == null) return List.of();
        UpgradesIndex index = api.getTeamUpgradesUtil().getMenuForArena(arena);
        if (index == null) return List.of();
        ITeam team = arena.getTeam(player);

        List<MenuContent> leaves = buyableLeaves(index);
        List<UpgradeContent> out = new ArrayList<>();
        for (int i = 0; i < leaves.size(); i++) {
            MenuContent mc = leaves.get(i);
            String display = displayName(safeDisplay(mc, player, team), mc.getName());
            Cost cost = upgradeCost(api, player, team, mc);
            // index-qualified id: survives duplicate names across flattened categories
            out.add(new UpgradeContent(i + "|" + mc.getName(), display,
                    cost.cost, cost.currency, cost.affordable, cost.maxed));
        }
        return out;
    }

    @Override
    public PurchaseResult purchase(Player player, String upgradeId) {
        BedWars api = access.get();
        if (api == null) return PurchaseResult.fail("provider unavailable");
        IArena arena = api.getArenaUtil().getArenaByPlayer(player);
        if (arena == null) return PurchaseResult.fail("not in game");
        UpgradesIndex index = api.getTeamUpgradesUtil().getMenuForArena(arena);
        if (index == null) return PurchaseResult.fail("provider unavailable");
        ITeam team = arena.getTeam(player);

        MenuContent target = resolve(index, upgradeId);
        if (target == null) return PurchaseResult.fail("not found");

        try {
            // openInv=false: skip the chest refresh (no open chest in the form flow).
            boolean ok = target.onClick(player, ClickType.LEFT, team, false, true, true, false);
            return ok ? PurchaseResult.ok() : PurchaseResult.fail("denied");
        } catch (Exception e) {
            logger.warning("Upgrade purchase failed for " + upgradeId + ": " + e.getClass().getSimpleName());
            return PurchaseResult.fail("error");
        }
    }

    // --- leaf enumeration ---------------------------------------------------

    /** Buyable leaves in deterministic slot order (top level, categories flattened). */
    private List<MenuContent> buyableLeaves(UpgradesIndex index) {
        List<MenuContent> out = new ArrayList<>();
        collect(new TreeMap<>(index.getMenuContentBySlot()).values(), false, out);
        return out;
    }

    private void collect(Collection<MenuContent> contents, boolean fromCategory, List<MenuContent> out) {
        for (MenuContent mc : contents) {
            List<MenuContent> children = childrenOf(mc);
            if (children != null && !children.isEmpty()) {
                collect(children, true, out); // category -> recurse into its children
                continue;
            }
            String cls = mc.getClass().getSimpleName();
            if (cls.equals("MenuUpgrade") || cls.equals("MenuBaseTrap")) {
                out.add(mc);
            } else if (!fromCategory && !cls.equals("MenuTrapSlot") && !cls.equals("MenuSeparator")) {
                out.add(mc); // unknown custom top-level content: keep
            }
            // unknown CATEGORY children (back arrows, fillers) are skipped
        }
    }

    /** Reads a category's private menuContentBySlot via reflection (slot-sorted); null if not a category. */
    private List<MenuContent> childrenOf(MenuContent mc) {
        try {
            Field f = mc.getClass().getDeclaredField("menuContentBySlot");
            f.setAccessible(true);
            Object map = f.get(mc);
            if (map instanceof Map<?, ?> m) {
                TreeMap<Integer, MenuContent> sorted = new TreeMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (e.getKey() instanceof Integer slot && e.getValue() instanceof MenuContent child) {
                        sorted.put(slot, child);
                    }
                }
                return new ArrayList<>(sorted.values());
            }
        } catch (NoSuchFieldException notACategory) {
            return null;
        } catch (Throwable t) {
            logger.warning("Upgrade category flatten failed: " + t.getClass().getSimpleName());
        }
        return null;
    }

    /** Resolve an index-qualified id ("i|name"), falling back to a name search. */
    private MenuContent resolve(UpgradesIndex index, String id) {
        List<MenuContent> leaves = buyableLeaves(index);
        int sep = id.indexOf('|');
        if (sep > 0) {
            try {
                int idx = Integer.parseInt(id.substring(0, sep));
                String name = id.substring(sep + 1);
                if (idx >= 0 && idx < leaves.size() && leaves.get(idx).getName().equals(name)) {
                    return leaves.get(idx);
                }
                for (MenuContent mc : leaves) {
                    if (mc.getName().equals(name)) return mc;
                }
                return null;
            } catch (NumberFormatException ignored) {
            }
        }
        for (MenuContent mc : leaves) {
            if (mc.getName().equals(id)) return mc;
        }
        return null;
    }

    // --- cost ----------------------------------------------------------------

    private record Cost(int cost, String currency, boolean affordable, boolean maxed) {
        static Cost plain() { return new Cost(0, "", true, false); }
        static Cost maxedOut() { return new Cost(0, "", true, true); }
    }

    /**
     * Next-tier cost for MenuUpgrade entries, mirroring MenuUpgrade.onClick: the team's current tier
     * comes from {@code team.getTeamUpgradeTiers()}, the next tier from the private {@code tiers} list.
     */
    private Cost upgradeCost(BedWars api, Player player, ITeam team, MenuContent mc) {
        if (team == null) return Cost.plain();
        String cls = mc.getClass().getSimpleName();
        if (cls.equals("MenuBaseTrap")) return trapCost(api, player, team, mc);
        if (!cls.equals("MenuUpgrade")) return Cost.plain();
        try {
            Field f = mc.getClass().getDeclaredField("tiers");
            f.setAccessible(true);
            List<?> tiers = (List<?>) f.get(mc);
            if (tiers == null || tiers.isEmpty()) return Cost.plain();

            int tier = -1;
            Map<String, Integer> teamTiers = team.getTeamUpgradeTiers();
            if (teamTiers != null && teamTiers.containsKey(mc.getName())) {
                tier = teamTiers.get(mc.getName());
            }
            if (tier + 1 >= tiers.size()) return Cost.maxedOut();

            Object next = tiers.get(tier + 1);
            var costMethod = next.getClass().getDeclaredMethod("getCost");
            costMethod.setAccessible(true);
            int cost = (int) costMethod.invoke(next);
            var currencyMethod = next.getClass().getDeclaredMethod("getCurrency");
            currencyMethod.setAccessible(true);
            Object cur = currencyMethod.invoke(next);
            if (!(cur instanceof Material currency) || currency == Material.AIR) {
                return new Cost(cost, "", true, false); // vault/unknown currency: show cost, no afford check
            }
            boolean affordable = api.getShopUtil().calculateMoney(player, currency) >= cost;
            return new Cost(cost, currencyName(currency), affordable, false);
        } catch (Throwable t) {
            return Cost.plain();
        }
    }

    /**
     * Trap price, mirroring MenuBaseTrap.onClick: fixed {@code cost}/{@code currency} fields when set,
     * otherwise dynamic {@code trap-start-price + activeTraps * trap-increment-price} from the
     * upgrades configuration (group-keyed), currency falling back to {@code trap-currency}.
     */
    private Cost trapCost(BedWars api, Player player, ITeam team, MenuContent mc) {
        try {
            Field costField = mc.getClass().getDeclaredField("cost");
            costField.setAccessible(true);
            int cost = costField.getInt(mc);
            Field currencyField = mc.getClass().getDeclaredField("currency");
            currencyField.setAccessible(true);
            Material currency = currencyField.get(mc) instanceof Material m ? m : null;

            String group = team.getArena() != null && team.getArena().getGroup() != null
                    ? team.getArena().getGroup().toLowerCase() : "default";
            Object yml = upgradesYml("com.tomkeuper.bedwars.BedWars");
            if (yml instanceof org.bukkit.configuration.ConfigurationSection cfg) {
                if (cost == 0) {
                    int start = cfg.getInt(group + "-upgrades-settings.trap-start-price");
                    int increment = cfg.getInt(group + "-upgrades-settings.trap-increment-price");
                    int active = team.getActiveTraps() != null ? team.getActiveTraps().size() : 0;
                    cost = start + active * increment;
                }
                if (currency == null) {
                    String currencyName = cfg.getString(group + "-upgrades-settings.trap-currency");
                    if (currencyName != null) currency = api.getShopUtil().getCurrency(currencyName);
                }
            }
            if (cost <= 0) return Cost.plain();
            if (currency == null || currency == Material.AIR) return new Cost(cost, "", true, false);
            boolean affordable = api.getShopUtil().calculateMoney(player, currency) >= cost;
            return new Cost(cost, currencyName(currency), affordable, false);
        } catch (Throwable t) {
            return Cost.plain();
        }
    }

    /** Reflectively reads the internal upgrades YamlConfiguration: BedWars.getUpgradeManager().getConfiguration().getYml(). */
    private Object upgradesYml(String mainClass) {
        try {
            Class<?> main = Class.forName(mainClass);
            Object manager = main.getMethod("getUpgradeManager").invoke(null);
            Object configuration = manager.getClass().getMethod("getConfiguration").invoke(manager);
            return configuration.getClass().getMethod("getYml").invoke(configuration);
        } catch (Throwable t) {
            return null;
        }
    }

    // --- display helpers -------------------------------------------------------

    private ItemStack safeDisplay(MenuContent mc, Player player, ITeam team) {
        try {
            return mc.getDisplayItem(player, team);
        } catch (Exception e) {
            return null;
        }
    }

    private String displayName(ItemStack icon, String fallback) {
        try {
            if (icon != null && icon.hasItemMeta() && icon.getItemMeta().hasDisplayName()) {
                return ChatColor.stripColor(icon.getItemMeta().getDisplayName());
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static String currencyName(Material currency) {
        if (currency == null) return "";
        switch (currency) {
            case IRON_INGOT: return "Iron";
            case GOLD_INGOT: return "Gold";
            case DIAMOND: return "Diamond";
            case EMERALD: return "Emerald";
            default:
                String n = currency.name().toLowerCase().replace('_', ' ');
                return Character.toUpperCase(n.charAt(0)) + n.substring(1);
        }
    }
}
