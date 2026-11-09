package it.pintux.life.essentialsaddon.provider;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIKitUsage;
import com.Zrips.CMI.Modules.Kits.Kit;
import com.Zrips.CMI.Modules.Kits.KitsManager;
import it.pintux.life.essentialsaddon.api.KitProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CMIKitProvider implements KitProvider {

    @Override
    public String getProviderId() {
        return "cmi";
    }

    @Override
    public boolean isReady() {
        return Bukkit.getPluginManager().isPluginEnabled("CMI")
                && CMI.getInstance() != null
                && CMI.getInstance().getKitsManager() != null;
    }

    @Override
    public Collection<String> getKitNames() {
        if (!isReady()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Kit kit : CMI.getInstance().getKitsManager().getKitMap().values()) {
            if (kit != null && kit.isEnabled()) {
                names.add(kit.getConfigName());
            }
        }
        return names;
    }

    @Override
    public boolean hasAccess(Player player, String kitName) {
        Kit kit = kit(kitName);
        return kit != null && (player.hasPermission("cmi.kit.*")
                || player.hasPermission("cmi.kit." + kit.getConfigName().toLowerCase(Locale.ROOT)));
    }

    @Override
    public boolean claimKit(Player player, String kitName) {
        Kit kit = kit(kitName);
        if (kit == null) {
            return false;
        }
        Long before = lastUsage(player, kit);
        boolean ran = player.performCommand("cmi kit " + kit.getCommandName());
        Long after = lastUsage(player, kit);
        return ran && after != null && !after.equals(before);
    }

    @Override
    public String getDisplayName(String kitName) {
        Kit kit = kit(kitName);
        if (kit == null || kit.getDisplayName() == null || kit.getDisplayName().isBlank()) {
            return kitName;
        }
        return kit.getDisplayName();
    }

    @Override
    public int getItemCount(String kitName) {
        Kit kit = kit(kitName);
        if (kit == null || kit.getItems() == null) {
            return -1;
        }
        int count = 0;
        for (ItemStack item : kit.getItems()) {
            if (item != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public long getCooldownSeconds(Player player, String kitName) {
        Kit kit = kit(kitName);
        if (kit == null || kit.getDelay() <= 0) {
            return 0;
        }
        Long last = lastUsage(player, kit);
        if (last == null) {
            return 0;
        }
        long remaining = last + kit.getDelay() * 1000L - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (remaining + 999) / 1000;
    }

    @Override
    public boolean isAvailable(Player player, String kitName) {
        Kit kit = kit(kitName);
        if (kit == null) {
            return false;
        }
        CMIKitUsage usage = usage(player, kit);
        if (kit.getMaxUsages() > 0 && usage != null && usage.getUsedTimes() >= kit.getMaxUsages()) {
            return false;
        }
        return getCooldownSeconds(player, kitName) == 0;
    }

    private Kit kit(String kitName) {
        if (kitName == null || !isReady()) {
            return null;
        }
        KitsManager manager = CMI.getInstance().getKitsManager();
        Kit kit = manager.getKit(kitName);
        if (kit != null) {
            return kit;
        }
        for (Kit candidate : manager.getKitMap().values()) {
            if (candidate != null && candidate.getConfigName().equalsIgnoreCase(kitName)) {
                return candidate;
            }
        }
        return null;
    }

    private Long lastUsage(Player player, Kit kit) {
        CMIKitUsage usage = usage(player, kit);
        return usage == null ? null : usage.getLastUsage();
    }

    private CMIKitUsage usage(Player player, Kit kit) {
        Map<String, CMIKitUsage> usages = KitsManager.getUserKits(player.getUniqueId());
        if (usages == null) {
            return null;
        }
        for (Map.Entry<String, CMIKitUsage> entry : usages.entrySet()) {
            CMIKitUsage usage = entry.getValue();
            if (usage == null) {
                continue;
            }
            if (entry.getKey().equalsIgnoreCase(kit.getConfigName())
                    || (usage.getKitName() != null && usage.getKitName().equalsIgnoreCase(kit.getConfigName()))) {
                return usage;
            }
        }
        return null;
    }
}
