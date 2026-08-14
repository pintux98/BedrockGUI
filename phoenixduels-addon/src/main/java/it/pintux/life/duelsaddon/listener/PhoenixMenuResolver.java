package it.pintux.life.duelsaddon.listener;

import com.phoenixplugins.phoenixduels.lib.common.uicomponents.newest.layout.ContainerLayout;
import com.phoenixplugins.phoenixduels.registry.MenuRegistry;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Maps an open PhoenixDuels layout back to the {@code MenuRegistry} key that registered it.
 *
 * <p>{@code ContainerLayout.getId()} looks like it would answer this, but it does not: the
 * constructor assigns {@code String.valueOf(DEFAULT_INCREMENT.getAndIncrement())}, so it is a
 * per-JVM counter — {@code "0"}, {@code "1"}, {@code "2"} — with no relation to the registry key.</p>
 *
 * <p>{@code MenuRegistry.getMenuOrNull(key)} is a plain map lookup that does hold the real mapping,
 * so identity comparison recovers the key. Two keys can share one layout instance, so candidates
 * are walked in the fixed order of {@link DuelsMenus#PRIORITY} and the first match wins — iterating
 * an unordered set here would pick a different winner per JVM run, since {@code Set.of} randomises
 * iteration order.</p>
 *
 * <p>The map is built lazily and rebuilt when an unrecognised layout appears, which is what makes
 * it survive {@code /pduels reload} recreating every menu.</p>
 */
public final class PhoenixMenuResolver {

    private final Map<ContainerLayout, String> known = new IdentityHashMap<>();
    private final Set<ContainerLayout> unresolvable = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<String> collisions = new TreeSet<>();

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
     * @return how many ids the registry currently resolves
     */
    public int refresh() {
        known.clear();
        unresolvable.clear();
        collisions.clear();
        try {
            MenuRegistry registry = MenuRegistry.getInstance();
            if (registry == null) {
                return 0;
            }
            for (String id : DuelsMenus.PRIORITY) {
                ContainerLayout layout = registry.getMenuOrNull(id);
                if (layout == null) {
                    continue;
                }
                String existing = known.putIfAbsent(layout, id);
                if (existing != null) {
                    collisions.add(existing + " == " + id);
                }
            }
        } catch (Throwable ignored) {
            return known.size();
        }
        return known.size();
    }

    /**
     * @return the ids this addon knows about that the registry currently resolves
     */
    public Set<String> resolvedKeys() {
        return new TreeSet<>(known.values());
    }

    /**
     * @return the ids this addon expects but the registry does not have, which means the
     *         transcription is wrong or the menu is paid-build only
     */
    public Set<String> unresolvedKeys() {
        Set<String> missing = new TreeSet<>(DuelsMenus.ALL);
        missing.removeAll(known.values());
        return missing;
    }

    /**
     * @return pairs of ids that resolved to the same layout instance, where only the first is used
     */
    public Set<String> collisions() {
        return Collections.unmodifiableSet(collisions);
    }

    /**
     * Reads the registry's own key set reflectively.
     *
     * <p>There is no API for this, and it is the only way to see the ids PhoenixDuels actually
     * registered rather than the ones this addon guessed. Diagnostics only — nothing routes off
     * it.</p>
     *
     * @return every key the live registry holds, or an empty list if the field cannot be read
     */
    public List<String> liveRegistryKeys() {
        try {
            MenuRegistry registry = MenuRegistry.getInstance();
            if (registry == null) {
                return List.of();
            }
            Field field = MenuRegistry.class.getDeclaredField("menus");
            field.setAccessible(true);
            Object value = field.get(registry);
            if (!(value instanceof Map<?, ?> map)) {
                return List.of();
            }
            List<String> keys = new ArrayList<>();
            for (Object key : map.keySet()) {
                keys.add(String.valueOf(key));
            }
            Collections.sort(keys);
            return keys;
        } catch (Throwable ignored) {
            return List.of();
        }
    }
}
