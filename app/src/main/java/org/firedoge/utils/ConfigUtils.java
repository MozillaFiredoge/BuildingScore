package org.firedoge.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.firedoge.Main;
import org.firedoge.models.Tier;

public class ConfigUtils {
    private static final List<Tier> tiers = new ArrayList<>();

    public void loadTiers() {
        FileConfiguration config = Main.getPlugin().getConfig();

        // 清空旧数据
        tiers.clear();

        // 读取配置文件中的 tiers 节点
        List<?> tierList = config.getList("tiers");
        if (tierList != null) {
            for (Object obj : tierList) {
                if (obj instanceof Map<?, ?> map) {
                    String name = (String) map.get("name");
                    double min = map.get("min") instanceof Number ? ((Number) map.get("min")).doubleValue() : 0.0;
                    double max = map.get("max") instanceof Number ? ((Number) map.get("max")).doubleValue() : 0.0;

                    Tier tier = new Tier(name, min, max);
                    tiers.add(tier);
                }
            }
        }
    }

    public static List<Tier> getTiers() {
        return tiers;
    }
}
