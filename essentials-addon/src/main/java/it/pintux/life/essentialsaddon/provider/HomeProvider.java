package it.pintux.life.essentialsaddon.provider;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public interface HomeProvider {

    String getProviderId();

    boolean isReady();

    List<String> getHomeNames(Player player);

    Location getHomeLocation(Player player, String homeName);

    boolean teleportHome(Player player, String homeName);

    boolean setHome(Player player, String homeName);

    boolean deleteHome(Player player, String homeName);

    int getMaxHomes(Player player);

    int getHomeCount(Player player);
}
