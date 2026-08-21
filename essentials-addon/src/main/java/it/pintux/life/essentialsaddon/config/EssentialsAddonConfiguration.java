package it.pintux.life.essentialsaddon.config;

import it.pintux.life.essentialsaddon.provider.ProviderSelector;
import it.pintux.life.essentialsaddon.util.CommandAliases;
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
    private final boolean moduleDeathMenu;
    private final boolean homeManageMenu;
    private final boolean homePublicHomes;
    private final boolean homePrivacy;

    // Intercepted commands
    private final CommandAliases commandWarps;
    private final CommandAliases commandKits;
    private final CommandAliases commandHomes;
    private final CommandAliases commandPublicHomes;
    private final CommandAliases commandSetHome;
    private final CommandAliases commandDeleteHome;
    private final CommandAliases commandTpa;
    private final CommandAliases commandPets;
    private final CommandAliases commandPetShop;
    private final CommandAliases commandPetSkilltree;
    private final CommandAliases commandShop;
    private final CommandAliases commandSellAll;

    // Provider preferences
    private final String providerWarps;
    private final String providerKits;
    private final String providerHomes;
    private final String providerTpa;

    // Incoming-TPA popup
    private final boolean tpaRequestPopupEnabled;

    // Actions-only (register actions without internal forms)
    private final boolean actionsWarps;
    private final boolean actionsKits;
    private final boolean actionsHomes;
    private final boolean actionsTpa;
    private final boolean actionsShopGuiPlus;
    private final boolean actionsEconomyShopGui;
    private final boolean actionsMyPet;

    // Global master switches (apply on top of the per-module toggles above)
    private final boolean integratedGui;
    private final boolean registerActions;

    // Hub
    private final String hubTitle;
    private final String hubContent;
    private final String hubButtonWarps;
    private final String hubButtonKits;
    private final String hubButtonHomes;
    private final String hubButtonTpa;
    private final String hubButtonPublicHomes;
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
    private final String homeNameInputText;
    private final String homeNameInputPlaceholder;
    private final String homeManageButton;
    private final String homeManageTitle;
    private final String homeManageContent;
    private final String homeManageTeleportButton;
    private final String homeManageDeleteButton;
    private final String homePublicSuffix;
    private final String homeMakePublicButton;
    private final String homeMakePrivateButton;
    private final String homeSetPublicToggle;
    private final String homePrivacyPublicSuccess;
    private final String homePrivacyPrivateSuccess;
    private final String homePrivacyFailed;
    private final String publicHomeTitle;
    private final String publicHomeContent;
    private final String publicHomeButton;
    private final String noPublicHomesMessage;
    private final String publicHomeTeleportSuccess;
    private final String publicHomeTeleportFailed;
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
    private final String tpaRequestTitle;
    private final String tpaRequestContent;

    private final String deathTitle;
    private final String deathContent;
    private final String deathBackButton;
    private final String deathSpawnButton;
    private final String deathCloseButton;
    private final String deathBackCommand;
    private final String deathSpawnCommand;
    private final String deathBackPermission;
    private final boolean deathShowBack;
    private final boolean deathShowSpawn;
    private final long deathFormDelayTicks;

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
        this.moduleDeathMenu = configuration.getBoolean("modules.death-menu", false);
        this.homeManageMenu = configuration.getBoolean("homes.manage-menu", true);
        this.homePublicHomes = configuration.getBoolean("homes.public-homes", true);
        this.homePrivacy = configuration.getBoolean("homes.privacy", true);

        this.commandWarps = aliases(configuration, "commands.warps", "warp", "warps", "warplist");
        this.commandKits = aliases(configuration, "commands.kits", "kit", "kits");
        this.commandHomes = aliases(configuration, "commands.homes", "home", "homes", "homelist");
        this.commandPublicHomes = aliases(configuration, "commands.public-homes",
                "phome", "phomes", "phomelist", "publichome", "publichomelist");
        this.commandSetHome = aliases(configuration, "commands.set-home", "sethome", "createhome");
        this.commandDeleteHome = aliases(configuration, "commands.delete-home", "delhome", "deletehome", "remhome");
        this.commandTpa = aliases(configuration, "commands.tpa", "tpa", "tpahere", "tpaccept", "tpyes",
                "tpdeny", "tpdecline", "tpno", "tpacancel", "tpcancel");
        this.commandPets = aliases(configuration, "commands.pets", "pet", "pets");
        this.commandPetShop = aliases(configuration, "commands.pet-shop", "petshop");
        this.commandPetSkilltree = aliases(configuration, "commands.pet-skilltree", "pcst", "petskilltree");
        this.commandShop = aliases(configuration, "commands.shop", "shop", "shopgui", "guishop");
        this.commandSellAll = aliases(configuration, "commands.sell-all", "sellall", "sell");

        this.providerWarps = providerName(configuration, "providers.warps");
        this.providerKits = providerName(configuration, "providers.kits");
        this.providerHomes = providerName(configuration, "providers.homes");
        this.providerTpa = providerName(configuration, "providers.tpa");

        this.tpaRequestPopupEnabled = configuration.getBoolean("tpa-request-popup.enabled", true);

        this.actionsWarps = configuration.getBoolean("actions-only.warps", false);
        this.actionsKits = configuration.getBoolean("actions-only.kits", false);
        this.actionsHomes = configuration.getBoolean("actions-only.homes", false);
        this.actionsTpa = configuration.getBoolean("actions-only.tpa", false);
        this.actionsShopGuiPlus = configuration.getBoolean("actions-only.shopgui-plus", false);
        this.actionsEconomyShopGui = configuration.getBoolean("actions-only.economyshop-gui", false);
        this.actionsMyPet = configuration.getBoolean("actions-only.mypet", false);

        this.integratedGui = configuration.getBoolean("integrated-gui", true);
        this.registerActions = configuration.getBoolean("register-actions", true);

        this.hubTitle = color(configuration.getString("hub.title", "&6&lEssentials Menu"));
        this.hubContent = color(configuration.getString("hub.content", "&7Select a feature to use."));
        this.hubButtonWarps = color(configuration.getString("hub.button-warps", "&b&lWarps"));
        this.hubButtonKits = color(configuration.getString("hub.button-kits", "&6&lKits"));
        this.hubButtonHomes = color(configuration.getString("hub.button-homes", "&a&lHomes"));
        this.hubButtonTpa = color(configuration.getString("hub.button-tpa", "&e&lTeleport"));
        this.hubButtonPublicHomes = color(configuration.getString("hub.button-public-homes", "&a&lPublic Homes"));
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

        this.backButton = color(configuration.getString("ui.back-button", "&0Back"));
        this.mainButton = color(configuration.getString("ui.main-button", "&0Main Menu"));
        this.previousButton = color(configuration.getString("ui.previous-button", "&0Previous Page"));
        this.nextButton = color(configuration.getString("ui.next-button", "&0Next Page"));

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
        this.homeNameInputText = color(configuration.getString("ui.home-name-input-text", "&7Enter a name for this home:"));
        this.homeNameInputPlaceholder = configuration.getString("ui.home-name-input-placeholder", "home");
        this.homeManageButton = color(configuration.getString("ui.home-manage-button", "&0Manage Homes"));
        this.homeManageTitle = color(configuration.getString("ui.home-manage-title", "&bManage Homes"));
        this.homeManageContent = color(configuration.getString("ui.home-manage-content", "&7Select a home to manage."));
        this.homeManageTeleportButton = color(configuration.getString("ui.home-manage-teleport-button", "&aTeleport"));
        this.homeManageDeleteButton = color(configuration.getString("ui.home-manage-delete-button", "&cDelete Home"));
        this.homePublicSuffix = color(configuration.getString("ui.home-public-suffix", " &a(Public)"));
        this.homeMakePublicButton = color(configuration.getString("ui.home-make-public-button", "&aMake Public"));
        this.homeMakePrivateButton = color(configuration.getString("ui.home-make-private-button", "&6Make Private"));
        this.homeSetPublicToggle = color(configuration.getString("ui.home-set-public-toggle", "&7Make this home public"));
        this.homePrivacyPublicSuccess = color(configuration.getString("ui.home-privacy-public-success", "&aHome '%home_name%' is now public."));
        this.homePrivacyPrivateSuccess = color(configuration.getString("ui.home-privacy-private-success", "&aHome '%home_name%' is now private."));
        this.homePrivacyFailed = color(configuration.getString("ui.home-privacy-failed", "&cFailed to change the privacy of %home_name%."));
        this.publicHomeTitle = color(configuration.getString("ui.public-home-title", "&bPublic Homes"));
        this.publicHomeContent = color(configuration.getString("ui.public-home-content", "&7Select a public home to teleport to."));
        this.publicHomeButton = color(configuration.getString("ui.public-home-button", "&e%home_name% &7by &f%owner%"));
        this.noPublicHomesMessage = color(configuration.getString("ui.no-public-homes-message", "&cThere are no public homes right now."));
        this.publicHomeTeleportSuccess = color(configuration.getString("ui.public-home-teleport-success", "&aTeleported to %home_name%."));
        this.publicHomeTeleportFailed = color(configuration.getString("ui.public-home-teleport-failed", "&cFailed to teleport to %home_name%"));
        this.homeProviderUnavailable = color(configuration.getString("ui.home-provider-unavailable", "&cHome provider is not available."));

        this.tpaTitle = color(configuration.getString("ui.tpa-title", "&bTeleport Requests"));
        this.tpaContent = color(configuration.getString("ui.tpa-content", "&7Manage your teleport requests."));
        this.tpaPendingContent = color(configuration.getString("ui.tpa-pending-content", "&7Pending from: &f%players%"));
        this.tpaAcceptButton = color(configuration.getString("ui.tpa-accept-button", "&aAccept Request"));
        this.tpaDenyButton = color(configuration.getString("ui.tpa-deny-button", "&cDeny Request"));
        this.tpaSendButton = color(configuration.getString("ui.tpa-send-button", "&eSend TPA"));
        this.tpaHereButton = color(configuration.getString("ui.tpa-here-button", "&6Send TPAHere"));
        this.tpaCancelButton = color(configuration.getString("ui.tpa-cancel-button", "&0Cancel Request"));
        this.tpaNoPending = color(configuration.getString("ui.tpa-no-pending", "&cNo pending requests."));
        this.tpaSendFailed = color(configuration.getString("ui.tpa-send-failed", "&cFailed to send request."));
        this.tpaTitleSend = color(configuration.getString("ui.tpa-title-send", "&eTPA - Select Player"));
        this.tpaTitleHere = color(configuration.getString("ui.tpa-title-here", "&6TPAHere - Select Player"));
        this.tpaSendContent = color(configuration.getString("ui.tpa-send-content", "&7Select a player to request teleport."));
        this.tpaPlayerInputText = color(configuration.getString("ui.tpa-player-input-text", "&7Enter player name:"));
        this.tpaPlayerInputPlaceholder = configuration.getString("ui.tpa-player-input-placeholder", "PlayerName");
        this.tpaProviderUnavailable = color(configuration.getString("ui.tpa-provider-unavailable", "&cTPA provider is not available."));
        this.tpaRequestTitle = color(configuration.getString("ui.tpa-request-title", "&bTeleport Request"));
        this.tpaRequestContent = color(configuration.getString("ui.tpa-request-content", "&f%player% &7wants to teleport. Accept?"));

        this.deathTitle = color(configuration.getString("ui.death-title", "&4&lYou Died"));
        this.deathContent = color(configuration.getString("ui.death-content", "&7Where do you want to go?"));
        this.deathBackButton = color(configuration.getString("ui.death-back-button", "&cReturn to death point"));
        this.deathSpawnButton = color(configuration.getString("ui.death-spawn-button", "&aTeleport to spawn"));
        this.deathCloseButton = color(configuration.getString("ui.death-close-button", "&0Stay here"));
        this.deathBackCommand = configuration.getString("ui.death-back-command", "back");
        this.deathSpawnCommand = configuration.getString("ui.death-spawn-command", "spawn");
        this.deathBackPermission = configuration.getString("ui.death-back-permission", "essentials.back");
        this.deathShowBack = configuration.getBoolean("ui.death-show-back", true);
        this.deathShowSpawn = configuration.getBoolean("ui.death-show-spawn", true);
        this.deathFormDelayTicks = Math.max(1L, configuration.getLong("ui.death-form-delay-ticks", 1L));

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
        this.providerUnavailable = color(configuration.getString("messages.provider-unavailable", "&cNo compatible provider is available for this feature."));

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

    private static CommandAliases aliases(YamlConfiguration configuration, String path, String... fallback) {
        if (!configuration.contains(path)) {
            return CommandAliases.of(fallback);
        }
        return CommandAliases.of(configuration.getStringList(path));
    }

    private static String providerName(YamlConfiguration configuration, String path) {
        String raw = configuration.getString(path, ProviderSelector.AUTO);
        return raw == null || raw.trim().isEmpty() ? ProviderSelector.AUTO : raw.trim();
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
    public String homeNameInputText() { return homeNameInputText; }
    public String homeNameInputPlaceholder() { return homeNameInputPlaceholder; }
    public String homeManageButton() { return homeManageButton; }
    public String homeManageTitle() { return homeManageTitle; }
    public String homeManageContent() { return homeManageContent; }
    public String homeManageTeleportButton() { return homeManageTeleportButton; }
    public String homeManageDeleteButton() { return homeManageDeleteButton; }
    public String homePublicSuffix() { return homePublicSuffix; }
    public String homeMakePublicButton() { return homeMakePublicButton; }
    public String homeMakePrivateButton() { return homeMakePrivateButton; }
    public String homeSetPublicToggle() { return homeSetPublicToggle; }
    public String homePrivacyPublicSuccess() { return homePrivacyPublicSuccess; }
    public String homePrivacyPrivateSuccess() { return homePrivacyPrivateSuccess; }
    public String homePrivacyFailed() { return homePrivacyFailed; }
    public String publicHomeTitle() { return publicHomeTitle; }
    public String publicHomeContent() { return publicHomeContent; }
    public String publicHomeButton() { return publicHomeButton; }
    public String noPublicHomesMessage() { return noPublicHomesMessage; }
    public String publicHomeTeleportSuccess() { return publicHomeTeleportSuccess; }
    public String publicHomeTeleportFailed() { return publicHomeTeleportFailed; }
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
    public String tpaRequestTitle() { return tpaRequestTitle; }
    public String tpaRequestContent() { return tpaRequestContent; }
    public boolean tpaRequestPopupEnabled() { return tpaRequestPopupEnabled; }

    public String deathTitle() { return deathTitle; }
    public String deathContent() { return deathContent; }
    public String deathBackButton() { return deathBackButton; }
    public String deathSpawnButton() { return deathSpawnButton; }
    public String deathCloseButton() { return deathCloseButton; }
    public String deathBackCommand() { return deathBackCommand; }
    public String deathSpawnCommand() { return deathSpawnCommand; }
    public String deathBackPermission() { return deathBackPermission; }
    public boolean deathShowBack() { return deathShowBack; }
    public boolean deathShowSpawn() { return deathShowSpawn; }
    public long deathFormDelayTicks() { return deathFormDelayTicks; }

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
    public boolean moduleDeathMenu() { return moduleDeathMenu; }

    // Home extras
    public boolean homeManageMenuEnabled() { return homeManageMenu; }
    public boolean homePublicHomesEnabled() { return homePublicHomes; }
    public boolean homePrivacyEnabled() { return homePrivacy; }

    // Intercepted commands
    public CommandAliases commandWarps() { return commandWarps; }
    public CommandAliases commandKits() { return commandKits; }
    public CommandAliases commandHomes() { return commandHomes; }
    public CommandAliases commandPublicHomes() { return commandPublicHomes; }
    public CommandAliases commandSetHome() { return commandSetHome; }
    public CommandAliases commandDeleteHome() { return commandDeleteHome; }
    public CommandAliases commandTpa() { return commandTpa; }
    public CommandAliases commandPets() { return commandPets; }
    public CommandAliases commandPetShop() { return commandPetShop; }
    public CommandAliases commandPetSkilltree() { return commandPetSkilltree; }
    public CommandAliases commandShop() { return commandShop; }
    public CommandAliases commandSellAll() { return commandSellAll; }

    // Provider preferences
    public String providerWarps() { return providerWarps; }
    public String providerKits() { return providerKits; }
    public String providerHomes() { return providerHomes; }
    public String providerTpa() { return providerTpa; }

    // Actions-only
    public boolean actionsWarps() { return actionsWarps && !moduleWarps; }
    public boolean actionsKits() { return actionsKits && !moduleKits; }
    public boolean actionsHomes() { return actionsHomes && !moduleHomes; }
    public boolean actionsTpa() { return actionsTpa && !moduleTpa; }
    public boolean actionsShopGuiPlus() { return actionsShopGuiPlus && !moduleShopGuiPlus; }
    public boolean actionsEconomyShopGui() { return actionsEconomyShopGui && !moduleEconomyShopGui; }
    public boolean actionsMyPet() { return actionsMyPet && !moduleMyPet; }

    /** Master switch: serve built-in forms and intercept commands/menus. */
    public boolean integratedGuiEnabled() { return integratedGui; }
    /** Master switch: register action handlers so other forms can drive Essentials. */
    public boolean registerActionsEnabled() { return registerActions; }

    // Hub
    public String hubTitle() { return hubTitle; }
    public String hubContent() { return hubContent; }
    public String hubButtonWarps() { return hubButtonWarps; }
    public String hubButtonKits() { return hubButtonKits; }
    public String hubButtonHomes() { return hubButtonHomes; }
    public String hubButtonTpa() { return hubButtonTpa; }
    public String hubButtonPublicHomes() { return hubButtonPublicHomes; }
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
