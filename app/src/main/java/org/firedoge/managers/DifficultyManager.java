package org.firedoge.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.firedoge.Main;
import org.firedoge.utils.Constants;

public class DifficultyManager {

    private void changeDifficulty() {
        Map<Double, List<Player>> groupScores = calMeanScoreForGroup();
        
        if(groupScores.isEmpty()) {
            return; // No players to process
        }

        groupScores.forEach((meanScore, players) -> {
            MonsterSpawnManager msp = new MonsterSpawnManager(meanScore, players);
            msp.spawnMonsters(); // Spawn monsters for the group based on their mean score
        });
    }

    public Map<Double, List<Player>> calMeanScoreForGroup() {
        Map<Double, List<Player>> groupScores = new java.util.HashMap<>();

        Bukkit.getWorlds().forEach(world -> {
            // Logic to spawn monsters in the world
            // This could include checking conditions, selecting spawn points, etc.
            Map<String, List<Player>> playerGroups = classifyPlayersByDistance(world);

            playerGroups.forEach((rangeKey, players) -> {
                // For each group of players, spawn monsters based on the group's distance
                int numSum = 0;
                double sum = 0.0;
                for (Player player : players) {
                    PlayerScoreManager psm = new PlayerScoreManager(player);
                    sum += psm.getScore();
                    numSum++;
                }
                double groupMean = sum / numSum;
                if(Main.getPlugin().isInDebugMode)
                    Main.getPlugin().getLogger().log(Level.FINE, "[DifficultyManager] Group {0} mean score: {1} with players: {2}", new Object[]{rangeKey, groupMean, players});
                groupScores.put(groupMean, players);
            });
        });
        return groupScores;
    }

    public static Map<String, List<Player>> classifyPlayersByDistance(World world) {
        List<Player> players = world.getPlayers();
        Map<String, List<Player>> groups = new HashMap<>();
        for (Player player1 : players) {
            for (Player player2 : players) {
                if (player1.equals(player2))
                    continue; // skip self
                Location loc1 = player1.getLocation();
                Location loc2 = player2.getLocation();
                if(loc1 == null || loc2 == null)
                    continue; // skip if location is null

                double distance = loc1.distance(loc2);
                String rangeKey = getRangeKey(distance, Constants.PLAYER_GROUP_DISTANCE);

                groups.putIfAbsent(rangeKey, new ArrayList<>());
                if (!groups.get(rangeKey).contains(player1)) {
                    groups.get(rangeKey).add(player1);
                }
            }
        }
        if(groups.isEmpty() && !players.isEmpty()) {
            List<Player> singlePlayers = new ArrayList<>(players);
            groups.put("0-0", singlePlayers);
        }
        return groups;
    }
    public void run(){
        new BukkitRunnable() {
            @Override
            public void run() {
                changeDifficulty();
            }
        }.runTaskTimer(Main.getPlugin(), 0L, Constants.MOB_SPAWN_INTERVAL); // Run every 1 minutes
    }

    private static String getRangeKey(double distance, double range) {
        int lowerBound = (int) (Math.floor(distance / range) * range);
        int upperBound = lowerBound + (int) range;
        return lowerBound + "-" + upperBound;
    }
}
