package org.firedoge.monsters;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.firedoge.Main;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;

public class Minions {
    private final LivingEntity mob;
    private final Location home;
    private final Random random = new Random();
    public Minions(LivingEntity entity, Location home) {
        this.mob = entity;
        this.home = home;
        EntityEquipment ee = mob.getEquipment();
        AttributeInstance scaleAttr = mob.getAttribute(Attribute.SCALE);
        AttributeInstance damage = mob.getAttribute(Attribute.ATTACK_DAMAGE);

        mob.setHealth(3.0);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));

        if (scaleAttr != null)
            scaleAttr.setBaseValue(0.2);

        if (ee != null)
            ee.setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.STICK));

        if (damage != null)
            damage.setBaseValue(0.5);

        if(random.nextDouble() <= 0.3)
            addDashAttack(mob);
        if(random.nextDouble() <= 0.1)
            addStealItem();
    }

    private void addStealItem() {
        // 检查附近是否有哨站

        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.isDead()) {
                    cancel();
                    return;
                }
                Player target = findNearestPlayerWithinRange(mob, 5);
                if (target == null) {
                    return;
                }
                Location loc = target.getLocation();
                if (loc == null) {
                    return;
                }
                if (loc.distance(mob.getLocation()) < 3) {
                    // 尝试偷取玩家手中的物品
                    ItemStack item = target.getInventory().getItemInMainHand();
                    if (item.getType() != Material.AIR) {
                        target.getInventory().setItemInMainHand(null);
                        mob.getWorld().dropItemNaturally(mob.getLocation(), item);
                        target.sendMessage(Component.text(ChatColor.RED + "Your Item has been stolen, go chase it!"));
                        goHome(mob, home);
                    }
                }
            }

        }.runTaskTimer(Main.getPlugin(), 0, 10);

    }

    private void addDashAttack(LivingEntity entity) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead())
                    cancel();
                Player target = findNearestPlayerWithinRange(entity, 3);
                if (target == null)
                    return;
                Location loc = target.getLocation();
                if (loc == null)
                    return;
                if (loc.distance(entity.getLocation()) < 10) {
                    Vector dir = loc.toVector().subtract(entity.getLocation().toVector()).normalize();
                    entity.setVelocity(dir.multiply(1.5).setY(0.3));
                }
            }
        }.runTaskTimer(Main.getPlugin(), 0, 60);
    }

    // 查找范围内最近的玩家
    private Player findNearestPlayerWithinRange(LivingEntity mob, double range) {
        Player nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Player player : mob.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(mob.getLocation());
            if (distance <= range && distance < minDistance) {
                minDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    // 让怪物逃跑
    private void goHome(LivingEntity mob, Location target) {
        if (mob instanceof org.bukkit.entity.Mob monster) {
            // 使用Paper API的寻路
            try {
                monster.setTarget(null);
                monster.getPathfinder().moveTo(target);
            } catch (Exception e) {
                // 备用方案：简单朝目标方向移动
                Vector direction = target.toVector().subtract(mob.getLocation().toVector()).normalize();
                mob.setVelocity(direction.multiply(0.5));
            }
        }
    }

}
