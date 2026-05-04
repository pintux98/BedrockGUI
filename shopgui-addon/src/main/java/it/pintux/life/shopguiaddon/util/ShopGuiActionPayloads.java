package it.pintux.life.shopguiaddon.util;

import it.pintux.life.shopguiaddon.service.BedrockShopAction;

public final class ShopGuiActionPayloads {
    private ShopGuiActionPayloads() {
    }

    public static String encodeShop(String shopId, int page) {
        return shopId + "|" + page;
    }

    public static ShopPayload decodeShop(String payload) {
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid shop payload: " + payload);
        }
        return new ShopPayload(parts[0], parseInteger(parts[1], 1));
    }

    public static String encodeItem(String shopId, String itemId, int page) {
        return shopId + "|" + itemId + "|" + page;
    }

    public static ItemPayload decodeItem(String payload) {
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid item payload: " + payload);
        }
        return new ItemPayload(parts[0], parts[1], parseInteger(parts[2], 1));
    }

    public static String encodeTransaction(BedrockShopAction action, String shopId, String itemId, int amount, int page) {
        return action.name() + "|" + shopId + "|" + itemId + "|" + amount + "|" + page;
    }

    public static TransactionPayload decodeTransaction(String payload) {
        String[] parts = payload.split("\\|", -1);
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid transaction payload: " + payload);
        }
        return new TransactionPayload(
                BedrockShopAction.valueOf(parts[0]),
                parts[1],
                parts[2],
                parseInteger(parts[3], 1),
                parseInteger(parts[4], 1)
        );
    }

    private static int parseInteger(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public record ShopPayload(String shopId, int page) { }
    public record ItemPayload(String shopId, String itemId, int page) { }
    public record TransactionPayload(BedrockShopAction action, String shopId, String itemId, int amount, int page) { }
}
