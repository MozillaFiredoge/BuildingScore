package org.firedoge.managers;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.firedoge.Main;
import org.firedoge.utils.Constants;
import org.firedoge.utils.sqlite;

public class ChunkScoreManager {
    private final Map<String, Double> chunkScores = new ConcurrentHashMap<>();
    private final Map<String, Boolean> chunkInitialized = new ConcurrentHashMap<>();

    private int updateFrequency = 0;

    public ChunkScoreManager() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    chunkScores.forEach((chunkKey, score) -> {
                        // Save each chunk score to the database
                        saveChunk(chunkKey, score);

                    });
                    chunkScores.clear(); // Clear the map after saving to prevent memory leaks
                    chunkInitialized.clear(); // Clear the initialized map as well
                    Main.getPlugin().getLogger().log(Level.FINE, "Saved chunk scores");
                } catch (Throwable t) {
                    Main.getPlugin().getLogger().log(Level.SEVERE,
                            "Error in scheduled compute task: {0}",
                            t.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(Main.getPlugin(), 20L * 5L, Constants.CHUNK_UPDATE_INTERVAL_TICKS); // initial delay 5s
    }

    public static String chunkKey(Chunk chunk) {
        World w = chunk.getWorld();
        UUID uid = w.getUID();
        return uid.toString() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    private void saveChunk(String chunkKey, double score) {
        // This method should save the chunk score to the database
        Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
            sqlite db = new sqlite();
            try {
                db.connect();
                db.updateScore(chunkKey, score);
                chunkScores.clear();
                chunkInitialized.clear();
            } catch (SQLException e) {
                Main.getPlugin().getLogger().log(Level.WARNING, "Error saving chunk: {0}", e.getMessage());
            } finally {
                try {
                    db.close(); // Ensure you close the connection after use
                } catch (SQLException e) {
                    Main.getPlugin().getLogger().log(Level.WARNING, "Error closing database connection: {0}", e.getMessage());
                }
            }
        });
    }

    public void updateChunkScore(String chunkKey, double score) {
        // This method should update the chunk score in the database
        if (chunkInitialized.getOrDefault(chunkKey, false)) {
            initializeChunk(chunkKey);
        }
        score = score + chunkScores.getOrDefault(chunkKey, 0.0);
        chunkScores.put(chunkKey, score);

        updateFrequency++;
        if (updateFrequency >= Constants.UPDATE_FREQUENCY) {
            // Save the chunk scores to the database
            saveChunk(chunkKey, score);
            updateFrequency = 0;
        }
    }

    public static double getChunkScore(String chunkKey) {
        // This method should load the chunk from the database
        // You can use the sqlite class to interact with the database
        double scoreObtained = 0.0;
        sqlite db = new sqlite();
        try {
            db.connect();
            scoreObtained = db.getScore(chunkKey);
        } catch (SQLException e) {
            Main.getPlugin().getLogger().log(Level.WARNING, "Error loading chunk: {0}", e.getMessage());
        } finally {
            try {
                db.close(); // Ensure you close the connection after use
            } catch (SQLException e) {
                Main.getPlugin().getLogger().log(Level.WARNING, "Error closing database connection: {0}", e.getMessage());
            }
        }

        return scoreObtained; // Return a default score if there was an error
    }

    public void initializeChunk(String chunkKey) {
        chunkInitialized.put(chunkKey, true); // Mark the chunk as initialized
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                // This method should initialize the chunk in the database
                sqlite db = new sqlite();
                // Set the chunk as initialized in the database
                try {
                    db.connect();
                    chunkScores.put(chunkKey, db.getScore(chunkKey));
                } catch (SQLException e) {
                    Main.getPlugin().getLogger().log(Level.WARNING, "Error initializing chunk: {0}", e.getMessage());
                } finally {
                    try {
                        db.close(); // Ensure you close the connection after use
                    } catch (SQLException e) {
                        Main.getPlugin().getLogger().log(Level.WARNING, "Error closing database connection: {0}", e.getMessage());
                    }
                }
            }
        };
        task.runTaskAsynchronously(Main.getPlugin());
    }

    public static boolean isChunkInitialized(String chunkKey) {
        sqlite db = new sqlite();
        // Set the chunk as initialized in the database
        try {
            db.connect();
            return db.recordExists(chunkKey);
        } catch (SQLException e) {
            Main.getPlugin().getLogger().log(Level.WARNING, "Error initializing chunk: {0}", e.getMessage());
        } finally {
            try {
                db.close(); // Ensure you close the connection after use
            } catch (SQLException e) {
                Main.getPlugin().getLogger().log(Level.WARNING, "Error closing database connection: {0}", e.getMessage());
            }
        }
        return false;
    }
}
