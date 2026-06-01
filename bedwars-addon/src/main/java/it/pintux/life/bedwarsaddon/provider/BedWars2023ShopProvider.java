package it.pintux.life.bedwarsaddon.provider;

import com.tomkeuper.bedwars.api.BedWars;
import com.tomkeuper.bedwars.api.arena.shop.ICategoryContent;
import com.tomkeuper.bedwars.api.arena.shop.IContentTier;
import com.tomkeuper.bedwars.api.shop.IShopCache;
import com.tomkeuper.bedwars.api.shop.IShopCategory;
import com.tomkeuper.bedwars.api.shop.IShopIndex;
import it.pintux.life.bedwarsaddon.api.ShopProvider;
import it.pintux.life.bedwarsaddon.model.PurchaseResult;
import it.pintux.life.bedwarsaddon.model.ShopCategory;
import it.pintux.life.bedwarsaddon.model.ShopContent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * BedWars2023 implementation of {@link ShopProvider}. The ONLY class touching com.tomkeuper.* types.
 * <p>
 * Tier/price selection mirrors {@code CategoryContent.execute}: tiers are 1-based via
 * {@code IShopCache.getContentTier(id)} (returns 1 when unowned); the displayed/purchasable tier is
 * {@code tiers.get(0)} when the item is not yet cached, {@code tiers.get(currentTier)} otherwise,
 * capped at the max tier. Purchasing delegates to {@code execute(player, cache, slot)} which performs
 * the affordability check, currency deduction and item grant internally and returns success.
 */
public final class BedWars2023ShopProvider implements ShopProvider {
    private final Logger logger;
    private final BedWarsApiAccess access;

    public BedWars2023ShopProvider(Logger logger, BedWarsApiAccess access) {
        this.logger = logger;
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars2023"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    private IShopIndex shopIndex() {
        BedWars api = access.get();
        return api == null ? null : api.getShopUtil().getShopManager().getShop();
    }

    private IShopCache playerCache(BedWars api, Player player) {
        return api.getShopUtil().getShopCache().getShopCache(player.getUniqueId());
    }

    @Override
    public List<ShopCategory> getCategories(Player player) {
        IShopIndex index = shopIndex();
        if (index == null) return List.of();
        List<ShopCategory> out = new ArrayList<>();
        for (IShopCategory cat : index.getCategoryList()) {
            // The category name is its stable identifier in BedWars2023.
            out.add(new ShopCategory(cat.getName(), ChatColor.stripColor(color(cat.getName())), cat.getSlot()));
        }
        return out;
    }

    @Override
    public List<ShopContent> getCategoryContents(Player player, String categoryId) {
        IShopIndex index = shopIndex();
        BedWars api = access.get();
        if (index == null || api == null) return List.of();

        IShopCategory category = findCategory(index, categoryId);
        if (category == null) return List.of();

        IShopCache cache = playerCache(api, player);
        List<ShopContent> out = new ArrayList<>();
        for (ICategoryContent content : category.getCategoryContentList()) {
            List<IContentTier> tiers = content.getContentTiers();
            if (tiers == null || tiers.isEmpty()) continue;

            IContentTier ct = selectTier(content, cache, tiers);
            int price = ct.getPrice();
            Material currency = ct.getCurrency();
            int money = api.getShopUtil().calculateMoney(player, currency);
            boolean affordable = money >= price;

            int currentTier = cache.getContentTier(content.getIdentifier());
            String tierLabel = tiers.size() > 1 ? api.getShopUtil().getRomanNumber(currentTier) : "";

            out.add(new ShopContent(content.getIdentifier(), displayName(content, player),
                    price, currencyName(currency), affordable, tierLabel));
        }
        return out;
    }

    @Override
    public PurchaseResult purchase(Player player, String contentId) {
        IShopIndex index = shopIndex();
        BedWars api = access.get();
        if (index == null || api == null) return PurchaseResult.fail("provider unavailable");

        IShopCache cache = playerCache(api, player);
        for (IShopCategory category : index.getCategoryList()) {
            ICategoryContent content = category.getCategoryContent(contentId, index);
            if (content != null) {
                try {
                    // execute() runs the affordability check, deduction and item grant; the int is the
                    // UI slot (used only for ShopBuyEvent). Returns false on denial/insufficient funds.
                    boolean ok = content.execute(player, cache, content.getSlot());
                    return ok ? PurchaseResult.ok() : PurchaseResult.fail("denied");
                } catch (Exception e) {
                    logger.warning("Purchase failed for " + contentId + ": " + e.getClass().getSimpleName());
                    return PurchaseResult.fail("error");
                }
            }
        }
        return PurchaseResult.fail("item not found");
    }

    @Override
    public boolean ownsInventory(Inventory inventory) {
        // Shop interception is event-based (ShopOpenEvent); inventory ownership is unused for the shop.
        return false;
    }

    // --- helpers -----------------------------------------------------------

    private IShopCategory findCategory(IShopIndex index, String categoryId) {
        for (IShopCategory c : index.getCategoryList()) {
            if (c.getName().equals(categoryId)) return c;
        }
        return null;
    }

    /** Mirrors CategoryContent.execute tier selection. */
    private IContentTier selectTier(ICategoryContent content, IShopCache cache, List<IContentTier> tiers) {
        int currentTier = cache.getContentTier(content.getIdentifier()); // 1-based, 1 when unowned
        if (currentTier == tiers.size()) {
            return tiers.get(currentTier - 1);
        }
        if (!cache.hasCachedItem(content)) {
            return tiers.get(0);
        }
        return tiers.get(currentTier);
    }

    private String displayName(ICategoryContent content, Player player) {
        try {
            ItemStack item = content.getItemStack(player);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                return ChatColor.stripColor(item.getItemMeta().getDisplayName());
            }
        } catch (Exception ignored) {
        }
        return content.getIdentifier();
    }

    private static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
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
