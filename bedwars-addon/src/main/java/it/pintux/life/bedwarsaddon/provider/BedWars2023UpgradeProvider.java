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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * BedWars2023 implementation of {@link UpgradeProvider}. The ONLY upgrade class touching com.tomkeuper.*.
 * <p>
 * Purchasing calls {@code MenuContent.onClick(player, LEFT, team, forFree=false, announce=true,
 * announceUnlocked=true, openInv=false)}. The plugin takes money, applies the upgrade and sends its
 * own messages; openInv=false skips the chest-GUI refresh (which would otherwise index the open
 * inventory and throw, since the Bedrock player only has the Cumulus form).
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

        List<UpgradeContent> out = new ArrayList<>();
        // Flatten categories (e.g. the "Traps" category opens a sub-menu) so their children become
        // direct buyable entries in the flat form. Ordered by slot to mirror the chest layout.
        for (MenuContent mc : flatten(new TreeMap<>(index.getMenuContentBySlot()).values())) {
            ItemStack icon = safeDisplay(mc, player, team);
            if (!isBuyable(icon)) continue; // skip separators / decorative fillers
            out.add(new UpgradeContent(mc.getName(), displayName(icon, mc.getName())));
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

        MenuContent target = null;
        for (MenuContent mc : flatten(index.getMenuContentBySlot().values())) {
            if (mc.getName().equals(upgradeId)) { target = mc; break; }
        }
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

    /**
     * Flattens MenuContent categories into their leaf children. A category (e.g. "Traps") stores its
     * children in a private {@code menuContentBySlot} map and opens a sub-chest on click; we read that
     * map reflectively so the children show as direct buyable entries. Leaves are returned as-is.
     */
    private List<MenuContent> flatten(java.util.Collection<MenuContent> top) {
        List<MenuContent> out = new ArrayList<>();
        for (MenuContent mc : top) {
            List<MenuContent> children = childrenOf(mc);
            if (children != null && !children.isEmpty()) {
                out.addAll(flatten(children)); // category -> recurse into its children
            } else {
                out.add(mc); // leaf (a real upgrade/trap)
            }
        }
        return out;
    }

    /** Reads a category's private menuContentBySlot via reflection; null if this is not a category. */
    private List<MenuContent> childrenOf(MenuContent mc) {
        try {
            java.lang.reflect.Field f = mc.getClass().getDeclaredField("menuContentBySlot");
            f.setAccessible(true);
            Object map = f.get(mc);
            if (map instanceof Map<?, ?> m) {
                List<MenuContent> out = new ArrayList<>();
                for (Object v : m.values()) {
                    if (v instanceof MenuContent child) out.add(child);
                }
                return out;
            }
        } catch (NoSuchFieldException notACategory) {
            return null;
        } catch (Throwable t) {
            logger.warning("Upgrade category flatten failed: " + t.getClass().getSimpleName());
        }
        return null;
    }

    private ItemStack safeDisplay(MenuContent mc, Player player, ITeam team) {
        try {
            return mc.getDisplayItem(player, team);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isBuyable(ItemStack icon) {
        if (icon == null || icon.getType().isAir()) return false;
        if (icon.getType().name().endsWith("GLASS_PANE")) return false; // separators
        return icon.hasItemMeta() && icon.getItemMeta().hasDisplayName();
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
}
