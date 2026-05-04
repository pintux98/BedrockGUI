package it.pintux.life.shopguiaddon.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShopGuiAddonConfiguration {
    public static final String FILE_NAME = "shopgui-addon.yml";

    private final String mainTitle;
    private final String mainContent;
    private final String shopTitle;
    private final String shopContent;
    private final String itemTitle;
    private final String itemContent;
    private final String emptyShopMessage;
    private final String emptyPageMessage;
    private final String unavailableItemMessage;
    private final String previousButton;
    private final String nextButton;
    private final String backButton;
    private final String mainButton;
    private final String openLinkedButton;
    private final String buyLabel;
    private final String sellLabel;
    private final String tradeLabel;
    private final String decorationLabel;
    private final String noBedrockGui;
    private final String shopsNotReady;
    private final String noShopAccess;
    private final String transactionSuccess;
    private final String transactionFailed;
    private final String unsupportedTransaction;
    private final List<Integer> amountPresets;
    private final int warnThresholdMs;

    private ShopGuiAddonConfiguration(YamlConfiguration configuration) {
        this.mainTitle = color(configuration.getString("ui.main-title", "&2Shop Categories"));
        this.mainContent = color(configuration.getString("ui.main-content", "&7Choose a ShopGUI+ category adapted for Bedrock."));
        this.shopTitle = color(configuration.getString("ui.shop-title", "&2%shop_name% &7(Page %page%/%max_page%)"));
        this.shopContent = color(configuration.getString("ui.shop-content", "&7Economy: &f%economy%"));
        this.itemTitle = color(configuration.getString("ui.item-title", "&2%item_name%"));
        this.itemContent = color(configuration.getString("ui.item-content", "&7Type: &f%item_type%"));
        this.emptyShopMessage = color(configuration.getString("ui.empty-shop-message", "&cNo categories are currently available."));
        this.emptyPageMessage = color(configuration.getString("ui.empty-page-message", "&eThis category page has no Bedrock-compatible entries."));
        this.unavailableItemMessage = color(configuration.getString("ui.unavailable-item-message", "&cThis ShopGUI+ entry cannot be used from the Bedrock interface."));
        this.previousButton = color(configuration.getString("ui.previous-button", "&7Previous Page"));
        this.nextButton = color(configuration.getString("ui.next-button", "&7Next Page"));
        this.backButton = color(configuration.getString("ui.back-button", "&7Back"));
        this.mainButton = color(configuration.getString("ui.main-button", "&7Main Menu"));
        this.openLinkedButton = color(configuration.getString("ui.open-linked-button", "&bOpen Linked Shop"));
        this.buyLabel = color(configuration.getString("ui.buy-label", "&aBuy"));
        this.sellLabel = color(configuration.getString("ui.sell-label", "&cSell"));
        this.tradeLabel = color(configuration.getString("ui.trade-label", "&bTrade"));
        this.decorationLabel = color(configuration.getString("ui.decoration-label", "&7Decoration"));
        this.noBedrockGui = color(configuration.getString("messages.no-bedrockgui", "&cBedrockGUI API is not available."));
        this.shopsNotReady = color(configuration.getString("messages.shops-not-ready", "&eShopGUI+ shops are not loaded yet. Please try again in a moment."));
        this.noShopAccess = color(configuration.getString("messages.no-shop-access", "&cYou do not have permission to access this shop."));
        this.transactionSuccess = color(configuration.getString("messages.transaction-success", "&aShop action completed successfully."));
        this.transactionFailed = color(configuration.getString("messages.transaction-failed", "&cShop action failed: %reason%"));
        this.unsupportedTransaction = color(configuration.getString("messages.unsupported-transaction", "&cThis ShopGUI+ server build does not expose a compatible Bedrock transaction bridge."));
        this.amountPresets = normalizePresets(configuration.getIntegerList("ui.amount-presets"));
        this.warnThresholdMs = Math.max(1, configuration.getInt("performance.warn-threshold-ms", 5));
    }

    public static ShopGuiAddonConfiguration load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        return new ShopGuiAddonConfiguration(YamlConfiguration.loadConfiguration(file));
    }

    private static List<Integer> normalizePresets(List<Integer> rawValues) {
        List<Integer> normalized = new ArrayList<>();
        for (Integer rawValue : rawValues) {
            if (rawValue == null) {
                continue;
            }
            int value = Math.max(1, Math.min(64, rawValue));
            if (!normalized.contains(value)) {
                normalized.add(value);
            }
        }
        if (normalized.isEmpty()) {
            normalized.add(1);
            normalized.add(8);
            normalized.add(16);
            normalized.add(32);
            normalized.add(64);
        }
        Collections.sort(normalized);
        return Collections.unmodifiableList(normalized);
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public String render(String template, Map<String, String> replacements) {
        String output = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            output = output.replace("%" + entry.getKey().toLowerCase(Locale.ROOT) + "%", entry.getValue());
        }
        return output;
    }

    public String mainTitle() { return mainTitle; }
    public String mainContent() { return mainContent; }
    public String shopTitle() { return shopTitle; }
    public String shopContent() { return shopContent; }
    public String itemTitle() { return itemTitle; }
    public String itemContent() { return itemContent; }
    public String emptyShopMessage() { return emptyShopMessage; }
    public String emptyPageMessage() { return emptyPageMessage; }
    public String unavailableItemMessage() { return unavailableItemMessage; }
    public String previousButton() { return previousButton; }
    public String nextButton() { return nextButton; }
    public String backButton() { return backButton; }
    public String mainButton() { return mainButton; }
    public String openLinkedButton() { return openLinkedButton; }
    public String buyLabel() { return buyLabel; }
    public String sellLabel() { return sellLabel; }
    public String tradeLabel() { return tradeLabel; }
    public String decorationLabel() { return decorationLabel; }
    public String noBedrockGui() { return noBedrockGui; }
    public String shopsNotReady() { return shopsNotReady; }
    public String noShopAccess() { return noShopAccess; }
    public String transactionSuccess() { return transactionSuccess; }
    public String transactionFailed() { return transactionFailed; }
    public String unsupportedTransaction() { return unsupportedTransaction; }
    public List<Integer> amountPresets() { return amountPresets; }
    public int warnThresholdMs() { return warnThresholdMs; }
}
