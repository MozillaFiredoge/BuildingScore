package org.firedoge.utils;

public enum Constants {
    CONSTANS;
    public static final double BREAK_PENALTY = -0.7;
    public static final double PLACE_REWARD = 1.0;
    public static final long CHUNK_UPDATE_INTERVAL_TICKS = 15L * 60L * 20L; // 15 minutes
    public static final long DIFF_UPDATE_INTERVAL = 20L * 60L; // 1 minute
    public static final double PLAYER_GROUP_DISTANCE = 100.0; // blocks
    public static final double UPDATE_FREQUENCY = 10; // save chunk per blocks
    public static final long MOB_SPAWN_INTERVAL = 20L * 60L * 1L; // 1 minutes
    public static final int MAX_MONSTER_SPAWN_COUNT = 80; // Maximum number of monsters to spawn
}
