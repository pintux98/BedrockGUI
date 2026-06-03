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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * BedWars1058 upgrades provider.
 * <p>
 * BedWars1058's public {@code TeamUpgradesUtil} does not expose getMenuForArena (that is
 * BedWars2023-only), but the internal {@code UpgradesManager.getMenuForArena(IArena)} static method
 * returns the same API {@link UpgradesIndex}. We obtain the index via that one reflective call, then
 * use the public API ({@code getMenuContentBySlot}, {@code MenuContent.onClick(player, click, team)}).
 * BedWars1058's onClick takes 3 args (no booleans) and returns void.
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

        List<UpgradeContent> out = new ArrayList<>();
        // Flatten categories (e.g. "Traps") into their leaf children so they show as direct entries.
        for (MenuContent mc : flatten(new TreeMap<>(index.getMenuContentBySlot()).values())) {
            ItemStack icon = safeDisplay(mc, player, team);
            if (!isBuyable(icon)) continue;
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
        UpgradesIndex index = menuForArena(arena);
        if (index == null) return PurchaseResult.fail("provider unavailable");
        ITeam team = arena.getTeam(player);

        for (MenuContent mc : flatten(index.getMenuContentBySlot().values())) {
            if (mc.getName().equals(upgradeId)) {
                try {
                    mc.onClick(player, ClickType.LEFT, team); // BedWars handles money/messages
                    return PurchaseResult.ok();
                } catch (Exception e) {
                    logger.warning("BW1058 upgrade purchase failed for " + upgradeId + ": " + e.getClass().getSimpleName());
                    return PurchaseResult.fail("error");
                }
            }
        }
        return PurchaseResult.fail("not found");
    }

    private List<MenuContent> flatten(java.util.Collection<MenuContent> top) {
        List<MenuContent> out = new ArrayList<>();
        for (MenuContent mc : top) {
            List<MenuContent> children = childrenOf(mc);
            if (children != null && !children.isEmpty()) {
                out.addAll(flatten(children));
            } else {
                out.add(mc);
            }
        }
        return out;
    }

    /** Reads a category's private menuContentBySlot via reflection; null if not a category. */
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
            logger.warning("BW1058 upgrade category flatten failed: " + t.getClass().getSimpleName());
        }
        return null;
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

    private ItemStack safeDisplay(MenuContent mc, Player player, ITeam team) {
        try {
            return mc.getDisplayItem(player, team);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isBuyable(ItemStack icon) {
        if (icon == null || icon.getType().isAir()) return false;
        if (icon.getType().name().endsWith("GLASS_PANE")) return false;
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
