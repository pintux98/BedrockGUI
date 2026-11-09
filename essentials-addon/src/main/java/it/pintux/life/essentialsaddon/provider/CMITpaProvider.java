package it.pintux.life.essentialsaddon.provider;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Teleportations.TpInfo;
import com.Zrips.CMI.events.CMIPlayerTeleportRequestEvent;
import it.pintux.life.essentialsaddon.api.TpaProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CMITpaProvider implements TpaProvider {

    @Override
    public String getProviderId() {
        return "cmi";
    }

    @Override
    public boolean registerRequestListener(Plugin plugin, RequestListener listener) {
        try {
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
                public void onTpaRequest(CMIPlayerTeleportRequestEvent event) {
                    Player target = event.getWhoAccepts();
                    Player sender = event.getWhoOffers();
                    if (target == null || sender == null) {
                        return;
                    }
                    listener.onRequest(target, sender.getName());
                }
            }, plugin);
            return true;
        } catch (Exception | LinkageError failure) {
            return false;
        }
    }

    @Override
    public boolean isReady() {
        return Bukkit.getPluginManager().isPluginEnabled("CMI")
                && CMI.getInstance() != null
                && CMI.getInstance().getTeleportManager() != null;
    }

    @Override
    public boolean sendTpaRequest(Player sender, Player target) {
        return isReady() && sender.performCommand("cmi tpa " + target.getName()) && isRequested(sender, target);
    }

    @Override
    public boolean sendTpahereRequest(Player sender, Player target) {
        return isReady() && sender.performCommand("cmi tpahere " + target.getName()) && isRequested(sender, target);
    }

    @Override
    public boolean acceptTpa(Player target) {
        return isReady() && hasPendingRequest(target) && target.performCommand("cmi tpaccept");
    }

    @Override
    public boolean denyTpa(Player target) {
        return isReady() && hasPendingRequest(target) && target.performCommand("cmi tpdeny");
    }

    @Override
    public boolean cancelTpa(Player sender) {
        if (!isReady()) {
            return false;
        }
        boolean removed = false;
        for (TpInfo info : requests()) {
            if (sender.equals(info.getWhoOffers()) && info.getWhoAccepts() != null) {
                removed |= CMI.getInstance().getTeleportManager().removeTpRequest(sender, info.getWhoAccepts());
            }
        }
        return removed;
    }

    @Override
    public List<String> getPendingRequests(Player player) {
        List<String> senders = new ArrayList<>();
        for (TpInfo info : requests()) {
            if (player.equals(info.getWhoAccepts()) && info.getWhoOffers() != null) {
                senders.add(info.getWhoOffers().getName());
            }
        }
        return senders;
    }

    @Override
    public boolean hasPendingRequest(Player player) {
        return !getPendingRequests(player).isEmpty();
    }

    @Override
    public String getPendingRequestSender(Player player) {
        List<String> requests = getPendingRequests(player);
        return requests.isEmpty() ? null : requests.get(0);
    }

    private boolean isRequested(Player sender, Player target) {
        for (TpInfo info : requests()) {
            if (sender.equals(info.getWhoOffers()) && target.equals(info.getWhoAccepts())) {
                return true;
            }
        }
        return false;
    }

    private List<TpInfo> requests() {
        if (!isReady()) {
            return List.of();
        }
        List<TpInfo> all = new ArrayList<>();
        for (Set<TpInfo> set : CMI.getInstance().getTeleportManager().getTeleportRequests().values()) {
            if (set != null) {
                all.addAll(set);
            }
        }
        return all;
    }
}
