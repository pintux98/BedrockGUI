package it.pintux.life.essentialsaddon.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class EssentialsAddonConfiguration {
    public static final String FILE_NAME = "config.yml";

    // Module toggles
    private final boolean moduleWarps;
    private final boolean moduleKits;
    private final boolean moduleHomes;
    private final boolean moduleTpa;
    private final boolean moduleShopGuiPlus;
    private final boolean moduleEconomyShopGui;
    private final boolean moduleMyPet;

    // Actions-only (register actions without internal forms)
    private final boolean actionsWarps;
    private final boolean actionsKits;
    private final boolean actionsHomes;
    private final boolean actionsTpa;
    private final boolean actionsShopGuiPlus;
    private final boolean actionsEconomyShopGui;
    private final boolean actionsMyPet;

    // Hub
    private final String hubTitle;
    private final String hubContent;
    private final String hubButtonWarps;
    private final String hubButtonKits;
    private final String hubButtonHomes;
    private final String hubButtonTpa;
    private final String hubButtonShopGuiPlus;
    private final String hubButtonEconomyShopGui;
    private final String hubButtonMyPet;

    private final String warpTitle;
    private final String warpContent;
    private final String warpButton;
    private final String noWarpsMessage;
    private final String noWarpAccess;
    private final String teleportSuccess;
    private final String teleportFailed;

    private final String kitTitle;
    private final String kitContent;
    private final String kitButton;
    private final String noKitsMessage;
    private final String noKitAccess;
    private final String kitClaimSuccess;
    private final String kitClaimFailed;
    private final String kitOnCooldown;

    private final String backButton;
    private final String mainButton;
    private final String previousButton;
    private final String nextButton;

    private final String homeTitle;
    private final String homeContent;
    private final String homeButton;
    private final String homeLimitText;
    private final String noHomesMessage;
    private final String homeNotFound;
    private final String homeTeleportSuccess;
    private final String homeTeleportFailed;
    private final String homeLimitReached;
    private final String homeSetSuccess;
    private final String homeSetFailed;
    private final String homeSetInvalid;
    private final String homeDeleteButton;
    private final String homeDeleteSuccess;
    private final String homeNoDelete;
    private final String homeDeleteFailed;
    private final String homeProviderUnavailable;

    private final String tpaTitle;
    private final String tpaContent;
    private final String tpaPendingContent;
    private final String tpaAcceptButton;
    private final String tpaDenyButton;
    private final String tpaSendButton;
    private final String tpaHereButton;
    private final String tpaCancelButton;
    private final String tpaNoPending;
    private final String tpaSendFailed;
    private final String tpaTitleSend;
    private final String tpaTitleHere;
    private final String tpaSendContent;
    private final String tpaPlayerInputText;
    private final String tpaPlayerInputPlaceholder;
    private final String tpaProviderUnavailable;

    private final String shopMainTitle;
    private final String shopMainContent;
    private final String shopShopTitle;
    private final String shopShopContent;
    private final String shopItemTitle;
    private final String shopItemContent;
    private final String shopEmptyShopMessage;
    private final String shopEmptyPageMessage;
    private final String shopUnavailableItemMessage;
    private final String shopOpenLinkedButton;
    private final String shopBuyLabel;
    private final String shopSellLabel;
    private final String shopTradeLabel;
    private final String shopDecorationLabel;
    private final String shopShopsNotReady;
    private final String shopNoShopAccess;
    private final String shopTransactionSuccess;
    private final String shopTransactionFailed;
    private final String shopUnsupportedTransaction;
    private final List<Integer> shopAmountPresets;
    private final boolean shopRequirePurchaseConfirmation;
    private final String soundShopPurchaseSuccess;
    private final String soundShopPurchaseFailed;

    private final String petShopTitle;
    private final String petShopContent;
    private final String petShopButton;
    private final String petShopOwnedSuffix;
    private final String petBuyConfirmTitle;
    private final String petBuyConfirmContent;
    private final String petBuyConfirmYes;
    private final String petBuyConfirmNo;
    private final String petListTitle;
    private final String petListContent;
    private final String petListButton;
    private final String petActiveSuffix;
    private final String petInfoTitle;
    private final String petInfoContent;
    private final String petCallButton;
    private final String petPutAwayButton;
    private final String petSkilltreeButton;
    private final String petSkilltreeTitle;
    private final String petSkilltreeContent;
    private final String petSkilltreeOption;
    private final String petSkilltreeCurrentSuffix;
    private final String petNoPets;
    private final String petNoActivePet;
    private final String petNotReady;
    private final String petBuySuccess;
    private final String petBuyFailed;
    private final String petCannotAfford;
    private final String petCallSuccess;
    private final String petCallFailed;
    private final String petPutAwaySuccess;
    private final String petPutAwayFailed;
    private final String petSkilltreeSetSuccess;
    private final String petSkilltreeSetFailed;

    private final String noBedrockGui;
    private final String essentialsNotReady;
    private final String providerUnavailable;

    private final boolean soundsEnabled;
    private final String soundFormOpen;
    private final String soundTeleportSuccess;
    private final String soundKitClaimSuccess;
    private final String soundActionFailed;
    private final float soundVolume;
    private final float soundPitch;

    private EssentialsAddonConfiguration(YamlConfiguration configuration) {
        this.moduleWarps = configuration.getBoolean("modules.warps", false);
        this.moduleKits = configuration.getBoolean("modules.kits", false);
        this.moduleHomes = configuration.getBoolean("modules.homes", false);
        this.moduleTpa = configuration.getBoolean("modules.tpa", false);
        this.moduleShopGuiPlus = configuration.getBoolean("modules.shopgui-plus", false);
        this.moduleEconomyShopGui = configuration.getBoolean("modules.economyshop-gui", false);
        this.moduleMyPet = configuration.getBoolean("modules.mypet", false);

        this.actionsWarps = configuration.getBoolean("actions-only.warps", false);
        this.actionsKits = configuration.getBoolean("actions-only.kits", false);
        this.actionsHomes = configuration.getBoolean("actions-only.homes", false);
        this.actionsTpa = configuration.getBoolean("actions-only.tpa", false);
        this.actionsShopGuiPlus = configuration.getBoolean("actions-only.shopgui-plus", false);
        this.actionsEconomyShopGui = configuration.getBoolean("actions-only.economyshop-gui", false);
        this.actionsMyPet = configuration.getBoolean("actions-only.mypet", false);

        this.hubTitle = color(configuration.getString("hub.title", "&6&lEssentials Menu"));
        this.hubContent = color(configuration.getString("hub.content", "&7Select a feature to use."));
        this.hubButtonWarps = color(configuration.getString("hub.button-warps", "&b&lWarps"));
        this.hubButtonKits = color(configuration.getString("hub.button-kits", "&6&lKits"));
        this.hubButtonHomes = color(configuration.getString("hub.button-homes", "&a&lHomes"));
        this.hubButtonTpa = color(configuration.getString("hub.button-tpa", "&e&lTeleport"));
        this.hubButtonShopGuiPlus = color(configuration.getString("hub.button-shopgui-plus", "&2&lShopGUI+"));
        this.hubButtonEconomyShopGui = color(configuration.getString("hub.button-economyshop-gui", "&2&lEconomyShop"));
        this.hubButtonMyPet = color(configuration.getString("hub.button-mypet", "&d&lPets"));

        this.warpTitle = color(configuration.getString("ui.warp-title", "&bWarps"));
        this.warpContent = color(configuration.getString("ui.warp-content", "&7Select a warp to teleport to."));
        this.warpButton = color(configuration.getString("ui.warp-button", "&e%warp_name%"));
        this.noWarpsMessage = color(configuration.getString("ui.no-warps-message", "&cNo warps are currently available."));
        this.noWarpAccess = color(configuration.getString("ui.no-warp-access", "&cYou do not have permission to use this warp."));
        this.teleportSuccess = color(configuration.getString("ui.teleport-success", "&aTeleported to %warp_name%."));
        this.teleportFailed = color(configuration.getString("ui.teleport-failed", "&cTeleport failed: %reason%"));

        this.kitTitle = color(configuration.getString("ui.kit-title", "&6Kits"));
        this.kitContent = color(configuration.getString("ui.kit-content", "&7Select a kit to claim."));
        this.kitButton = color(configuration.getString("ui.kit-button", "&e%kit_name%"));
        this.noKitsMessage = color(configuration.getString("ui.no-kits-message", "&cNo kits are currently available."));
        this.noKitAccess = color(configuration.getString("ui.no-kit-access", "&cYou do not have permission to use this kit."));
        this.kitClaimSuccess = color(configuration.getString("ui.kit-claim-success", "&aKit claimed successfully!"));
        this.kitClaimFailed = color(configuration.getString("ui.kit-claim-failed", "&cKit claim failed: %reason%"));
        this.kitOnCooldown = color(configuration.getString("ui.kit-on-cooldown", "&cThis kit is on cooldown. Available in %time%."));

        this.backButton = color(configuration.getString("ui.back-button", "&7Back"));
        this.mainButton = color(configuration.getString("ui.main-button", "&7Main Menu"));
        this.previousButton = color(configuration.getString("ui.previous-button", "&7Previous Page"));
        this.nextButton = color(configuration.getString("ui.next-button", "&7Next Page"));

        this.homeTitle = color(configuration.getString("ui.home-title", "&bHomes"));
        this.homeContent = color(configuration.getString("ui.home-content", "&7Select a home to teleport to."));
        this.homeButton = color(configuration.getString("ui.home-button", "&e%home_name%"));
        this.homeLimitText = color(configuration.getString("ui.home-limit-text", " (%count%/%max%)"));
        this.noHomesMessage = color(configuration.getString("ui.no-homes-message", "&cYou do not have any homes set."));
        this.homeNotFound = color(configuration.getString("ui.home-not-found", "&cHome not found: %home_name%"));
        this.homeTeleportSuccess = color(configuration.getString("ui.home-teleport-success", "&aTeleported to %home_name%!"));
        this.homeTeleportFailed = color(configuration.getString("ui.home-teleport-failed", "&cFailed to teleport to %home_name%"));
        this.homeLimitReached = color(configuration.getString("ui.home-limit-reached", "&cYou have reached your home limit (%count%/%max%)"));
        this.homeSetSuccess = color(configuration.getString("ui.home-set-success", "&aHome '%home_name%' set!"));
        this.homeSetFailed = color(configuration.getString("ui.home-set-failed", "&cFailed to set home."));
        this.homeSetInvalid = color(configuration.getString("ui.home-set-invalid", "&cInvalid home name."));
        this.homeDeleteButton = color(configuration.getString("ui.home-delete-prompt", "&c%home_name%"));
        this.homeDeleteSuccess = color(configuration.getString("ui.home-delete-success", "&aHome '%home_name%' deleted!"));
        this.homeNoDelete = color(configuration.getString("ui.home-no-delete", "&cYou do not have any homes to delete."));
        this.homeDeleteFailed = color(configuration.getString("ui.home-delete-failed", "&cFailed to delete home."));
        this.homeProviderUnavailable = color(configuration.getString("ui.home-provider-unavailable", "&cHome provider is not available."));

        this.tpaTitle = color(configuration.getString("ui.tpa-title", "&bTeleport Requests"));
        this.tpaContent = color(configuration.getString("ui.tpa-content", "&7Manage your teleport requests."));
        this.tpaPendingContent = color(configuration.getString("ui.tpa-pending-content", "&7Pending from: &f%players%"));
        this.tpaAcceptButton = color(configuration.getString("ui.tpa-accept-button", "&aAccept Request"));
        this.tpaDenyButton = color(configuration.getString("ui.tpa-deny-button", "&cDeny Request"));
        this.tpaSendButton = color(configuration.getString("ui.tpa-send-button", "&eSend TPA"));
        this.tpaHereButton = color(configuration.getString("ui.tpa-here-button", "&6Send TPAHere"));
        this.tpaCancelButton = color(configuration.getString("ui.tpa-cancel-button", "&7Cancel Request"));
        this.tpaNoPending = color(configuration.getString("ui.tpa-no-pending", "&cNo pending requests."));
        this.tpaSendFailed = color(configuration.getString("ui.tpa-send-failed", "&cFailed to send request."));
        this.tpaTitleSend = color(configuration.getString("ui.tpa-title-send", "&eTPA - Select Player"));
        this.tpaTitleHere = color(configuration.getString("ui.tpa-title-here", "&6TPAHere - Select Player"));
        this.tpaSendContent = color(configuration.getString("ui.tpa-send-content", "&7Select a player to request teleport."));
        this.tpaPlayerInputText = color(configuration.getString("ui.tpa-player-input-text", "&7Enter player name:"));
        this.tpaPlayerInputPlaceholder = configuration.getString("ui.tpa-player-input-placeholder", "PlayerName");
        this.tpaProviderUnavailable = color(configuration.getString("ui.tpa-provider-unavailable", "&cTPA provider is not available."));

        this.shopMainTitle = color(configuration.getString("ui.shop-main-title", "&2Shop Categories"));
        this.shopMainContent = color(configuration.getString("ui.shop-main-content", "&7Choose a supported shop category adapted for Bedrock."));
        this.shopShopTitle = color(configuration.getString("ui.shop-shop-title", "&2%shop_name% &7(Page %page%/%max_page%)"));
        this.shopShopContent = color(configuration.getString("ui.shop-shop-content", "&7Economy: &f%economy%"));
        this.shopItemTitle = color(configuration.getString("ui.shop-item-title", "&2%item_name%"));
        this.shopItemContent = color(configuration.getString("ui.shop-item-content", "&7Type: &f%item_type%"));
        this.shopEmptyShopMessage = color(configuration.getString("ui.shop-empty-shop-message", "&cNo categories are currently available."));
        this.shopEmptyPageMessage = color(configuration.getString("ui.shop-empty-page-message", "&eThis category page has no Bedrock-compatible entries."));
        this.shopUnavailableItemMessage = color(configuration.getString("ui.shop-unavailable-item-message", "&cThis shop entry cannot be used from the Bedrock interface."));
        this.shopOpenLinkedButton = color(configuration.getString("ui.shop-open-linked-button", "&bOpen Linked Shop"));
        this.shopBuyLabel = color(configuration.getString("ui.shop-buy-label", "&aBuy"));
        this.shopSellLabel = color(configuration.getString("ui.shop-sell-label", "&cSell"));
        this.shopTradeLabel = color(configuration.getString("ui.shop-trade-label", "&bTrade"));
        this.shopDecorationLabel = color(configuration.getString("ui.shop-decoration-label", "&7Decoration"));
        this.shopShopsNotReady = color(configuration.getString("messages.shop-shops-not-ready", "&eThe shop backend is not loaded yet."));
        this.shopNoShopAccess = color(configuration.getString("messages.shop-no-shop-access", "&cYou do not have permission to access this shop."));
        this.shopTransactionSuccess = color(configuration.getString("messages.shop-transaction-success", "&aShop action completed successfully."));
        this.shopTransactionFailed = color(configuration.getString("messages.shop-transaction-failed", "&cShop action failed: %reason%"));
        this.shopUnsupportedTransaction = color(configuration.getString("messages.shop-unsupported-transaction", "&cThis shop backend does not expose a compatible Bedrock transaction bridge."));
        this.shopAmountPresets = normalizePresets(configuration.getIntegerList("ui.shop-amount-presets"));
        this.shopRequirePurchaseConfirmation = configuration.getBoolean("ui.shop-require-purchase-confirmation", true);
        this.soundShopPurchaseSuccess = configuration.getString("sounds.shop-purchase-success", "entity.player.levelup");
        this.soundShopPurchaseFailed = configuration.getString("sounds.shop-purchase-failed", "block.note_block.pling");

        this.petShopTitle = color(configuration.getString("ui.pet-shop-title", "&dPet Shop"));
        this.petShopContent = color(configuration.getString("ui.pet-shop-content", "&7Buy a pet."));
        this.petShopButton = color(configuration.getString("ui.pet-shop-button", "&e%pet_name% &7- &a%price%"));
        this.petShopOwnedSuffix = color(configuration.getString("ui.pet-shop-owned-suffix", " &8(Owned)"));
        this.petBuyConfirmTitle = color(configuration.getString("ui.pet-buy-confirm-title", "&dBuy Pet"));
        this.petBuyConfirmContent = color(configuration.getString("ui.pet-buy-confirm-content", "&7Buy &f%pet_name% &7for &a%price%&7?"));
        this.petBuyConfirmYes = color(configuration.getString("ui.pet-buy-confirm-yes", "&aBuy"));
        this.petBuyConfirmNo = color(configuration.getString("ui.pet-buy-confirm-no", "&cCancel"));
        this.petListTitle = color(configuration.getString("ui.pet-list-title", "&dYour Pets"));
        this.petListContent = color(configuration.getString("ui.pet-list-content", "&7Select a pet."));
        this.petListButton = color(configuration.getString("ui.pet-list-button", "&e%pet_name% &7(%pet_type%)"));
        this.petActiveSuffix = color(configuration.getString("ui.pet-active-suffix", " &a(Active)"));
        this.petInfoTitle = color(configuration.getString("ui.pet-info-title", "&d%pet_name%"));
        this.petInfoContent = color(configuration.getString("ui.pet-info-content",
                "&7Type: &f%pet_type%\n&7Level: &f%level%\n&7Health: &f%hp%/%max_hp%\n&7Hunger: &f%hunger%\n&7Skilltree: &f%skilltree%"));
        this.petCallButton = color(configuration.getString("ui.pet-call-button", "&aCall Pet"));
        this.petPutAwayButton = color(configuration.getString("ui.pet-put-away-button", "&6Put Away"));
        this.petSkilltreeButton = color(configuration.getString("ui.pet-skilltree-button", "&bSkilltree"));
        this.petSkilltreeTitle = color(configuration.getString("ui.pet-skilltree-title", "&bSkilltrees"));
        this.petSkilltreeContent = color(configuration.getString("ui.pet-skilltree-content", "&7Choose a skilltree."));
        this.petSkilltreeOption = color(configuration.getString("ui.pet-skilltree-option", "&e%skilltree%"));
        this.petSkilltreeCurrentSuffix = color(configuration.getString("ui.pet-skilltree-current-suffix", " &a(Current)"));
        this.petNoPets = color(configuration.getString("messages.pet-no-pets", "&cYou have no pets."));
        this.petNoActivePet = color(configuration.getString("messages.pet-no-active-pet", "&cYou need an active pet first."));
        this.petNotReady = color(configuration.getString("messages.pet-not-ready", "&eMyPet is not loaded yet."));
        this.petBuySuccess = color(configuration.getString("messages.pet-buy-success", "&aPet purchased!"));
        this.petBuyFailed = color(configuration.getString("messages.pet-buy-failed", "&cPurchase failed: %reason%"));
        this.petCannotAfford = color(configuration.getString("messages.pet-cannot-afford", "&cYou cannot afford this pet."));
        this.petCallSuccess = color(configuration.getString("messages.pet-call-success", "&aPet called!"));
        this.petCallFailed = color(configuration.getString("messages.pet-call-failed", "&cCould not call that pet."));
        this.petPutAwaySuccess = color(configuration.getString("messages.pet-put-away-success", "&aPet put away."));
        this.petPutAwayFailed = color(configuration.getString("messages.pet-put-away-failed", "&cCould not put that pet away."));
        this.petSkilltreeSetSuccess = color(configuration.getString("messages.pet-skilltree-set-success", "&aSkilltree changed!"));
        this.petSkilltreeSetFailed = color(configuration.getString("messages.pet-skilltree-set-failed", "&cCould not change skilltree."));

        this.noBedrockGui = color(configuration.getString("messages.no-bedrockgui", "&cBedrockGUI API is not available."));
        this.essentialsNotReady = color(configuration.getString("messages.essentials-not-ready", "&eThe Essentials backend is not loaded yet."));
        this.providerUnavailable = color(configuration.getString("messages.provider-unavailable", "&cNo compatible provider is available."));

        this.soundsEnabled = configuration.getBoolean("sounds.enabled", true);
        this.soundFormOpen = configuration.getString("sounds.form-open", "ui.button.click");
        this.soundTeleportSuccess = configuration.getString("sounds.teleport-success", "entity.enderman.teleport");
        this.soundKitClaimSuccess = configuration.getString("sounds.kit-claim-success", "entity.player.levelup");
        this.soundActionFailed = configuration.getString("sounds.action-failed", "block.note_block.pling");
        this.soundVolume = (float) configuration.getDouble("sounds.volume", 1.0);
        this.soundPitch = (float) configuration.getDouble("sounds.pitch", 1.0);
    }

    public static EssentialsAddonConfiguration load(JavaPlugin plugin) {
        YamlConfiguration config = new ConfigMigrator(plugin, FILE_NAME).migrate();
        return new EssentialsAddonConfiguration(config);
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    private static List<Integer> normalizePresets(List<Integer> rawValues) {
        List<Integer> normalized = new ArrayList<>();
        for (Integer rawValue : rawValues) {
            if (rawValue == null) continue;
            int value = Math.max(1, Math.min(64, rawValue));
            if (!normalized.contains(value)) normalized.add(value);
        }
        if (normalized.isEmpty()) {
            normalized.add(1); normalized.add(8); normalized.add(16);
            normalized.add(32); normalized.add(64);
        }
        Collections.sort(normalized);
        return Collections.unmodifiableList(normalized);
    }

    public String render(String template, Map<String, String> replacements) {
        String output = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            output = output.replace("%" + entry.getKey().toLowerCase(Locale.ROOT) + "%", entry.getValue());
        }
        return output;
    }

    public String warpTitle() { return warpTitle; }
    public String warpContent() { return warpContent; }
    public String warpButton() { return warpButton; }
    public String noWarpsMessage() { return noWarpsMessage; }
    public String noWarpAccess() { return noWarpAccess; }
    public String teleportSuccess() { return teleportSuccess; }
    public String teleportFailed() { return teleportFailed; }

    public String kitTitle() { return kitTitle; }
    public String kitContent() { return kitContent; }
    public String kitButton() { return kitButton; }
    public String noKitsMessage() { return noKitsMessage; }
    public String noKitAccess() { return noKitAccess; }
    public String kitClaimSuccess() { return kitClaimSuccess; }
    public String kitClaimFailed() { return kitClaimFailed; }
    public String kitOnCooldown() { return kitOnCooldown; }

    public String backButton() { return backButton; }
    public String mainButton() { return mainButton; }
    public String previousButton() { return previousButton; }
    public String nextButton() { return nextButton; }

    public String homeTitle() { return homeTitle; }
    public String homeContent() { return homeContent; }
    public String homeButton() { return homeButton; }
    public String homeLimitText() { return homeLimitText; }
    public String noHomesMessage() { return noHomesMessage; }
    public String homeNotFound() { return homeNotFound; }
    public String homeTeleportSuccess() { return homeTeleportSuccess; }
    public String homeTeleportFailed() { return homeTeleportFailed; }
    public String homeLimitReached() { return homeLimitReached; }
    public String homeSetSuccess() { return homeSetSuccess; }
    public String homeSetFailed() { return homeSetFailed; }
    public String homeSetInvalid() { return homeSetInvalid; }
    public String homeDeleteButton() { return homeDeleteButton; }
    public String homeDeleteSuccess() { return homeDeleteSuccess; }
    public String homeNoDelete() { return homeNoDelete; }
    public String homeDeleteFailed() { return homeDeleteFailed; }
    public String homeProviderUnavailable() { return homeProviderUnavailable; }

    public String tpaTitle() { return tpaTitle; }
    public String tpaContent() { return tpaContent; }
    public String tpaPendingContent() { return tpaPendingContent; }
    public String tpaAcceptButton() { return tpaAcceptButton; }
    public String tpaDenyButton() { return tpaDenyButton; }
    public String tpaSendButton() { return tpaSendButton; }
    public String tpaHereButton() { return tpaHereButton; }
    public String tpaCancelButton() { return tpaCancelButton; }
    public String tpaNoPending() { return tpaNoPending; }
    public String tpaSendFailed() { return tpaSendFailed; }
    public String tpaTitleSend() { return tpaTitleSend; }
    public String tpaTitleHere() { return tpaTitleHere; }
    public String tpaSendContent() { return tpaSendContent; }
    public String tpaPlayerInputText() { return tpaPlayerInputText; }
    public String tpaPlayerInputPlaceholder() { return tpaPlayerInputPlaceholder; }
    public String tpaProviderUnavailable() { return tpaProviderUnavailable; }

    public String shopMainTitle() { return shopMainTitle; }
    public String shopMainContent() { return shopMainContent; }
    public String shopShopTitle() { return shopShopTitle; }
    public String shopShopContent() { return shopShopContent; }
    public String shopItemTitle() { return shopItemTitle; }
    public String shopItemContent() { return shopItemContent; }
    public String shopEmptyShopMessage() { return shopEmptyShopMessage; }
    public String shopEmptyPageMessage() { return shopEmptyPageMessage; }
    public String shopUnavailableItemMessage() { return shopUnavailableItemMessage; }
    public String shopOpenLinkedButton() { return shopOpenLinkedButton; }
    public String shopBuyLabel() { return shopBuyLabel; }
    public String shopSellLabel() { return shopSellLabel; }
    public String shopTradeLabel() { return shopTradeLabel; }
    public String shopDecorationLabel() { return shopDecorationLabel; }
    public String shopShopsNotReady() { return shopShopsNotReady; }
    public String shopNoShopAccess() { return shopNoShopAccess; }
    public String shopTransactionSuccess() { return shopTransactionSuccess; }
    public String shopTransactionFailed() { return shopTransactionFailed; }
    public String shopUnsupportedTransaction() { return shopUnsupportedTransaction; }
    public List<Integer> shopAmountPresets() { return shopAmountPresets; }
    public boolean shopRequirePurchaseConfirmation() { return shopRequirePurchaseConfirmation; }
    public String soundShopPurchaseSuccess() { return soundShopPurchaseSuccess; }
    public String soundShopPurchaseFailed() { return soundShopPurchaseFailed; }

    public String noBedrockGui() { return noBedrockGui; }
    public String essentialsNotReady() { return essentialsNotReady; }
    public String providerUnavailable() { return providerUnavailable; }

    public boolean soundsEnabled() { return soundsEnabled; }
    public String soundFormOpen() { return soundFormOpen; }
    public String soundTeleportSuccess() { return soundTeleportSuccess; }
    public String soundKitClaimSuccess() { return soundKitClaimSuccess; }
    public String soundActionFailed() { return soundActionFailed; }
    public float soundVolume() { return soundVolume; }
    public float soundPitch() { return soundPitch; }

    // Module toggles
    public boolean moduleWarps() { return moduleWarps; }
    public boolean moduleKits() { return moduleKits; }
    public boolean moduleHomes() { return moduleHomes; }
    public boolean moduleTpa() { return moduleTpa; }
    public boolean moduleShopGuiPlus() { return moduleShopGuiPlus; }
    public boolean moduleEconomyShopGui() { return moduleEconomyShopGui; }
    public boolean moduleMyPet() { return moduleMyPet; }

    // Actions-only
    public boolean actionsWarps() { return actionsWarps && !moduleWarps; }
    public boolean actionsKits() { return actionsKits && !moduleKits; }
    public boolean actionsHomes() { return actionsHomes && !moduleHomes; }
    public boolean actionsTpa() { return actionsTpa && !moduleTpa; }
    public boolean actionsShopGuiPlus() { return actionsShopGuiPlus && !moduleShopGuiPlus; }
    public boolean actionsEconomyShopGui() { return actionsEconomyShopGui && !moduleEconomyShopGui; }
    public boolean actionsMyPet() { return actionsMyPet && !moduleMyPet; }

    // Hub
    public String hubTitle() { return hubTitle; }
    public String hubContent() { return hubContent; }
    public String hubButtonWarps() { return hubButtonWarps; }
    public String hubButtonKits() { return hubButtonKits; }
    public String hubButtonHomes() { return hubButtonHomes; }
    public String hubButtonTpa() { return hubButtonTpa; }
    public String hubButtonShopGuiPlus() { return hubButtonShopGuiPlus; }
    public String hubButtonEconomyShopGui() { return hubButtonEconomyShopGui; }
    public String hubButtonMyPet() { return hubButtonMyPet; }

    public String petShopTitle() { return petShopTitle; }
    public String petShopContent() { return petShopContent; }
    public String petShopButton() { return petShopButton; }
    public String petShopOwnedSuffix() { return petShopOwnedSuffix; }
    public String petBuyConfirmTitle() { return petBuyConfirmTitle; }
    public String petBuyConfirmContent() { return petBuyConfirmContent; }
    public String petBuyConfirmYes() { return petBuyConfirmYes; }
    public String petBuyConfirmNo() { return petBuyConfirmNo; }
    public String petListTitle() { return petListTitle; }
    public String petListContent() { return petListContent; }
    public String petListButton() { return petListButton; }
    public String petActiveSuffix() { return petActiveSuffix; }
    public String petInfoTitle() { return petInfoTitle; }
    public String petInfoContent() { return petInfoContent; }
    public String petCallButton() { return petCallButton; }
    public String petPutAwayButton() { return petPutAwayButton; }
    public String petSkilltreeButton() { return petSkilltreeButton; }
    public String petSkilltreeTitle() { return petSkilltreeTitle; }
    public String petSkilltreeContent() { return petSkilltreeContent; }
    public String petSkilltreeOption() { return petSkilltreeOption; }
    public String petSkilltreeCurrentSuffix() { return petSkilltreeCurrentSuffix; }
    public String petNoPets() { return petNoPets; }
    public String petNoActivePet() { return petNoActivePet; }
    public String petNotReady() { return petNotReady; }
    public String petBuySuccess() { return petBuySuccess; }
    public String petBuyFailed() { return petBuyFailed; }
    public String petCannotAfford() { return petCannotAfford; }
    public String petCallSuccess() { return petCallSuccess; }
    public String petCallFailed() { return petCallFailed; }
    public String petPutAwaySuccess() { return petPutAwaySuccess; }
    public String petPutAwayFailed() { return petPutAwayFailed; }
    public String petSkilltreeSetSuccess() { return petSkilltreeSetSuccess; }
    public String petSkilltreeSetFailed() { return petSkilltreeSetFailed; }
}
