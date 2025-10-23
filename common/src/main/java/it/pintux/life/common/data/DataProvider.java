package it.pintux.life.common.data;

import it.pintux.life.common.utils.FormPlayer;
import java.util.List;
import java.util.Map;


public interface DataProvider {
    
    
    List<Map<String, String>> getOnlinePlayersData();
    
    
    List<Map<String, String>> getServerInfoData();
    
    
    List<Map<String, String>> getPlayerPermissionsData(FormPlayer player);
    
    
    List<Map<String, String>> getWorldsData();
    
    
    List<Map<String, String>> getPluginsData();
}
