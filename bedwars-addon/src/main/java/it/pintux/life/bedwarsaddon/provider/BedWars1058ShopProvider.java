package it.pintux.life.bedwarsaddon.provider;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.shop.ICategoryContent;
import com.andrei1058.bedwars.api.arena.shop.IContentTier;
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
import java.util.UUID;
import java.util.logging.Logger;

/**
 * BedWars1058 shop provider — REFLECTION-BASED and EXPERIMENTAL.
 * <p>
 * BedWars1058's public API exposes neither a shop manager/index nor a buy method (those are
 * BedWars2023-only additions). We therefore reach into BedWars1058 internals via reflection:
 * <ul>
 *   <li>{@code ShopManager.getShop()} (static) → internal {@code ShopIndex}</li>
 *   <li>{@code ShopIndex.getCategoryList()} → internal {@code ShopCategory} list</li>
 *   <li>{@code ShopCategory.getCategoryContentList()} → {@code List<ICategoryContent>} (public API type)</li>
 *   <li>{@code ShopCache.getShopCache(UUID)} (static) / {@code new ShopCache(UUID)}</li>
 *   <li>{@code CategoryContent.execute(Player, ShopCache, int)} (void) — performs the buy</li>
 * </ul>
 * Tiers, identifier, price and currency are read through the public API ({@link ICategoryContent},
 * {@link IContentTier}). All reflection is guarded; failures degrade to empty lists / failed purchases
 * and are logged. This is inherently fragile across BedWars1058 versions.
 */
public final class BedWars1058ShopProvider implements ShopProvider {
    private static final String SHOP_MANAGER = "com.andrei1058.bedwars.shop.ShopManager";
    private static final String SHOP_CACHE = "com.andrei1058.bedwars.shop.ShopCache";

    private final Logger logger;
    private final BedWars1058ApiAccess access;

    public BedWars1058ShopProvider(Logger logger, BedWars1058ApiAccess access) {
        this.logger = logger;
        this.access = access;
    }

    @Override public String getProviderId() { return "BedWars1058"; }

    @Override public boolean isReady() { return access.isAvailable(); }

    @Override
    public List<ShopCategory> getCategories(Player player) {
        List<ShopCategory> out = new ArrayList<>();
        try {
            for (Object cat : categoryObjects()) {
                String name = (String) cat.getClass().getMethod("getName").invoke(cat);
                int slot = (int) cat.getClass().getMethod("getSlot").invoke(cat);
                out.add(new ShopCategory(name, categoryDisplay(cat, player, name), slot));
            }
        } catch (Throwable t) {
            logger.warning("BW1058 getCategories reflection failed: " + t.getClass().getSimpleName());
        }
        return out;
    }

    @Override
    public List<ShopContent> getCategoryContents(Player player, String categoryId) {
        BedWars api = access.get();
        if (api == null) return List.of();
        List<ShopContent> out = new ArrayList<>();
        try {
            Object cache = shopCache(player);
            for (Object cat : categoryObjects()) {
                String name = (String) cat.getClass().getMethod("getName").invoke(cat);
                if (!name.equals(categoryId)) continue;
                Object listObj = cat.getClass().getMethod("getCategoryContentList").invoke(cat);
                if (!(listObj instanceof List<?> contents)) break;
                for (Object o : contents) {
                    if (!(o instanceof ICategoryContent content)) continue;
                    List<IContentTier> tiers = content.getContentTiers();
                    if (tiers == null || tiers.isEmpty()) continue;
                    IContentTier ct = selectTier(content, cache, tiers);
                    int price = ct.getPrice();
                    Material currency = ct.getCurrency();
                    int money = api.getShopUtil().calculateMoney(player, currency);
                    boolean affordable = money >= price;
                    out.add(new ShopContent(content.getIdentifier(), displayName(content, player),
                            price, currencyName(currency), affordable, ""));
                }
                break;
            }
        } catch (Throwable t) {
            logger.warning("BW1058 getCategoryContents reflection failed: " + t.getClass().getSimpleName());
        }
        return out;
    }

    @Override
    public PurchaseResult purchase(Player player, String contentId) {
        try {
            Object cache = shopCache(player);
            Class<?> shopCacheClass = Class.forName(SHOP_CACHE);
            for (Object cat : categoryObjects()) {
                Object listObj = cat.getClass().getMethod("getCategoryContentList").invoke(cat);
                if (!(listObj instanceof List<?> contents)) continue;
                for (Object o : contents) {
                    if (!(o instanceof ICategoryContent content)) continue;
                    if (!content.getIdentifier().equals(contentId)) continue;
                    // CategoryContent.execute(Player, ShopCache, int) — void; BedWars handles money/messages.
                    content.getClass().getMethod("execute", Player.class, shopCacheClass, int.class)
                            .invoke(content, player, cache, content.getSlot());
                    return PurchaseResult.ok();
                }
            }
            return PurchaseResult.fail("not found");
        } catch (Throwable t) {
            logger.warning("BW1058 purchase reflection failed for " + contentId + ": " + t.getClass().getSimpleName());
            return PurchaseResult.fail("error");
        }
    }

    @Override
    public boolean ownsInventory(Inventory inventory) {
        // Shop interception is event-based (BedWars1058 ShopOpenEvent); unused here.
        return false;
    }

    // --- reflection helpers ------------------------------------------------

    /** ShopManager.getShop() (static) → ShopIndex.getCategoryList() → list of internal ShopCategory objects. */
    private List<?> categoryObjects() throws Exception {
        Class<?> shopManager = Class.forName(SHOP_MANAGER);
        Object index = shopManager.getMethod("getShop").invoke(null);
        if (index == null) return List.of();
        Object listObj = index.getClass().getMethod("getCategoryList").invoke(index);
        return listObj instanceof List<?> list ? list : List.of();
    }

    /** ShopCache.getShopCache(uuid) (static); creates one if absent. */
    private Object shopCache(Player player) throws Exception {
        Class<?> shopCache = Class.forName(SHOP_CACHE);
        UUID id = player.getUniqueId();
        Object cache = shopCache.getMethod("getShopCache", UUID.class).invoke(null, id);
        if (cache == null) {
            cache = shopCache.getConstructor(UUID.class).newInstance(id);
        }
        return cache;
    }

    /** Mirrors BedWars2023's tier selection using the reflective getContentTier. */
    private IContentTier selectTier(ICategoryContent content, Object cache, List<IContentTier> tiers) {
        int currentTier = 1;
        try {
            currentTier = (int) cache.getClass().getMethod("getContentTier", String.class)
                    .invoke(cache, content.getIdentifier()); // 1-based, 1 when unowned
        } catch (Throwable ignored) {
        }
        if (currentTier >= tiers.size()) return tiers.get(tiers.size() - 1);
        if (currentTier < 1) return tiers.get(0);
        return tiers.get(currentTier);
    }

    private String categoryDisplay(Object cat, Player player, String fallback) {
        try {
            Object icon = cat.getClass().getMethod("getItemStack", Player.class).invoke(cat, player);
            if (icon instanceof ItemStack is && is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
                return ChatColor.stripColor(is.getItemMeta().getDisplayName());
            }
        } catch (Throwable ignored) {
        }
        return fallback;
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
