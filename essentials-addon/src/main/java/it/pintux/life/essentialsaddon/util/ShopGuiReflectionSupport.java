package it.pintux.life.essentialsaddon.util;

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
import java.util.Objects;

public final class ShopGuiReflectionSupport {

    private ShopGuiReflectionSupport() {
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
        return itemStack.getType().name().toLowerCase(Locale.ROOT);
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
