package it.pintux.life.essentialsaddon.provider;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public interface CrateProvider {

    String getProviderId();

    boolean isReady();

    Collection<String> getCrateNames();

    boolean hasAccess(Player player, String crateId);

    String getDisplayName(String crateId);

    String getDescription(String crateId);

    ItemStack getCrateIcon(String crateId);

    List<ItemStack> getPreviewContents(String crateId);

    boolean openPreview(Player player, String crateId);
}
