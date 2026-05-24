package it.pintux.life.essentialsaddon.provider;

import it.pintux.life.essentialsaddon.api.TpaProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

public final class CMITpaProvider implements TpaProvider {

    private Object cmiInstance;
    private Method getTeleportManagerMethod;

    @Override
    public String getProviderId() {
        return "cmi";
    }

    @Override
    public boolean isReady() {
        if (cmiInstance != null) {
            return true;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CMI");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            cmiInstance = plugin;
            getTeleportManagerMethod = cmiInstance.getClass().getMethod("getTeleportManager");
            return getTeleportManagerMethod.invoke(cmiInstance) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean sendTpaRequest(Player sender, Player target) {
        if (!isReady()) return false;
        try {
            Object teleportManager = getTeleportManagerMethod.invoke(cmiInstance);
            Method tpa = teleportManager.getClass().getMethod("tpa", Player.class, Player.class, boolean.class);
            tpa.invoke(teleportManager, sender, target, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean sendTpahereRequest(Player sender, Player target) {
        if (!isReady()) return false;
        try {
            Object teleportManager = getTeleportManagerMethod.invoke(cmiInstance);
            Method tpahere = teleportManager.getClass().getMethod("tpahere", Player.class, Player.class, boolean.class);
            tpahere.invoke(teleportManager, sender, target, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean acceptTpa(Player target) {
        if (!isReady()) return false;
        try {
            Object teleportManager = getTeleportManagerMethod.invoke(cmiInstance);
            Method accept = teleportManager.getClass().getMethod("tpAccept", Player.class);
            accept.invoke(teleportManager, target);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean denyTpa(Player target) {
        if (!isReady()) return false;
        try {
            Object teleportManager = getTeleportManagerMethod.invoke(cmiInstance);
            Method deny = teleportManager.getClass().getMethod("tpDeny", Player.class);
            deny.invoke(teleportManager, target);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean cancelTpa(Player sender) {
        if (!isReady()) return false;
        try {
            Object teleportManager = getTeleportManagerMethod.invoke(cmiInstance);
            Method cancel = teleportManager.getClass().getMethod("tpCancel", Player.class);
            cancel.invoke(teleportManager, sender);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<String> getPendingRequests(Player player) {
        if (!isReady()) return List.of();
        try {
            Object teleportManager = getTeleportManagerMethod.invoke(cmiInstance);
            Method getRequests = teleportManager.getClass().getMethod("getPendingTeleports", Player.class);
            Object requests = getRequests.invoke(teleportManager, player);
            if (requests instanceof Collection) {
                List<String> names = new ArrayList<>();
                for (Object req : (Collection<?>) requests) {
                    Method getSource = req.getClass().getMethod("getSource");
                    Object source = getSource.invoke(req);
                    if (source instanceof Player) {
                        names.add(((Player) source).getName());
                    }
                }
                return names;
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
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
}
