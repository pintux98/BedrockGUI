package it.pintux.life.essentialsaddon.provider;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("unchecked")
public final class ExcellentCratesCrateProvider implements CrateProvider {

    private Object xCratesInstance;
    private Method getCrateManagerMethod;

    @Override
    public String getProviderId() {
        return "excellentcrates";
    }

    @Override
    public boolean isReady() {
        if (xCratesInstance != null) {
            return true;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ExcellentCrates");
        if (plugin == null || !plugin.isEnabled()) {
            return false;
        }
        try {
            xCratesInstance = plugin;
            getCrateManagerMethod = xCratesInstance.getClass().getMethod("getCrateManager");
            Object crateManager = getCrateManagerMethod.invoke(xCratesInstance);
            return crateManager != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Collection<String> getCrateNames() {
        if (!isReady()) {
            return List.of();
        }
        try {
            Object crateManager = getCrateManagerMethod.invoke(xCratesInstance);
            Method getCrates = crateManager.getClass().getMethod("getCrates");
            Object cratesMap = getCrates.invoke(crateManager);
            if (cratesMap instanceof Map) {
                return new ArrayList<>(((Map<?, ?>) cratesMap).keySet().stream()
                        .map(Object::toString)
                        .toList());
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean hasAccess(Player player, String crateId) {
        if (!isReady()) {
            return false;
        }
        try {
            Object crateManager = getCrateManagerMethod.invoke(xCratesInstance);
            Method getCrate = crateManager.getClass().getMethod("getCrate", String.class);
            Object crate = getCrate.invoke(crateManager, crateId);
            if (crate == null) return false;
            Method getPermission = crate.getClass().getMethod("getPermission");
            String permission = (String) getPermission.invoke(crate);
            if (permission != null && !permission.isEmpty()) {
                return player.hasPermission(permission);
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public String getDisplayName(String crateId) {
        if (!isReady()) return crateId;
        try {
            Object crateManager = getCrateManagerMethod.invoke(xCratesInstance);
            Method getCrate = crateManager.getClass().getMethod("getCrate", String.class);
            Object crate = getCrate.invoke(crateManager, crateId);
            if (crate == null) return crateId;
            Method getName = crate.getClass().getMethod("getName");
            String name = (String) getName.invoke(crate);
            return name != null && !name.isEmpty() ? name : crateId;
        } catch (Exception e) {
            return crateId;
        }
    }

    @Override
    public String getDescription(String crateId) {
        if (!isReady()) return "";
        try {
            Object crateManager = getCrateManagerMethod.invoke(xCratesInstance);
            Method getCrate = crateManager.getClass().getMethod("getCrate", String.class);
            Object crate = getCrate.invoke(crateManager, crateId);
            if (crate == null) return "";
            Method getDescription = crate.getClass().getMethod("getDescription");
            String desc = (String) getDescription.invoke(crate);
            return desc != null ? desc : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public ItemStack getCrateIcon(String crateId) {
        if (!isReady()) return null;
        try {
            Object crateManager = getCrateManagerMethod.invoke(xCratesInstance);
            Method getCrate = crateManager.getClass().getMethod("getCrate", String.class);
            Object crate = getCrate.invoke(crateManager, crateId);
            if (crate == null) return null;
            Method getIcon = crate.getClass().getMethod("getIcon");
            return (ItemStack) getIcon.invoke(crate);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<ItemStack> getPreviewContents(String crateId) {
        if (!isReady()) return List.of();
        try {
            Object crateManager = getCrateManagerMethod.invoke(xCratesInstance);
            Method getCrate = crateManager.getClass().getMethod("getCrate", String.class);
            Object crate = getCrate.invoke(crateManager, crateId);
            if (crate == null) return List.of();
            Method getRewards = crate.getClass().getMethod("getRewards");
            Object rewards = getRewards.invoke(crate);
            if (rewards instanceof List) {
                List<ItemStack> items = new ArrayList<>();
                for (Object reward : (List<?>) rewards) {
                    Method getItem = reward.getClass().getMethod("getItem");
                    ItemStack item = (ItemStack) getItem.invoke(reward);
                    if (item != null) items.add(item);
                }
                return items;
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean openPreview(Player player, String crateId) {
        if (!isReady()) return false;
        try {
            Object crateManager = getCrateManagerMethod.invoke(xCratesInstance);
            Method openPreview = crateManager.getClass().getMethod("openPreview", Player.class, String.class);
            openPreview.invoke(crateManager, player, crateId);
            return true;
        } catch (Exception e) {
            try {
                Object crateManager = getCrateManagerMethod.invoke(xCratesInstance);
                Method getCrate = crateManager.getClass().getMethod("getCrate", String.class);
                Object crate = getCrate.invoke(crateManager, crateId);
                if (crate == null) return false;
                Method openPreview = crate.getClass().getMethod("openPreview", Player.class);
                openPreview.invoke(crate, player);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
}
