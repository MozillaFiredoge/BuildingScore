package org.firedoge.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.firedoge.Main;
import org.firedoge.managers.ChunkScoreManager;
import org.firedoge.utils.Constants;

public class BlockListener implements Listener {
    private final ChunkScoreManager chunkManager = new ChunkScoreManager();

    // This class will handle block break events
    // Add methods to handle specific events related to block breaking
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        String chunkKey = ChunkScoreManager.chunkKey(event.getBlock().getChunk());
        chunkManager.updateChunkScore(chunkKey, Constants.BREAK_PENALTY);
        if (Main.getPlugin().isInDebugMode) {
            event.getPlayer().sendMessage("[BuildingScore][Debug] Updating chunk " + chunkKey + ", Current Score: " + ChunkScoreManager.getChunkScore(chunkKey));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        String chunkKey = ChunkScoreManager.chunkKey(event.getBlock().getChunk());
        chunkManager.updateChunkScore(chunkKey, Constants.PLACE_REWARD);
        if (Main.getPlugin().isInDebugMode) {
            event.getPlayer().sendMessage("[BuildingScore][Debug] Updating chunk " + chunkKey + ", Current Score: " + ChunkScoreManager.getChunkScore(chunkKey));
        }
    }
}
