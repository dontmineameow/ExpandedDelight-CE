package com.huidu.expandeddelight.tree;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;

import java.util.Random;

public class CinnamonTreePopulator implements Listener {

    private final CinnamonTreeManager treeManager;

    public CinnamonTreePopulator(CinnamonTreeManager treeManager) {
        this.treeManager = treeManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        World world = event.getWorld();
        // 僅限主世界
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        Chunk chunk = event.getChunk();
        Random random = new Random((long) chunk.getX() * 341873128712L + (long) chunk.getZ() * 132897987541L);

        // 檢查該區塊中心生態系是否為叢林
        Biome biome = world.getBiome(chunk.getBlock(8, 64, 8).getLocation());
        if (!isJungleBiome(biome)) {
            return;
        }

        // 稀疏叢林生成機率較高 (約 25%)，一般密林約 12%
        float chance = (biome == Biome.SPARSE_JUNGLE) ? 0.25f : 0.12f;
        if (random.nextFloat() > chance) {
            return;
        }

        // 在區塊內選取 1~2 個位置嘗試生成
        int attempts = 1 + random.nextInt(2);
        for (int i = 0; i < attempts; i++) {
            int x = 2 + random.nextInt(12);
            int z = 2 + random.nextInt(12);
            Block highestBlock = world.getHighestBlockAt(chunk.getX() * 16 + x, chunk.getZ() * 16 + z, HeightMap.MOTION_BLOCKING_NO_LEAVES);

            // 必須是草方塊或泥土
            Material type = highestBlock.getType();
            if (type == Material.GRASS_BLOCK || type == Material.DIRT) {
                Block targetAir = highestBlock.getRelative(0, 1, 0);
                if (targetAir.isEmpty()) {
                    treeManager.growCinnamonTree(targetAir.getLocation());
                    break;
                }
            }
        }
    }

    private boolean isJungleBiome(Biome biome) {
        if (biome == null) return false;
        String name = biome.name();
        return name.contains("JUNGLE");
    }
}
