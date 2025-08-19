package org.firedoge.models;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.firedoge.Main;

public class MobSpawn {
    private static final Random random = ThreadLocalRandom.current();

    private void spawnMonsters(List<Player> players, int baseCount, MonsterTier tier) {
        if (players.isEmpty())
            return;

        Location center = calculateGroupCenter(players);
        int playerCount = players.size();
        // adjust mobs' count based on number of players
        int monsterCount = baseCount + (int) (playerCount * 1.5);

        for (int i = 0; i < monsterCount; i++) {
            // spawn at a circular area around the center(Radius: 50)
            Location spawnLoc = center.clone().add(
                    random.nextDouble() * 150 - 75,
                    0,
                    random.nextDouble() * 150 - 75);

            // Make sure it's safe to spawn
            World w = spawnLoc.getWorld();
            if (w == null)
                continue;
            double y = w.getHighestBlockYAt(spawnLoc);
            spawnLoc.setY(y + 1);

            EntityType type = selectMonsterType(tier);
            LivingEntity entity = (LivingEntity) w.spawnEntity(spawnLoc, type);

            enhanceMonster(entity, tier, playerCount);
        }
    }

    private Location calculateGroupCenter(List<Player> players) {
        double x = 0, y = 0, z = 0;
        for (Player p : players) {
            Location l = p.getLocation();
            if (l == null)
                continue;
            x += l.getX();
            y += l.getY();
            z += l.getZ();
        }
        int count = players.size();
        return new Location(players.get(0).getWorld(), x / count, y / count, z / count);
    }

    private EntityType selectMonsterType(MonsterTier tier) {
        return switch (tier) {
            case LOW -> random.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;
            case MEDIUM -> random.nextBoolean() ? EntityType.SPIDER : EntityType.CAVE_SPIDER;
            case HIGH -> random.nextBoolean() ? EntityType.ENDERMAN : EntityType.PILLAGER;
            case VERY_HIGH -> random.nextBoolean() ? EntityType.RAVAGER : EntityType.WITHER_SKELETON;
            default -> EntityType.ZOMBIE;
        };
    }

    private void enhanceMonster(LivingEntity entity, MonsterTier tier, int playerCount) {

        double playerFactor = 1 + (playerCount * 0.1);

        switch (tier) {
            case LOW -> {
                entity.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1));
                setEntityScale(entity, 0.2);
            }

            case MEDIUM -> {
                entity.addPotionEffect(new PotionEffect(
                        PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));
                entity.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                setEntityScale(entity, 2.0);
                if (random.nextDouble() < 0.3)
                    addWebShotAbility(entity);
            }

            case HIGH -> {
                entity.addPotionEffect(new PotionEffect(
                        PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
                setEntityScale(entity, 5.0);

                if (entity instanceof Phantom phantom) {
                    phantom.setSize(6 + random.nextInt(6));
                }
                if (random.nextDouble() < 0.5)
                    addAuraEffect(entity);
            }

            case VERY_HIGH -> {
                setEntityScale(entity, 3.0 + (playerFactor * 0.5));
                entity.addPotionEffect(new PotionEffect(
                        PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2));
                entity.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                setEntityScale(entity, 10.0);

                if (entity instanceof Ravager ravager) {
                    addGroundSlamAbility(ravager);
                } else {
                    addDarkAura(entity);
                }
            }

        }

    }

    // Special Skills
    private void addWebShotAbility(LivingEntity entity) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead())
                    this.cancel();

                Player target = findNearestPlayer(entity);
                Location loc = target.getLocation();
                if (loc != null && loc.distance(entity.getLocation()) < 12) {
                    target.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS, 100, 2));
                    target.getWorld().spawnParticle(org.bukkit.Particle.ASH,
                            loc, 20);
                }
            }
        }.runTaskTimer(Main.getPlugin(), 0, 80);
    }

    private void addAuraEffect(LivingEntity entity) {

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead())
                    this.cancel();

                entity.getNearbyEntities(5, 3, 5).forEach(e -> {
                    if (e instanceof Player player) {
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.BLINDNESS, 60, 0));
                    }
                });
                entity.getWorld().spawnParticle(org.bukkit.Particle.LARGE_SMOKE,
                        entity.getLocation(), 30);
            }
        }.runTaskTimer(Main.getPlugin(), 0, 100);
    }

    private void addGroundSlamAbility(Ravager ravager) {

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (ravager.isDead())
                    this.cancel();

                ravager.getNearbyEntities(6, 3, 6).forEach(e -> {
                    if (e instanceof Player) {
                        Vector launch = e.getLocation().toVector()
                                .subtract(ravager.getLocation().toVector())
                                .normalize()
                                .multiply(2)
                                .setY(1.2);
                        e.setVelocity(launch);
                    }
                });

                Location loc = ravager.getLocation();
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        if (random.nextDouble() < 0.7) {
                            loc.clone().add(x, -1, z).getBlock().breakNaturally();
                        }
                    }
                }

                ravager.getWorld().spawnParticle(org.bukkit.Particle.BLOCK_CRUMBLE,
                        loc, 50, Material.DIRT.createBlockData());
            }
        }.runTaskTimer(Main.getPlugin(), 0, 140);
    }

    private void addDarkAura(LivingEntity entity) {

        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead())
                    this.cancel();

                entity.getNearbyEntities(8, 5, 8).forEach(e -> {
                    if (e instanceof Player player) {
                        player.damage(2, entity);
                        e.setFireTicks(40);
                    }
                });

                entity.getWorld().spawnParticle(org.bukkit.Particle.WHITE_SMOKE,
                        entity.getLocation(), 300, 8, 3, 8, 0.1);
            }
        }.runTaskTimer(Main.getPlugin(), 0, 20);
    }

    private Player findNearestPlayer(LivingEntity entity) {
        return entity.getWorld().getPlayers().stream()
                .filter(player -> player.getLocation() != null)
                .min((p1, p2) -> {
                    Location loc1 = p1.getLocation();
                    Location loc2 = p2.getLocation();
                    Location entityLoc = entity.getLocation();

                    
                    if (loc1 == null)
                        return 1; // 如果 loc1 为 null，认为 p2 更近
                    if (loc2 == null)
                        return -1; // 如果 loc2 为 null，认为 p1 更近

                    return Double.compare(loc1.distanceSquared(entityLoc), loc2.distanceSquared(entityLoc));
                })
                .orElse(null);
    }

    public void setEntityScale(LivingEntity entity, double scale) {
        AttributeInstance scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(scale);
        }
    }

    // ======== 对外接口 ========
    public void spawnLowTierMonsters(List<Player> players) {
        spawnMonsters(players, 3 + players.size(), MonsterTier.LOW);
    }

    public void spawnMediumTierMonsters(List<Player> players) {
        spawnMonsters(players, 2 + players.size() / 2, MonsterTier.MEDIUM);
    }

    public void spawnHighTierMonsters(List<Player> players) {
        spawnMonsters(players, 1 + players.size() / 3, MonsterTier.HIGH);
    }

    public void spawnVeryHighTierMonsters(List<Player> players) {
        spawnMonsters(players, Math.max(1, players.size() / 4), MonsterTier.VERY_HIGH);
    }

    private enum MonsterTier {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }
}