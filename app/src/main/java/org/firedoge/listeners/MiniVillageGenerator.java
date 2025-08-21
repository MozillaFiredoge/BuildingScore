package org.firedoge.listeners;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.firedoge.Main;
import org.firedoge.models.Structure;

public class MiniVillageGenerator implements Listener {
    private final JavaPlugin plugin;
    private final Random random;
    private List<Structure> structures;

    // 原版村庄生成的生物群系列表
    private final List<Biome> validVillageBiomes = List.of(
            Biome.PLAINS,
            Biome.DESERT,
            Biome.SAVANNA,
            Biome.TAIGA,
            Biome.SNOWY_TAIGA,
            Biome.SUNFLOWER_PLAINS);

    public MiniVillageGenerator() {
        this.plugin = Main.getPlugin();
        this.random = new Random();
        try {
            this.structures = Structure.loadAllStructuresFromResources();
            plugin.getLogger().log(Level.FINE, "Loaded all {0} structures", this.structures.size());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load structures", e);
        }
    }

    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent e) {
        Chunk chunk = e.getChunk();
        tryPlaceVillageAtChunk(chunk);
    }

    /**
     * 公开方法：在给定的区块尝试生成村庄。外部（例如自定义 ChunkGenerator/Populator、datapack
     * hook）应在合适时机调用此方法。
     *
     * 返回 true 表示成功生成。
     */
    public boolean tryPlaceVillageAtChunk(Chunk chunk) {
        World world = chunk.getWorld();

        // 只在主世界 "world" 生成（和你原先的逻辑一致）
        if (!world.getName().equals("world"))
            return false;

        // 获取区块中心座标
        int x = (chunk.getX() << 4) + 8;
        int z = (chunk.getZ() << 4) + 8;
        int y = world.getHighestBlockYAt(x, z);

        // 不在海平面或水面上生成
        int sea = world.getSeaLevel();
        Block centerBlock = world.getBlockAt(x, y, z);
        Block below = world.getBlockAt(x, y - 1, z);
        if (y <= sea + 1)
            return false; // 太低，接近海面
        if (centerBlock.getType() == Material.WATER || below.getType() == Material.WATER)
            return false;

        // 检查生物群系
        Biome biome = world.getBiome(x, y, z);
        if (!validVillageBiomes.contains(biome))
            return false;

        // 检查区域是否相对平坦
        if (!isAreaRelativelyFlat(world, x, z, 6, 3))
            return false;

        // 随机决策
        if (random.nextDouble() < 0.02) {
            Location center = new Location(world, x, y + 1, z);
            generateVillage(world, center);
            plugin.getLogger().log(Level.INFO, "Generated a MiniVillage at {0} {1} {2}", new Object[] { x, y, z });
            return true;
        }

        return false;
    }

    private boolean isAreaRelativelyFlat(World world, int centerX, int centerZ, int radius, int maxHeightVariation) {
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int x = centerX - radius; x <= centerX + radius; x += 4) {
            for (int z = centerZ - radius; z <= centerZ + radius; z += 4) {
                int y = world.getHighestBlockYAt(x, z);
                if (y < minY)
                    minY = y;
                if (y > maxY)
                    maxY = y;

                if (maxY - minY > maxHeightVariation) {
                    return false;
                }
            }
        }
        return true;
    }

    public void generateVillage(World world, Location center) {
        // 1. 放置井（如果定义了 well 这个结构）
        placeWell(center);

        // 2. 决定建筑数量 (10-17座建筑)
        int buildingCount = 10 + random.nextInt(7);

        // 3. 生成建筑
        List<Location> buildingLocations = new ArrayList<>();

        for (int i = 0; i < buildingCount; i++) {
            Location buildingLoc = findSuitableBuildingLocation(world, center, buildingLocations, 10, 20);
            if (buildingLoc != null) {

                // IMPORTANT ONE BLOCK ABOVE THE GROUND!!
                buildingLoc = buildingLoc.clone().add(0, 1, 0);
                placeBuilding(buildingLoc);
                buildingLocations.add(buildingLoc);

                // 连接到中心或其他建筑：现在使用 A* 铺路
                Location connectTo;
                if (buildingLocations.size() > 1) {
                    connectTo = buildingLocations.get(random.nextInt(buildingLocations.size() - 1));
                } else {
                    connectTo = center;
                }
                placePathAStar(world, buildingLoc, connectTo);

                // 装饰
                decorateAround(world, buildingLoc, 2);
            }
        }

        // 装饰中心
        decorateAround(world, center, 4);

        // 添加边界
        addVillageBoundary(world, center, 25);
    }

    private Location findSuitableBuildingLocation(World world, Location center, List<Location> existingLocs,
            int minDist, int maxDist) {
        for (int attempts = 0; attempts < 30; attempts++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            int dist = minDist + random.nextInt(Math.max(1, maxDist - minDist));
            int x = center.getBlockX() + (int) (Math.cos(angle) * dist);
            int z = center.getBlockZ() + (int) (Math.sin(angle) * dist);
            int y = world.getHighestBlockYAt(x, z);

            Location candidate = new Location(world, x, y, z);

            boolean tooClose = false;
            for (Location existing : existingLocs) {
                if (existing.getWorld().equals(candidate.getWorld()) && candidate.distance(existing) < 8) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose && isAreaRelativelyFlat(world, x, z, 4, 2)) {
                return candidate;
            }
        }
        return null;
    }

    private void placeWell(Location center) {

        for (Structure st : structures) {
            if ("well".equalsIgnoreCase(st.getStructureName())) {
                st.placeStructure(center);
                return;
            }
        }
    }

    private void placeTree(Location loc) {
        for (Structure st : structures) {
            if ("tree".equalsIgnoreCase(st.getStructureName())) {
                st.placeStructure(loc);
                return;
            }
        }
    }

    private void placeBuilding(Location loc) {
        if (structures.isEmpty())
            return;
        Structure building = structures.get(random.nextInt(structures.size()));
        if (building.getStructureName().equalsIgnoreCase("well")
                || building.getStructureName().equalsIgnoreCase("tree"))
            return;
        building.placeStructure(loc);
    }

    /*
     * -------------------
     * A* 铺路实现（用于替换原来的生成自然路径方法）
     * -------------------
     */

    private static final int[] DX = { -1, 1, 0, 0, -1, -1, 1, 1 };
    private static final int[] DZ = { 0, 0, -1, 1, -1, 1, -1, 1 };

    private void placePathAStar(World world, Location startLoc, Location endLoc) {
        // A* 在地表 x,z 网格上搜索路径，基于 world.getHighestBlockYAt
        int sx = startLoc.getBlockX();
        int sz = startLoc.getBlockZ();
        int ex = endLoc.getBlockX();
        int ez = endLoc.getBlockZ();

        List<int[]> path = computeAStarPath(world, sx, sz, ex, ez, 2000);
        if (path == null || path.isEmpty()) {
            // 回退到简单直线（保底）
            generateNaturalPathFallback(world, sx, sz, ex, ez);
            return;
        }

        // 将路径铺成 DIRT_PATH 并偶尔放光源
        for (int[] p : path) {
            int x = p[0], z = p[1];
            int y = world.getHighestBlockYAt(x, z);
            Block ground = world.getBlockAt(x, y, z);
            if (ground.getType() == Material.GRASS_BLOCK ||
                    ground.getType() == Material.DIRT ||
                    ground.getType() == Material.COARSE_DIRT ||
                    ground.getType() == Material.PODZOL) {

                // 把地表改为路径（在最高点的下方）
                ground.setType(Material.DIRT_PATH);

                // 放置光源的概率（经 A* 路径，光源可沿着路径放）
                if (random.nextInt(5) == 0) {
                    // 放置灯或火把（火把贴在地面上）
                    Block above = world.getBlockAt(x, y + 1, z);
                    if (above.getType() == Material.AIR) {

                        // 尝试放灯笼（需要下方有支撑）
                        if (ground.getType().isSolid()) {
                            above.setType(Material.LANTERN);
                        }

                    }
                }
            }
        }
    }

    /**
     * A* 实现（基于 x,z 网格）
     * - maxNodes 限制搜索规模以防止死循环
     * - 返回的路径是从 start 到 end 的 (x,z) 坐标列表（包含 start，包含 end）
     */
    private List<int[]> computeAStarPath(World world, int sx, int sz, int ex, int ez, int maxNodes) {
        class Node {
            int x, z;
            double g, f;
            Node parent;

            Node(int x, int z, double g, double f, Node parent) {
                this.x = x;
                this.z = z;
                this.g = g;
                this.f = f;
                this.parent = parent;
            }

            @Override
            public int hashCode() {
                return Objects.hash(x, z);
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Node))
                    return false;
                Node other = (Node) o;
                return other.x == x && other.z == z;
            }
        }

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<Long, Double> gScore = new HashMap<>();
        Set<Long> closed = new HashSet<>();

        long startKey = (((long) sx) << 32) ^ (sz & 0xffffffffL);
        Node startNode = new Node(sx, sz, 0, heuristic(sx, sz, ex, ez), null);
        open.add(startNode);
        gScore.put(startKey, 0.0);

        int nodes = 0;
        while (!open.isEmpty() && nodes < maxNodes) {
            Node cur = open.poll();
            nodes++;
            long curKey = (((long) cur.x) << 32) ^ (cur.z & 0xffffffffL);
            if (closed.contains(curKey))
                continue;
            closed.add(curKey);

            if (cur.x == ex && cur.z == ez) {
                // 重建路径
                LinkedList<int[]> path = new LinkedList<>();
                Node it = cur;
                while (it != null) {
                    path.addFirst(new int[] { it.x, it.z });
                    it = it.parent;
                }
                return path;
            }

            // 遍历邻居（8 向）
            for (int i = 0; i < DX.length; i++) {
                int nx = cur.x + DX[i];
                int nz = cur.z + DZ[i];
                long nKey = (((long) nx) << 32) ^ (nz & 0xffffffffL);
                if (closed.contains(nKey))
                    continue;

                // 检查此位置是否可通过（不是水面、不是熔岩、在合理的高度差范围内）
                int ny = world.getHighestBlockYAt(nx, nz);
                Block ground = world.getBlockAt(nx, ny - 1, nz);
                Material gm = ground.getType();
                if (gm == Material.WATER || gm == Material.LAVA)
                    continue;
                if (Math.abs(ny - world.getHighestBlockYAt(cur.x, cur.z)) > 3)
                    continue; // 高差过大不可走

                double moveCost = (DX[i] != 0 && DZ[i] != 0) ? Math.sqrt(2) : 1.0;
                double tentativeG = cur.g + moveCost + slopePenalty(world, cur.x, cur.z, nx, nz);

                Double prevG = gScore.get(nKey);
                if (prevG == null || tentativeG < prevG) {
                    double f = tentativeG + heuristic(nx, nz, ex, ez);
                    Node neighbor = new Node(nx, nz, tentativeG, f, cur);
                    gScore.put(nKey, tentativeG);
                    open.add(neighbor);
                }
            }
        }

        return null; // 未找到路径
    }

    private double heuristic(int x, int z, int tx, int tz) {
        // 使用对角距离 (Octile) 的启发函数
        int dx = Math.abs(x - tx);
        int dz = Math.abs(z - tz);
        int min = Math.min(dx, dz);
        int max = Math.max(dx, dz);
        return (Math.sqrt(2) * min + (max - min));
    }

    private double slopePenalty(World world, int x1, int z1, int x2, int z2) {
        int y1 = world.getHighestBlockYAt(x1, z1);
        int y2 = world.getHighestBlockYAt(x2, z2);
        return Math.abs(y1 - y2); // 简单惩罚：高度差越大成本越高
    }

    /**
     * 算法失败时的回退方法：简单的直线/逐步逼近（保底）
     */
    private void generateNaturalPathFallback(World world, int startX, int startZ, int endX, int endZ) {
        int x = startX, z = startZ;
        while (x != endX || z != endZ) {
            int y = world.getHighestBlockYAt(x, z);
            Block block = world.getBlockAt(x, y - 1, z);
            if (block.getType() == Material.GRASS_BLOCK ||
                    block.getType() == Material.DIRT ||
                    block.getType() == Material.COARSE_DIRT) {
                block.setType(Material.DIRT_PATH);
                if (random.nextInt(10) == 0)
                    placeLightSource(world, x, y, z);
            }

            if (x < endX)
                x++;
            else if (x > endX)
                x--;
            if (z < endZ)
                z++;
            else if (z > endZ)
                z--;

            if (random.nextInt(4) == 0) {
                x += random.nextInt(3) - 1;
                z += random.nextInt(3) - 1;
            }
        }
    }

    private void placeLightSource(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() == Material.AIR) {
            Block below = world.getBlockAt(x, y, z);
            if (below.getType().isSolid()) {
                block.setType(Material.LANTERN);
            }
        }
    }

    /*
     * -------------------
     * 其余装饰与边界逻辑保留（和原版类似）
     * -------------------
     */

    private void decorateAround(World world, Location loc, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = loc.getBlockX() + dx;
                int z = loc.getBlockZ() + dz;
                int y = world.getHighestBlockYAt(x, z);

                Block ground = world.getBlockAt(x, y, z);
                Block decor = world.getBlockAt(x, y + 1, z);

                if (ground.getType() == Material.GRASS_BLOCK && decor.getType() == Material.AIR) {
                    double r = random.nextDouble();
                    if (r < 0.3) {
                        decor.setType(Material.TALL_GRASS);
                    } else if (r < 0.5) {
                        decor.setType(Material.FERN);
                    } else if (r < 0.6) {
                        Material[] flowers = {
                                Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID,
                                Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP,
                                Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP
                        };
                        decor.setType(flowers[random.nextInt(flowers.length)]);
                    } else if (r < 0.7) {
                        placeTree(decor.getLocation());
                    }
                }
            }
        }
    }

    private void addVillageBoundary(World world, Location center, int radius) {
        for (int i = 0; i < 360; i++) {
            double angle = 2 * Math.PI * i / 360;
            int x = center.getBlockX() + (int) (Math.cos(angle) * radius);
            int z = center.getBlockZ() + (int) (Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z);

            Block block = world.getBlockAt(x, y + 1, z);
            Block blockBelow = world.getBlockAt(x, y, z);

            if (block.getType() == Material.AIR && blockBelow.getType().isSolid()) {
                if (random.nextBoolean()) {
                    block.setType(Material.OAK_FENCE);
                } else {
                    block.setType(Material.COBBLESTONE_WALL);
                }

                if (random.nextInt(8) == 0) {
                    for (int j = -1; j <= 1; j++) {
                        int gateX = x + j * (int) Math.signum((int) Math.cos(angle));
                        int gateZ = z + j * (int) Math.signum((int) Math.sin(angle));
                        int gateY = world.getHighestBlockYAt(gateX, gateZ);

                        Block gateBlock = world.getBlockAt(gateX, gateY, gateZ);
                        if (gateBlock.getType() == Material.AIR) {
                            gateBlock.setType(Material.OAK_FENCE_GATE);
                            // 方向设置如果需要可以使用 BlockData，根据 angle 调整 Facing
                        }
                    }
                }
            }
        }
    }
}
