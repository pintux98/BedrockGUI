package it.pintux.life.shopguiaddon.util;

import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
