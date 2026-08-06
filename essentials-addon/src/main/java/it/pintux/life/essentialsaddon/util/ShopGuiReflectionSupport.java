package it.pintux.life.essentialsaddon.util;

import it.pintux.life.common.utils.IconResolver;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopGuiReflectionSupport {

    private static final Map<Class<?>, Optional<Method>> ACCESS_METHOD_CACHE = new ConcurrentHashMap<>();

    private ShopGuiReflectionSupport() {
    }

    /**
     * Calls the shop's own per-item access check without binding to a single ShopGUI+ signature.
     * The API we compile against declares {@code hasAccess(Player, ShopItem, boolean)}, while 1.113.0 renamed
     * that gate to {@code hasAccessToItem} and kept {@code hasAccess(Player)} as the shop-level gate, so a
     * direct call raises NoSuchMethodError. Only overloads that take the item count as item checks - the
     * player-only overload answers a different question and must never stand in for one.
     *
     * @return the shop's verdict, or {@code null} when this build exposes no usable overload
     */
    public static Boolean invokeAccessCheck(Object shop, Object player, Object shopItem) {
        if (shop == null || player == null) {
            return null;
        }
        Optional<Method> resolved = ACCESS_METHOD_CACHE.computeIfAbsent(shop.getClass(),
                type -> resolveAccessMethod(type, player, shopItem));
        if (resolved.isEmpty()) {
            return null;
        }
        Method method = resolved.get();
        // the third argument is ShopGUI+'s "tell the player why" flag, so keep it false and stay silent
        Object[] args = method.getParameterCount() == 2
                ? new Object[]{player, shopItem}
                : new Object[]{player, shopItem, Boolean.FALSE};
        try {
            Object result = method.invoke(shop, args);
            return result instanceof Boolean value ? value : null;
        } catch (Throwable throwable) {
            return null;
        }
    }

    /**
     * Reads a no-argument boolean getter if the running build has it, otherwise returns {@code fallback}.
     */
    public static boolean booleanFlag(Object target, boolean fallback, String... methodNames) {
        if (target == null) {
            return fallback;
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value instanceof Boolean flag) {
                    return flag;
                }
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }

    /** Item-gate method names, most specific first. */
    private static final List<String> ITEM_ACCESS_METHODS = List.of("hasAccessToItem", "hasAccess");

    private static Optional<Method> resolveAccessMethod(Class<?> shopType, Object player, Object shopItem) {
        Method best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Method candidate : shopType.getMethods()) {
            int nameRank = ITEM_ACCESS_METHODS.indexOf(candidate.getName());
            if (nameRank < 0 || !isBoolean(candidate.getReturnType())) {
                continue;
            }
            Class<?>[] parameters = candidate.getParameterTypes();
            // an item check must take the item; hasAccess(Player) is the shop-level gate, not this one
            if (parameters.length < 2 || parameters.length > 3) {
                continue;
            }
            if (!accepts(parameters[0], player) || !accepts(parameters[1], shopItem)) {
                continue;
            }
            if (parameters.length == 3 && !isBoolean(parameters[2])) {
                continue;
            }
            // prefer the dedicated name, then the overload that also takes the silent flag
            int score = (ITEM_ACCESS_METHODS.size() - nameRank) * 10 + parameters.length;
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean accepts(Class<?> parameterType, Object argument) {
        return argument != null && parameterType.isInstance(argument);
    }

    private static boolean isBoolean(Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    public static String resolveLinkedShopId(ShopItem shopItem) {
        Object direct = invokeMatchingAccessor(shopItem, String.class,
                "getLinkedShopId", "getTargetShopId", "getShopLink", "getLinkedShop", "getTargetShop");
        if (direct instanceof String value && !value.isBlank()) {
            return value;
        }
        if (direct instanceof Shop shop) {
            return shop.getId();
        }

        Object fieldValue = readMatchingField(shopItem, "linkedShop", "linkedShopId", "targetShop", "targetShopId");
        if (fieldValue instanceof String value && !value.isBlank()) {
            return value;
        }
        if (fieldValue instanceof Shop shop) {
            return shop.getId();
        }
        return null;
    }

    public static String displayName(ItemStack itemStack) {
        if (itemStack == null) {
            return "Unknown Item";
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            return itemMeta.getDisplayName();
        }
        if (itemMeta instanceof PotionMeta potionMeta) {
            try {
                PotionData data = potionMeta.getBasePotionData();
                if (data != null && data.getType() != null) {
                    return composePotionName(itemStack.getType().name(), data.getType().name(),
                            data.isUpgraded(), data.isExtended());
                }
            } catch (Throwable ignored) {
                // fall through to generic prettify below
            }
        }
        return prettify(itemStack.getType().name());
    }

    public static String description(ItemStack itemStack) {
        if (itemStack == null) {
            return "No extra details";
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || !itemMeta.hasLore() || itemMeta.getLore() == null || itemMeta.getLore().isEmpty()) {
            return "No extra details";
        }
        List<String> cleaned = new ArrayList<>();
        for (String line : itemMeta.getLore()) {
            cleaned.add(ChatColor.stripColor(line));
        }
        return String.join(" | ", cleaned);
    }

    public static String material(ItemStack itemStack) {
        if (itemStack == null) {
            return "unknown";
        }
        String potionTexture = potionTexture(itemStack);
        if (potionTexture != null) {
            return potionTexture;
        }
        return itemStack.getType().name().toLowerCase(Locale.ROOT);
    }

    /**
     * Bedrock draws a different texture per potion effect, and the effect lives in the item meta
     * rather than the material. Resolve it here so the form button shows the right bottle/arrow;
     * the returned value is already a texture path, which IconResolver passes through untouched.
     */
    private static String potionTexture(ItemStack itemStack) {
        if (!(itemStack.getItemMeta() instanceof PotionMeta potionMeta)) {
            return null;
        }
        String type = null;
        try {
            PotionData data = potionMeta.getBasePotionData();
            if (data != null && data.getType() != null) {
                type = data.getType().name();
            }
        } catch (Throwable ignored) {
            // removed on newer APIs; the plain container texture is a fine fallback
        }
        return IconResolver.resolvePotion(itemStack.getType().name(), type);
    }

    public static String prettify(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        String[] parts = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    public static String composePotionName(String materialName, String potionTypeName,
                                           boolean upgraded, boolean extended) {
        String prefix = potionPrefix(materialName);
        if (potionTypeName == null || isBasePotion(potionTypeName)) {
            return prefix;
        }
        StringBuilder builder = new StringBuilder(prefix).append(" of ").append(prettify(potionTypeName));
        if (upgraded) {
            builder.append(" II");
        }
        if (extended) {
            builder.append(" (Extended)");
        }
        return builder.toString();
    }

    private static String potionPrefix(String materialName) {
        if (materialName == null) {
            return "Potion";
        }
        switch (materialName) {
            case "SPLASH_POTION": return "Splash Potion";
            case "LINGERING_POTION": return "Lingering Potion";
            case "TIPPED_ARROW": return "Tipped Arrow";
            default: return "Potion";
        }
    }

    private static boolean isBasePotion(String potionTypeName) {
        switch (potionTypeName) {
            case "WATER":
            case "MUNDANE":
            case "THICK":
            case "AWKWARD":
                return true;
            default:
                return false;
        }
    }

    private static Object invokeMatchingAccessor(Object target, Class<?> expectedType, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);
                if (value == null || expectedType.isInstance(value) || value instanceof Shop) {
                    return value;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Object readMatchingField(Object target, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value != null) {
                    return value;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
