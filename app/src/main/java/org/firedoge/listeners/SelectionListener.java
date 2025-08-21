package org.firedoge.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.firedoge.managers.SelectionManager;

public class SelectionListener implements Listener {

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent e) {
        if (!SelectionManager.isInSelectMode()) return;
        ItemStack item = e.getItem();
        if (item != null && item.getType() == Material.STICK) {
            Block block = e.getClickedBlock();
            if (block != null) {
                if (e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                    SelectionManager.setPos1(e.getPlayer(), block.getLocation());
                    e.getPlayer().sendMessage("§a已设置点 1: " + block.getLocation());
                } else if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    SelectionManager.setPos2(e.getPlayer(), block.getLocation());
                    e.getPlayer().sendMessage("§a已设置点 2: " + block.getLocation());
                }
                e.setCancelled(true);
            }
        }
    }

}
