package it.pintux.life.duelsaddon.listener;

import com.phoenixplugins.phoenixduels.lib.common.uicomponents.newest.layout.ContainerLayout;
import com.phoenixplugins.phoenixduels.registry.MenuRegistry;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps an open PhoenixDuels layout back to the {@code MenuRegistry} key that registered it.
 *
 * <p>{@code ContainerLayout.getId()} looks like it would answer this, but it does not: the
 * constructor assigns {@code String.valueOf(DEFAULT_INCREMENT.getAndIncrement())}, so it is a
 * per-JVM counter — {@code "0"}, {@code "1"}, {@code "2"} — with no relation to the registry key.
 * Comparing it against {@code "party"} never matches.</p>
 *
 * <p>{@code MenuRegistry.getMenuOrNull(key)} does hold the real mapping, and returns the single
 * registered layout instance per key, so identity comparison recovers the key exactly. The map is
 * built lazily and rebuilt when an unrecognised layout appears, which is what makes it survive
 * {@code /pduels reload} recreating every menu.</p>
 */
public final class PhoenixMenuResolver {

    private final Map<ContainerLayout, String> known = new IdentityHashMap<>();
    private final Set<ContainerLayout> unresolvable = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * @return the registry key for this layout, or {@code null} if PhoenixDuels did not register it
     *         under any key this addon knows about
     */
    public String keyFor(ContainerLayout layout) {
        if (layout == null) {
            return null;
        }
        String key = known.get(layout);
        if (key != null) {
            return key;
        }
        if (unresolvable.contains(layout)) {
            return null;
        }
        refresh();
        key = known.get(layout);
        if (key == null) {
            unresolvable.add(layout);
        }
        return key;
    }

    /**
     * Re-reads every id this addon knows about out of {@link MenuRegistry}.
     *
     * @return how many ids the registry currently resolves, so startup and {@code /duelsaddon
     *         menus} can report whether the hook is really live
     */
    public int refresh() {
        known.clear();
        unresolvable.clear();
        try {
            MenuRegistry registry = MenuRegistry.getInstance();
            if (registry == null) {
                return 0;
            }
            for (String id : DuelsMenus.ALL) {
                ContainerLayout layout = registry.getMenuOrNull(id);
                if (layout != null) {
                    known.put(layout, id);
                }
            }
        } catch (Throwable ignored) {
            return known.size();
        }
        return known.size();
    }

    /**
     * @return how many layouts are currently mapped, without touching PhoenixDuels
     */
    public int mappedCount() {
        return known.size();
    }
}
