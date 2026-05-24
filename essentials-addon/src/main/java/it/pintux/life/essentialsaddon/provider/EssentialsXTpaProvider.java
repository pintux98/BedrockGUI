package it.pintux.life.essentialsaddon.provider;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import it.pintux.life.essentialsaddon.api.TpaProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class EssentialsXTpaProvider implements TpaProvider {

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
    public boolean sendTpaRequest(Player sender, Player target) {
        if (!isReady()) return false;
        try {
            essentials.getPluginCommand("tpa").execute(sender, "tpa", new String[]{target.getName()});
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean sendTpahereRequest(Player sender, Player target) {
        if (!isReady()) return false;
        try {
            essentials.getPluginCommand("tpahere").execute(sender, "tpahere", new String[]{target.getName()});
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean acceptTpa(Player target) {
        if (!isReady()) return false;
        try {
            essentials.getPluginCommand("tpaccept").execute(target, "tpaccept", new String[]{});
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean denyTpa(Player target) {
        if (!isReady()) return false;
        try {
            essentials.getPluginCommand("tpdeny").execute(target, "tpdeny", new String[]{});
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean cancelTpa(Player sender) {
        if (!isReady()) return false;
        try {
            essentials.getPluginCommand("tpacancel").execute(sender, "tpacancel", new String[]{});
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> getPendingRequests(Player player) {
        if (!isReady()) return List.of();
        try {
            User user = essentials.getUser(player);
            if (user == null) return List.of();
            java.util.Collection<String> keys = user.getPendingTpaKeys();
            return keys == null ? List.of() : new ArrayList<>(keys);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean hasPendingRequest(Player player) {
        if (!isReady()) return false;
        try {
            User user = essentials.getUser(player);
            if (user == null) return false;
            return user.hasPendingTpaRequests(false, false);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getPendingRequestSender(Player player) {
        List<String> requests = getPendingRequests(player);
        return requests.isEmpty() ? null : requests.get(0);
    }
}
