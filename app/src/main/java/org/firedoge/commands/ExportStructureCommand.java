package org.firedoge.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.firedoge.Main;
import org.firedoge.managers.SelectionManager;
import org.firedoge.models.Structure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

public class ExportStructureCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            if (sender == null)
                return false;
            sender.sendMessage("只能玩家执行");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("用法: /exportstructure <name>|<select> offset");
            return true;
        }
        if (args[0].equalsIgnoreCase("select")) {
            SelectionManager.setSelectMode(!SelectionManager.isInSelectMode());
            player.sendMessage(SelectionManager.isInSelectMode() ? "§a已进入选择区域模式" : "§c已退出选择区域模式");
            return true;
        }

        int offsetX = 0, offsetY = 0, offsetZ = 0;
        if (args.length == 4) {
            offsetX = args[1].equalsIgnoreCase("~") ? 0 : Integer.parseInt(args[1]);
            offsetY = args[2].equalsIgnoreCase("~") ? 0 : Integer.parseInt(args[2]);
            offsetZ = args[3].equalsIgnoreCase("~") ? 0 : Integer.parseInt(args[3]);
        }

        Location[] sel = SelectionManager.getSelection(player);
        if (sel == null || sel[0] == null || sel[1] == null) {
            player.sendMessage("§c请先用木棍左键/右键选区两个点");
            return true;
        }

        // 确定边界
        int minX = Math.min(sel[0].getBlockX(), sel[1].getBlockX());
        int minY = Math.min(sel[0].getBlockY(), sel[1].getBlockY());
        int minZ = Math.min(sel[0].getBlockZ(), sel[1].getBlockZ());
        int maxX = Math.max(sel[0].getBlockX(), sel[1].getBlockX());
        int maxY = Math.max(sel[0].getBlockY(), sel[1].getBlockY());
        int maxZ = Math.max(sel[0].getBlockZ(), sel[1].getBlockZ());

        List<Structure._BlockData> blocks = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = sel[0].getWorld().getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        String blockDataString = block.getBlockData().getAsString();
                        blocks.add(new Structure._BlockData(
                                x - minX + offsetX,
                                y - minY + offsetY,
                                z - minZ + offsetZ,
                                block.getType().toString(),
                                blockDataString));
                    }
                }
            }
        }

        Structure structure = new Structure();
        structure.setBlocks(blocks);

        try {
            File dir = new File(Main.getPlugin().getDataFolder(), "structures");
            if (!dir.exists())
                dir.mkdirs();

            File file = new File(dir, args[0] + ".json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(structure, writer);
            }
            player.sendMessage("§a导出成功: " + file.getPath());
        } catch (JsonIOException | IOException e) {
            player.sendMessage("§c导出失败: " + e.getMessage());
        }

        return true;
    }
}
