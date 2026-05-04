package it.pintux.life.shopguiaddon.service;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.shop.ShopManager.ShopAction;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

public final class ReflectiveShopGuiTransactionGateway implements ShopGuiTransactionGateway {
    private final Logger logger;

    public ReflectiveShopGuiTransactionGateway(Logger logger) {
        this.logger = logger;
    }

    @Override
    public TransactionExecutionResult execute(Player player, ShopItem shopItem, BedrockShopAction action, int amount) {
        Object plugin = ShopGuiPlusApi.getPlugin();
        if (plugin == null) {
            return TransactionExecutionResult.failure("ShopGUI+ plugin instance is unavailable");
        }

        for (Object target : enumerateTargets(plugin)) {
            for (Method method : target.getClass().getMethods()) {
                if (!isCandidate(method, action)) {
                    continue;
                }
                Object[] arguments = buildArguments(method.getParameterTypes(), player, shopItem, action, amount);
                if (arguments == null) {
                    continue;
                }
                try {
                    Object result = method.invoke(target, arguments);
                    return interpretResult(result, method.getName(), action);
                } catch (Exception exception) {
                    logger.fine("ShopGUI+ transaction candidate failed on " + method.getName() + ": " + exception.getMessage());
                }
            }
        }
        return TransactionExecutionResult.failure("No compatible ShopGUI+ transaction method was found");
    }

    private List<Object> enumerateTargets(Object plugin) {
        Set<Object> targets = new LinkedHashSet<>();
        targets.add(plugin);
        for (Method method : plugin.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || !method.getName().startsWith("get") || !method.getName().endsWith("Manager")) {
                continue;
            }
            try {
                Object value = method.invoke(plugin);
                if (value != null) {
                    targets.add(value);
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(targets);
    }

    private boolean isCandidate(Method method, BedrockShopAction action) {
        String methodName = method.getName().toLowerCase(Locale.ROOT);
        if (method.getParameterCount() == 0 || !containsShopParameters(method.getParameterTypes())) {
            return false;
        }
        return switch (action) {
            case BUY, TRADE -> methodName.contains("buy") || methodName.contains("purchase") || methodName.contains("transaction");
            case SELL -> methodName.contains("sell") || methodName.contains("transaction");
            case LINK -> false;
        };
    }

    private boolean containsShopParameters(Class<?>[] parameterTypes) {
        boolean hasPlayer = false;
        boolean hasItem = false;
        for (Class<?> parameterType : parameterTypes) {
            hasPlayer |= Player.class.isAssignableFrom(parameterType);
            hasItem |= ShopItem.class.isAssignableFrom(parameterType);
        }
        return hasPlayer && hasItem;
    }

    private Object[] buildArguments(Class<?>[] parameterTypes, Player player, ShopItem shopItem, BedrockShopAction action, int amount) {
        Object[] arguments = new Object[parameterTypes.length];
        double price = action == BedrockShopAction.SELL ? shopItem.getSellPriceForAmount(player, amount) : shopItem.getBuyPriceForAmount(player, amount);
        for (int index = 0; index < parameterTypes.length; index++) {
            Class<?> parameterType = parameterTypes[index];
            if (Player.class.isAssignableFrom(parameterType)) {
                arguments[index] = player;
            } else if (ShopItem.class.isAssignableFrom(parameterType)) {
                arguments[index] = shopItem;
            } else if (parameterType == int.class || parameterType == Integer.class) {
                arguments[index] = amount;
            } else if (parameterType == double.class || parameterType == Double.class) {
                arguments[index] = price;
            } else if (parameterType == boolean.class || parameterType == Boolean.class) {
                arguments[index] = Boolean.FALSE;
            } else if (parameterType.isEnum() && parameterType.getSimpleName().equals("ShopAction")) {
                arguments[index] = Enum.valueOf((Class<Enum>) parameterType.asSubclass(Enum.class), mapAction(action).name());
            } else {
                return null;
            }
        }
        return arguments;
    }

    private TransactionExecutionResult interpretResult(Object result, String methodName, BedrockShopAction action) {
        if (result == null) {
            return TransactionExecutionResult.success("Executed via " + methodName);
        }
        if (result instanceof Boolean booleanResult) {
            return booleanResult
                    ? TransactionExecutionResult.success("Executed via " + methodName)
                    : TransactionExecutionResult.failure("ShopGUI+ rejected the " + action.name().toLowerCase(Locale.ROOT) + " request");
        }
        try {
            Method resultAccessor = result.getClass().getMethod("getResult");
            Object value = resultAccessor.invoke(result);
            if (value != null && value.toString().toUpperCase(Locale.ROOT).contains("SUCCESS")) {
                return TransactionExecutionResult.success(value.toString());
            }
            if (value != null) {
                return TransactionExecutionResult.failure(value.toString());
            }
        } catch (Exception ignored) {
        }
        return TransactionExecutionResult.success(result.toString());
    }

    private ShopAction mapAction(BedrockShopAction action) {
        return switch (action) {
            case SELL -> ShopAction.SELL;
            case BUY, TRADE, LINK -> ShopAction.BUY;
        };
    }
}
