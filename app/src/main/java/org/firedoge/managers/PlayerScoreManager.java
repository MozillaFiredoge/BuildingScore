package org.firedoge.managers;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.firedoge.Main;

public final class PlayerScoreManager {
    private final Player player;
    private double score;

    public PlayerScoreManager(Player player) {
        this.player = player;
        updateScore();

    }

    public void updateScore() {
        int view = Bukkit.getServer().getViewDistance();

        Location loc = player.getLocation();
        if (loc == null)
            return;

        Chunk center = loc.getChunk();
        int sumCount = 0;
        double sum = 0.0;
        World w = player.getWorld();
        int cx = center.getX();
        int cz = center.getZ();
        for (int dx = -view; dx <= view; dx++) {
            for (int dz = -view; dz <= view; dz++) {
                String key = ChunkScoreManager.chunkKey(w.getChunkAt(cx + dx, cz + dz));
                if (ChunkScoreManager.isChunkInitialized(key)) {
                    sumCount++;
                    sum += ChunkScoreManager.getChunkScore(key);
                }
            }
        }
        if (sumCount == 0) {
            score = 0.0;
            return;
        }
        score = sum / sumCount;

        if (Main.getPlugin().isInDebugMode) {
            player.sendMessage("[PlayerScoreManager]Your score has been updated to: " + score);
        }
    }

    public double getScore() {
        return score;
    }

}
