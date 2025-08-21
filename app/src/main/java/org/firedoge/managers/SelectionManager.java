package org.firedoge.managers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SelectionManager {
    private static final Map<Player, Location[]> selections = new HashMap<>();
    private static boolean isInSelectMode = false;

    public static void setPos1(Player player, Location loc) {
        selections.computeIfAbsent(player, k -> new Location[2])[0] = loc;
    }

    public static void setPos2(Player player, Location loc) {
        selections.computeIfAbsent(player, k -> new Location[2])[1] = loc;
    }

    public static Location[] getSelection(Player player) {
        return selections.get(player);
    }

    public static void setSelectMode(boolean mode) {
        isInSelectMode = mode;
    }

    public static boolean isInSelectMode() {
        return isInSelectMode;
    }
}

