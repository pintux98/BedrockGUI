package it.pintux.life.bedwarsaddon.provider;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.upgrades.MenuContent;
import com.andrei1058.bedwars.api.upgrades.UpgradesIndex;
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
 * BedWars1058 upgrades provider — mirrors the BedWars2023 one (fork lineage).
 * <p>
 * The index is obtained via the internal static {@code UpgradesManager.getMenuForArena(IArena)}
 * (not exposed on the public API). Category children (e.g. "Traps") are read from the private
 * {@code menuContentBySlot} map; next-tier cost from MenuUpgrade's private {@code tiers} list.
 * BedWars1058's {@code MenuContent.onClick(player, click, team)} takes 3 args and returns void —
 * BedWars handles money/messages internally.
 */
public final class BedWars1058UpgradeProvider implements UpgradeProvider {
    private static final String UPGRADES_MANAGER = "com.andrei1058.bedwars.upgrades.UpgradesManager";

    private final Logger logger;
    private final BedWars1058ApiAccess access;

    public BedWars1058UpgradeProvider(Logger logger, BedWars1058ApiAccess access) {
        this.logger = logger;
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars1058"; }

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
        UpgradesIndex index = menuForArena(arena);
        if (index == null) return List.of();
        ITeam team = arena.getTeam(player);

        List<MenuContent> leaves = buyableLeaves(index);
        List<UpgradeContent> out = new ArrayList<>();
        for (int i = 0; i < leaves.size(); i++) {
            MenuContent mc = leaves.get(i);
            String display = displayName(safeDisplay(mc, player, team), mc.getName());
            Cost cost = upgradeCost(api, player, team, mc);
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
        UpgradesIndex index = menuForArena(arena);
        if (index == null) return PurchaseResult.fail("provider unavailable");
        ITeam team = arena.getTeam(player);

        MenuContent target = resolve(index, upgradeId);
        if (target == null) return PurchaseResult.fail("not found");

        try {
            target.onClick(player, ClickType.LEFT, team); // BedWars handles money/messages
            return PurchaseResult.ok();
        } catch (Exception e) {
            logger.warning("BW1058 upgrade purchase failed for " + upgradeId + ": " + e.getClass().getSimpleName());
            return PurchaseResult.fail("error");
        }
    }

    /** Reflectively call UpgradesManager.getMenuForArena (not exposed on the public API). */
    private UpgradesIndex menuForArena(IArena arena) {
        try {
            Class<?> mgr = Class.forName(UPGRADES_MANAGER);
            Object result = mgr.getMethod("getMenuForArena", IArena.class).invoke(null, arena);
            return result instanceof UpgradesIndex ? (UpgradesIndex) result : null;
        } catch (Throwable t) {
            logger.warning("BW1058 getMenuForArena reflection failed: " + t.getClass().getSimpleName());
            return null;
        }
    }

    // --- leaf enumeration (same approach as the BedWars2023 provider) ---------

    private List<MenuContent> buyableLeaves(UpgradesIndex index) {
        List<MenuContent> out = new ArrayList<>();
        collect(new TreeMap<>(index.getMenuContentBySlot()).values(), false, out);
        return out;
    }

    private void collect(Collection<MenuContent> contents, boolean fromCategory, List<MenuContent> out) {
        for (MenuContent mc : contents) {
            List<MenuContent> children = childrenOf(mc);
            if (children != null && !children.isEmpty()) {
                collect(children, true, out);
                continue;
            }
            String cls = mc.getClass().getSimpleName();
            if (cls.equals("MenuUpgrade") || cls.equals("MenuBaseTrap")) {
                out.add(mc);
            } else if (!fromCategory && !cls.equals("MenuTrapSlot") && !cls.equals("MenuSeparator")) {
                out.add(mc);
            }
        }
    }

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
            logger.warning("BW1058 upgrade category flatten failed: " + t.getClass().getSimpleName());
        }
        return null;
    }

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

    // --- cost ------------------------------------------------------------------

    private record Cost(int cost, String currency, boolean affordable, boolean maxed) {
        static Cost plain() { return new Cost(0, "", true, false); }
        static Cost maxedOut() { return new Cost(0, "", true, true); }
    }

    private Cost upgradeCost(BedWars api, Player player, ITeam team, MenuContent mc) {
        if (team == null || !mc.getClass().getSimpleName().equals("MenuUpgrade")) return Cost.plain();
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
                return new Cost(cost, "", true, false);
            }
            boolean affordable = api.getShopUtil().calculateMoney(player, currency) >= cost;
            return new Cost(cost, currencyName(currency), affordable, false);
        } catch (Throwable t) {
            return Cost.plain();
        }
    }

    // --- display helpers ---------------------------------------------------------

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
