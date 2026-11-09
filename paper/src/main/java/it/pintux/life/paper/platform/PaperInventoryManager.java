package it.pintux.life.paper.platform;

import it.pintux.life.common.platform.PlatformInventoryManager;
import it.pintux.life.common.utils.FormPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PaperInventoryManager implements PlatformInventoryManager {

    @Override
    public int countItem(FormPlayer formPlayer, String item) {
        Player player = Bukkit.getPlayer(formPlayer.getUniqueId());
        Material material = Material.matchMaterial(item.trim());
        if (player == null || material == null || !material.isItem()) {
            return 0;
        }
        ItemStack probe = new ItemStack(material);
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.isSimilar(probe)) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    @Override
    public boolean removeItem(FormPlayer formPlayer, String item, int amount) {
        Player player = Bukkit.getPlayer(formPlayer.getUniqueId());
        Material material = Material.matchMaterial(item.trim());
        if (player == null || material == null || !material.isItem() || countItem(formPlayer, item) < amount) {
            return false;
        }
        return player.getInventory().removeItem(new ItemStack(material, amount)).isEmpty();
    }
}
