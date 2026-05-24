package it.pintux.life.essentialsaddon.provider;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kit;
import com.earth2me.essentials.Kits;
import com.earth2me.essentials.User;
import it.pintux.life.essentialsaddon.api.KitProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

@SuppressWarnings("unchecked")
public final class EssentialsXKitProvider implements KitProvider {

    private Essentials essentials;

    @Override
    public String getProviderId() {
        return "essentialsx";
    }

    @Override
    public boolean isReady() {
        if (essentials != null && essentials.isEnabled()) {
            return true;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Essentials");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            essentials = (Essentials) plugin;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Collection<String> getKitNames() {
        if (!isReady()) {
            return List.of();
        }
        try {
            Kits kits = essentials.getKits();
            if (kits == null) {
                return List.of();
            }
            return new ArrayList<>(kits.getKitKeys());
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getKitData(String kitName) throws Exception {
        Kits kits = essentials.getKits();
        return kits.getKit(kitName);
    }

    @Override
    public boolean hasAccess(Player player, String kitName) {
        if (!isReady()) {
            return false;
        }
        // EssentialsX kit permission: essentials.kits.<kitname> or essentials.kit.*
        if (player.hasPermission("essentials.kits.*") || player.hasPermission("essentials.kits." + kitName)) {
            return true;
        }
        // Fallback: essentials.kit.<kitname>
        if (player.hasPermission("essentials.kit." + kitName)) {
            return true;
        }
        return true;
    }

    @Override
    public boolean claimKit(Player player, String kitName) {
        if (!isReady()) {
            return false;
        }
        try {
            User user = essentials.getUser(player);
            if (user == null) {
                return false;
            }
            Kit kit = new Kit(kitName, essentials);
            return kit.expandItems(user);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getItemCount(String kitName) {
        if (!isReady()) {
            return -1;
        }
        try {
            Map<String, Object> kitData = getKitData(kitName);
            if (kitData == null) {
                return -1;
            }
            Object items = kitData.get("items");
            if (items instanceof List) {
                return ((List<?>) items).size();
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public long getCooldownSeconds(Player player, String kitName) {
        if (!isReady()) {
            return 0;
        }
        try {
            User user = essentials.getUser(player);
            if (user == null) {
                return 0;
            }
            long lastUse = user.getKitTimestamp(kitName);
            if (lastUse <= 0) {
                return 0;
            }
            Map<String, Object> kitData = getKitData(kitName);
            if (kitData == null) {
                return 0;
            }
            Object delayObj = kitData.get("delay");
            if (delayObj instanceof Number) {
                long delaySeconds = ((Number) delayObj).longValue();
                long elapsed = (System.currentTimeMillis() - lastUse) / 1000;
                long remaining = delaySeconds - elapsed;
                return Math.max(0, remaining);
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean isAvailable(Player player, String kitName) {
        return getCooldownSeconds(player, kitName) <= 0;
    }
}
