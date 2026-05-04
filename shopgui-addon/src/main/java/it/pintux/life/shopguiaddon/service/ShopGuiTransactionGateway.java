package it.pintux.life.shopguiaddon.service;

import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.entity.Player;

public interface ShopGuiTransactionGateway {
    TransactionExecutionResult execute(Player player, ShopItem shopItem, BedrockShopAction action, int amount);

    record TransactionExecutionResult(boolean success, String message) {
        public static TransactionExecutionResult success(String message) {
            return new TransactionExecutionResult(true, message);
        }

        public static TransactionExecutionResult failure(String message) {
            return new TransactionExecutionResult(false, message);
        }
    }
}
