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
        // Ordered by slot so the form mirrors the chest layout.
        for (Map.Entry<Integer, MenuContent> entry : new TreeMap<>(index.getMenuContentBySlot()).entrySet()) {
            MenuContent mc = entry.getValue();
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
        for (MenuContent mc : index.getMenuContentBySlot().values()) {
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
