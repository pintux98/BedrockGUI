package it.pintux.life.essentialsaddon.api;

import it.pintux.life.essentialsaddon.model.HomeView;
import it.pintux.life.essentialsaddon.model.HomeWriteResult;
import it.pintux.life.essentialsaddon.model.PublicHomeView;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Contract for home providers (EssentialsX, CMI, HuskHomes).
 *
 * <p>Every call hands its result to a callback and must return without waiting. HuskHomes answers
 * its API on the server thread, so a provider that blocked for an answer while holding that thread
 * could never receive one. Callbacks therefore run on whichever thread the backing plugin
 * finishes on, and callers must not assume the server thread.</p>
 */
public interface HomeProvider {
    String getProviderId();

    boolean isReady();

    void homeNames(Player player, Consumer<List<String>> callback);

    void homeLimit(Player player, IntConsumer callback);

    void teleportHome(Player player, String homeName, Consumer<Boolean> callback);

    void setHome(Player player, String homeName, Consumer<HomeWriteResult> callback);

    void deleteHome(Player player, String homeName, Consumer<Boolean> callback);

    /**
     * Homes with the extra detail the manage form needs. Providers without a privacy concept
     * inherit this, which reports every home as private.
     */
    default void homeDetails(Player player, Consumer<List<HomeView>> callback) {
        homeNames(player, names -> {
            List<HomeView> views = new java.util.ArrayList<>();
            for (String name : names) {
                views.add(new HomeView(name, false));
            }
            callback.accept(views);
        });
    }

    /** True when the backing plugin has a browsable directory of homes shared with everyone. */
    default boolean supportsPublicHomes() {
        return false;
    }

    /**
     * True when a single home can be made public or private. Separate from
     * {@link #supportsPublicHomes()}: CMI marks homes private without publishing a directory of
     * the public ones, so it has the switch but nothing to browse.
     */
    default boolean supportsPrivacy() {
        return supportsPublicHomes();
    }

    /**
     * What separates owner from home name in a public home identifier. Configurable in the
     * backing plugin, so it is asked for rather than assumed — and a Bedrock username may itself
     * start with that character.
     */
    default String publicHomeSeparator() {
        return ".";
    }

    /** Public homes, each carrying the identifier to act on plus its owner and name to show. */
    default void publicHomes(Player player, Consumer<List<PublicHomeView>> callback) {
        callback.accept(List.of());
    }

    default void teleportPublicHome(Player player, String identifier, Consumer<Boolean> callback) {
        callback.accept(false);
    }

    /** True when a home can be renamed without losing it. */
    default boolean supportsRename() {
        return false;
    }

    default void renameHome(Player player, String homeName, String newName,
                            Consumer<HomeWriteResult> callback) {
        callback.accept(HomeWriteResult.failed("renaming is not supported"));
    }

    /** Makes an existing home public or private; only meaningful with {@link #supportsPublicHomes()}. */
    default void setHomePrivacy(Player player, String homeName, boolean isPublic,
                                Consumer<HomeWriteResult> callback) {
        callback.accept(HomeWriteResult.failed("privacy is not supported"));
    }
}
