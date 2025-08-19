package org.firedoge.managers;

import java.util.List;

import org.bukkit.entity.Player;
import org.firedoge.models.MobSpawn;
import org.firedoge.models.Tier;
import org.firedoge.utils.ConfigUtils;

public class MonsterSpawnManager {

    private final double meanScore;
    private final List<Player> players;

    private final List<Tier> tiers = ConfigUtils.getTiers();

    public MonsterSpawnManager(double meanScore, List<Player> players) {
        this.meanScore = meanScore;
        this.players = players;
    }

    public void spawnMonsters() {
        // Logic to spawn monsters based on the meanScore and players
        Tier applicableTier = getApplicableTier();
        // Spawn monsters based on the applicable tier
        String[] parts = applicableTier.getName().split("-");
        int num = Integer.parseInt(parts[0]);
        MobSpawn mobSpawn = new MobSpawn();
        DiffScoreBoard scoreBoard = new DiffScoreBoard(players.size(), num, meanScore, players);

        scoreBoard.updateScoreboard();
        switch (num) {
            case 0 -> {
                mobSpawn.spawnLowTierMonsters(players);
            }
            case 1 -> {
                mobSpawn.spawnMediumTierMonsters(players);
            }
            case 2 -> {
                mobSpawn.spawnHighTierMonsters(players);
            }
            case 3 -> {
                mobSpawn.spawnVeryHighTierMonsters(players);
            }
            default -> throw new AssertionError();
        }
    }


    private Tier getApplicableTier() {
        Tier applicableTier = null;
        for (Tier tier : tiers) {
            if (meanScore > tier.getMinScore() && meanScore <= tier.getMaxScore()) {
                applicableTier = tier;
            }
        }
        return applicableTier == null ? new Tier("0-0", meanScore, meanScore) : applicableTier;
    }
}
