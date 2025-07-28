package it.pintux.life.common.data;

import it.pintux.life.common.utils.FormPlayer;
import java.util.List;
import java.util.Map;

/**
 * Interface for providing data to the List action handler
 * Platform-specific implementations should provide real data
 */
public interface DataProvider {
    
    /**
     * Gets a list of online players with their information
     * @return List of player data maps containing name, uuid, and other info
     */
    List<Map<String, String>> getOnlinePlayersData();
    
    /**
     * Gets server information data
     * @return List of server info maps containing various server metrics
     */
    List<Map<String, String>> getServerInfoData();
    
    /**
     * Gets player permissions data for a specific player
     * @param player The player to get permissions for
     * @return List of permission data maps
     */
    List<Map<String, String>> getPlayerPermissionsData(FormPlayer player);
    
    /**
     * Gets world information data
     * @return List of world data maps
     */
    List<Map<String, String>> getWorldsData();
    
    /**
     * Gets plugin information data
     * @return List of plugin data maps
     */
    List<Map<String, String>> getPluginsData();
}