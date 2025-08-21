package org.firedoge.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.firedoge.Main;
import org.firedoge.utils.Constants;

import net.kyori.adventure.text.Component;

public class MobSpawn {




    private static final Random random = ThreadLocalRandom.current();
    private List<Player> players = new ArrayList<>();
    private int totalCount = 0;




    
    private void spawnMonsters(List<Player> players, int baseCount, MonsterTier tier) {
        if (players.isEmpty())
            return;
        
        if (totalCount >= Constants.MAX_MONSTER_SPAWN_COUNT) {
            totalCount = 0; // Reset count if it exceeds the limit
            return;
        }

        this.players = players;
        Location center = calculateGroupCenter(players);
        int playerCount = players.size();
        // adjust mobs' count based on number of players
        int monsterCount = baseCount + (int) (playerCount * 5);

        for (int i = 0; i < monsterCount; i++) {
            
            double minDist = (tier == MonsterTier.LOW) ? 24 : 50;
            double maxDist = (tier == MonsterTier.LOW) ? 50 : 128;

            Location spawnLoc = findLegalSpawnPoint(center, minDist, maxDist);
            if (spawnLoc == null) continue; 
            World w = spawnLoc.getWorld();
            if (w == null) continue;


            EntityType type = selectMonsterType(tier);
            LivingEntity entity = (LivingEntity) w.spawnEntity(spawnLoc, type);


            if (Main.getPlugin().isInDebugMode)
                entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1));


            enhanceMonster(entity, tier, playerCount);

            totalCount++;
        }
    }


    @Nullable
    private Location findLegalSpawnPoint(Location center, double minDist, double maxDist) {
        for (int tries = 0; tries < 15; tries++) { // 最多尝试15次
            double angle = random.nextDouble() * 2 * Math.PI;

            double distance = minDist + random.nextDouble() * (maxDist - minDist);
            double offsetX = distance * Math.cos(angle);
            double offsetZ = distance * Math.sin(angle);

            Location candidate = center.clone().add(offsetX, 0, offsetZ);
            World w = candidate.getWorld();
            if (w == null)
                continue;

            
            candidate = w.getHighestBlockAt(candidate).getLocation().add(0, 1, 0);

            
            int light = candidate.getBlock().getLightLevel();
            if (light > 7)
                continue;

            
            if (!candidate.clone().subtract(0, 1, 0).getBlock().getType().isSolid())
                continue;
            if (!candidate.getBlock().isEmpty())
                continue;

            return candidate;
        }
        return null;
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
        List<EntityType> pool = new ArrayList<>();

        switch (tier) {
            case LOW -> {
                
                pool.add(EntityType.ZOMBIE);
                pool.add(EntityType.SKELETON);
                pool.add(EntityType.VINDICATOR);
                pool.add(EntityType.HUSK);
                pool.add(EntityType.DROWNED);
            }
            case MEDIUM -> {
                pool.add(EntityType.SPIDER);
                pool.add(EntityType.CAVE_SPIDER);
            }
            case HIGH -> {
                pool.add(EntityType.ENDERMAN);
                pool.add(EntityType.ENDERMITE);
                pool.add(EntityType.PHANTOM);
            }
            case VERY_HIGH -> {
                
                pool.add(EntityType.RAVAGER);
                pool.add(EntityType.WITHER_SKELETON);
                pool.add(EntityType.EVOKER);
            }
        }

        return pool.get(random.nextInt(pool.size()));
    }

    private void enhanceMonster(LivingEntity entity, MonsterTier tier, int playerCount) {
        double playerFactor = 1 + (playerCount * 0.1);
        if (entity == null)
            return;
        switch (tier) {
            case LOW -> {
                setEntityScale(entity, 0.5);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0));
            }

            case MEDIUM -> {
                setEntityScale(entity, 2.0);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0));

                if (random.nextDouble() < 0.3)
                    addWebShotAbility(entity);
                if (random.nextDouble() < 0.2)
                    addPoisonArrow(entity);
                spawnLowTierMonsters(players);
            }

            case HIGH -> {
                setEntityScale(entity, 5.0);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));

                if (entity instanceof Phantom phantom) {
                    phantom.setSize(8 + random.nextInt(4));
                }
                if (random.nextDouble() < 0.5)
                    addAuraEffect(entity);
                if (random.nextDouble() < 0.3)
                    addBlinkAbility(entity);
                spawnMediumTierMonsters(players);
            }

            case VERY_HIGH -> {
                setEntityScale(entity, 8.0 + playerFactor);
                entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 3));
                entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2));

                if (entity instanceof Ravager ravager) {
                    addGroundSlamAbility(ravager);
                } else {
                    addDarkAura(entity);
                }

                if (random.nextDouble() < 0.4)
                    addMinionSummon(entity);
            }
        }
    }


    private void addPoisonArrow(LivingEntity entity) {
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
                if (loc.distance(entity.getLocation()) < 15) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                    target.getWorld().spawnParticle(org.bukkit.Particle.SPORE_BLOSSOM_AIR, loc, 20);
                }
            }
        }.runTaskTimer(Main.getPlugin(), 0, 100);
    }

    private void addBlinkAbility(LivingEntity entity) {
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
                if (random.nextDouble() < 0.3) {
                    entity.teleport(loc.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1));
                    entity.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, entity.getLocation(), 50);
                }
            }
        }.runTaskTimer(Main.getPlugin(), 0, 120);
    }

    private void addMinionSummon(LivingEntity entity) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead())
                    cancel();
                Location loc = entity.getLocation();
                for (int i = 0; i < 2; i++) {
                    World w = loc.getWorld();
                    if (w == null)
                        return;
                    LivingEntity minion = (LivingEntity) w.spawnEntity(
                            loc.clone().add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1),
                            EntityType.ZOMBIE);
                    minion.customName(Component.text("§7小随从"));
                    minion.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
                }
                entity.getWorld().spawnParticle(org.bukkit.Particle.SOUL, loc, 30);
            }
        }.runTaskTimer(Main.getPlugin(), 0, 300);
    }

    // Special Skills
    private void addWebShotAbility(LivingEntity entity) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead())
                    this.cancel();

                Player target = findNearestPlayerWithinRange(entity, 3);
                if (target == null)
                    return;
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

        new BukkitRunnable() {
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

        new BukkitRunnable() {
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

        new BukkitRunnable() {
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

    @Nullable
    private Player findNearestPlayerWithinRange(LivingEntity entity, double range) {
        return entity.getWorld().getPlayers().stream()
                .filter(player -> {
                    Location location = player.getLocation();
                    return location != null && location.distance(entity.getLocation()) <= range;
                })
                .min((p1, p2) -> {
                    Location loc1 = p1.getLocation();
                    Location loc2 = p2.getLocation();
                    Location entityLoc = entity.getLocation();

                    if (loc1 == null || loc2 == null)
                        return 0; 
                    return Double.compare(loc1.distanceSquared(entityLoc), loc2.distanceSquared(entityLoc));
                })
                .orElse(null);
    }

    public static void setEntityScale(LivingEntity entity, double scale) {
        AttributeInstance scaleAttr = entity.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(scale);
        }
    }


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