package it.pintux.life.paper.platform;

import it.pintux.life.common.actions.ActionSystem;
import it.pintux.life.common.form.FormMenuUtil;
import it.pintux.life.common.form.obj.FormMenu;
import it.pintux.life.common.form.obj.JavaMenuDefinition;
import it.pintux.life.common.form.obj.JavaMenuItem;
import it.pintux.life.common.form.obj.JavaMenuType;
import it.pintux.life.common.platform.PlatformJavaMenuManager;
import it.pintux.life.common.utils.FormPlayer;
import it.pintux.life.common.utils.MessageData;
import it.pintux.life.paper.utils.PaperPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PaperJavaMenuManager implements PlatformJavaMenuManager, Listener {
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final MessageData messageData;

    private static class Session {
        final UUID playerId;
        final Inventory inventory;
        final Map<Integer, java.util.List<ActionSystem.Action>> actions;
        final Map<String, String> placeholders;
        final FormMenuUtil util;
        Session(UUID playerId, Inventory inv, Map<Integer, java.util.List<ActionSystem.Action>> actions,
                Map<String, String> placeholders, FormMenuUtil util) {
            this.playerId = playerId;
            this.inventory = inv;
            this.actions = actions;
            this.placeholders = placeholders;
            this.util = util;
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public PaperJavaMenuManager(org.bukkit.plugin.java.JavaPlugin plugin, MessageData messageData) {
        this.plugin = plugin;
        this.messageData = messageData;
    }

    @Override
    public void openJavaMenu(FormPlayer player, FormMenu menu, Map<String, String> placeholders, FormMenuUtil util) {
        if (!(player instanceof PaperPlayer)) {
            return;
        }
        Player bukkitPlayer = ((PaperPlayer) player).getBukkitPlayer();
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return;

        JavaMenuDefinition jdef = menu.getJavaMenu();
        if (jdef == null) return;

        String title = renderText(jdef.getTitle(), player, placeholders);
        Inventory inv;
        if (jdef.getType() == JavaMenuType.CHEST) {
            int size = Math.max(9, Math.min(jdef.getSize() <= 0 ? 9 : jdef.getSize(), 54));
            size = ((size + 8) / 9) * 9;
            inv = Bukkit.createInventory((InventoryHolder) null, size, title);
        } else if (jdef.getType() == JavaMenuType.ANVIL) {
            inv = Bukkit.createInventory((InventoryHolder) null, org.bukkit.event.inventory.InventoryType.ANVIL, title);
        } else if (jdef.getType() == JavaMenuType.CRAFTING) {
            bukkitPlayer.openWorkbench(bukkitPlayer.getLocation(), true);
            return;
        } else {
            return;
        }

        Map<Integer, java.util.List<ActionSystem.Action>> actions = new HashMap<>();
        for (Map.Entry<Integer, JavaMenuItem> e : jdef.getItems().entrySet()) {
            int slot = e.getKey();
            JavaMenuItem item = e.getValue();
            ItemStack stack = buildItem(item, player, placeholders);
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, stack);
            }
            if (item.getActions() != null && !item.getActions().isEmpty()) {
                actions.put(slot, item.getActions());
            }
        }

        bukkitPlayer.openInventory(inv);
        sessions.put(bukkitPlayer.getUniqueId(), new Session(bukkitPlayer.getUniqueId(), inv, actions, placeholders, util));
    }

    private ItemStack buildItem(JavaMenuItem item, FormPlayer player, Map<String, String> placeholders) {
        Material mat;
        try {
            mat = Material.valueOf(item.getMaterial().toUpperCase());
        } catch (Exception e) {
            mat = Material.STONE;
        }
        ItemStack stack = new ItemStack(mat, Math.max(1, item.getAmount()));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (item.getName() != null) {
                String name = renderText(item.getName(), player, placeholders);
                meta.setDisplayName(name);
            }
            if (item.getLore() != null && !item.getLore().isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String line : item.getLore()) colored.add(renderText(line, player, placeholders));
                meta.setLore(colored);
            }
            if (item.isGlow()) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String renderText(String text, FormPlayer player, Map<String, String> placeholders) {
        if (text == null) return null;
        String result = text;
        if (placeholders != null && !placeholders.isEmpty()) {
            result = it.pintux.life.common.utils.PlaceholderUtil.processDynamicPlaceholders(result, placeholders);
        }
        if (messageData != null && result.contains("%")) {
            result = messageData.replaceVariables(result, null, player);
        }
        if (messageData != null) {
            result = messageData.applyColor(result);
        }
        return result;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        InventoryView view = player.getOpenInventory();
        if (view == null || view.getTopInventory() != session.inventory) return;
        int rawSlot = event.getRawSlot();
        if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY ||
            event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK ||
            event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
            event.setCancelled(true);
        }
        if (rawSlot < view.getTopInventory().getSize()) {
            event.setCancelled(true);
            java.util.List<ActionSystem.Action> defs = session.actions.get(rawSlot);
            if (defs != null && !defs.isEmpty()) {
                FormPlayer fp = new PaperPlayer(player);
                ActionSystem.ActionContext ctx = it.pintux.life.common.utils.PlaceholderUtil.createContextWithBuiltinPlaceholders(fp, session.placeholders, messageData);
                it.pintux.life.common.actions.ActionExecutor exec = session.util.getActionExecutor();
                java.util.List<ActionSystem.ActionResult> resList = exec.executeActions(fp, defs, ctx);
                for (ActionSystem.ActionResult res : resList) {
                    if (res.isFailure() && res.message() != null) {
                        player.sendMessage("Action failed: " + res.message());
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        InventoryView view = player.getOpenInventory();
        if (view == null || view.getTopInventory() != session.inventory) return;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < view.getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        if (event.getInventory() == session.inventory) {
            sessions.remove(player.getUniqueId());
        }
    }
}

