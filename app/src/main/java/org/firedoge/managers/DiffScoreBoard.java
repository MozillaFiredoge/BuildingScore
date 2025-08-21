package org.firedoge.managers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;

public class DiffScoreBoard {

    private final Map<Player, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();
    private final ScoreboardManager manager = Bukkit.getScoreboardManager();

    private final int groupSize;
    private final int tierLevel;
    private final double score;
    private final List<Player> players;

    public DiffScoreBoard(int groupSize, int tierLevel, double score, List<Player> players) {
        this.groupSize = groupSize;
        this.tierLevel = tierLevel;
        this.score = score;
        this.players = players;

    }

    public void updateScoreboard() {
        players.forEach(player -> {
            Scoreboard board = playerScoreboards.computeIfAbsent(player, p -> manager.getNewScoreboard());

            String objectiveName = "groupInfo";
            Objective obj = board.getObjective(objectiveName);
            if (obj == null) {
                // 使用新的方法注册计分板目标
                String s = ChatColor.DARK_RED + "☠ " + ChatColor.BOLD + "灾难警报" + ChatColor.DARK_RED + " ☠ ";
                obj = board.registerNewObjective(objectiveName, Criteria.DUMMY,
                        Component.text(s));
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);

                String tier = getTierName(tierLevel);

                // 更新计分板内容
                obj.getScore(ChatColor.GRAY + "------------------").setScore(6); // 分隔线

                obj.getScore(ChatColor.YELLOW + "玩家ID: " + ChatColor.AQUA + player.getName()).setScore(5);
                obj.getScore(ChatColor.YELLOW + "当前分数: " + ChatColor.GREEN + String.format("%.1f", score)).setScore(4);
                obj.getScore(ChatColor.YELLOW + "组内人数: " + groupSize + "人").setScore(3);
                obj.getScore(ChatColor.YELLOW + "威胁等级: " + getTierColor(tier) + tier).setScore(2);

                obj.getScore(ChatColor.GRAY + "==================").setScore(0); // 底部装饰

                player.setScoreboard(board);
            }
        });
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(manager.getMainScoreboard());
        playerScoreboards.remove(player);
    }

    private String getTierName(int tierLevel) {
        return switch (tierLevel) {
            case 0 -> "安全区域";
            case 1 -> "轻度威胁";
            case 2 -> "中等威胁";
            case 3 -> "高度危险";
            default -> "未知";
        };
    }

    private ChatColor getTierColor(String tierName) {
        return switch (tierName) {
            case "安全区域" -> ChatColor.GREEN;
            case "轻度威胁" -> ChatColor.YELLOW;
            case "中等威胁" -> ChatColor.RED;
            case "高度危险" -> ChatColor.DARK_RED;
            default -> ChatColor.GRAY;
        };
    }
}