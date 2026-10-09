package com.huidu.expandeddelight.tree;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import com.huidu.farmersdelight.api.block.FarmersDelightBlocks;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class CinnamonTreeManager implements Listener {

    private final ExpandedDelightPlugin plugin;

    private static final Key LOG_KEY = Key.of("expandeddelight:cinnamon_log");
    private static final Key LEAVES_KEY = Key.of("expandeddelight:cinnamon_leaves");

    public CinnamonTreeManager(ExpandedDelightPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 骨粉催熟肉桂樹苗
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSaplingBonemeal(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;

        String blockId = FarmersDelightBlocks.blockIdOf(block);
        if (!"expandeddelight:cinnamon_sapling".equals(blockId)) {
            return;
        }

        ItemStack handItem = event.getItem();
        if (handItem == null || handItem.getType() != Material.BONE_MEAL) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        // 消耗骨粉
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            handItem.setAmount(handItem.getAmount() - 1);
        }

        Location loc = block.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.05);
        world.playSound(loc, Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);

        // 45% 機率成功長成肉桂樹
        if (ThreadLocalRandom.current().nextFloat() < 0.45f) {
            growCinnamonTree(loc);
        }
    }

    /**
     * 肉桂樹葉破壞掉落自訂處理 (樹葉、樹苗、木棍、肉桂棒)
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onLeavesBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String blockId = FarmersDelightBlocks.blockIdOf(block);
        if (!"expandeddelight:cinnamon_leaves".equals(blockId)) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        Location dropLoc = block.getLocation().clone().add(0.5, 0.5, 0.5);
        World world = block.getWorld();

        // 剪刀或精準採集直接掉落樹葉本體
        boolean hasSilk = tool.containsEnchantment(Enchantment.SILK_TOUCH);
        boolean isShears = tool.getType() == Material.SHEARS;

        if (hasSilk || isShears) {
            ItemStack leavesItem = FarmersDelightItems.createOrFallback("expandeddelight:cinnamon_leaves", Material.OAK_LEAVES);
            world.dropItemNaturally(dropLoc, leavesItem);
            return;
        }

        // 時運加成
        int fortune = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        float saplingChance = 0.05f + fortune * 0.025f; // 基礎 5%，時運每級 +2.5%
        float stickChance = 0.02f + fortune * 0.01f;

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // 掉落肉桂樹苗
        if (rand.nextFloat() < saplingChance) {
            ItemStack sapling = FarmersDelightItems.createOrFallback("expandeddelight:cinnamon_sapling", Material.OAK_SAPLING);
            world.dropItemNaturally(dropLoc, sapling);
        }

        // 掉落肉桂棒 (5%)
        if (rand.nextFloat() < (0.05f + fortune * 0.02f)) {
            ItemStack cinnamonStick = FarmersDelightItems.createOrFallback("expandeddelight:cinnamon_stick", Material.STICK);
            world.dropItemNaturally(dropLoc, cinnamonStick);
        }

        // 掉落普通木棍
        if (rand.nextFloat() < stickChance) {
            world.dropItemNaturally(dropLoc, new ItemStack(Material.STICK, rand.nextInt(1, 3)));
        }
    }

    /**
     * 生成肉桂樹結構
     * 樹幹高 5~7 格，樹冠由肉桂樹葉緊密包裹頂端 3~4 層
     */
    public boolean growCinnamonTree(Location baseLoc) {
        World world = baseLoc.getWorld();
        if (world == null) return false;

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int trunkHeight = 5 + rand.nextInt(3); // 5~7 格高

        // 檢查上方空間是否足夠
        for (int y = 1; y <= trunkHeight + 2; y++) {
            Block above = baseLoc.clone().add(0, y, 0).getBlock();
            if (!above.isPassable() && above.getType() != Material.AIR) {
                return false;
            }
        }

        // 1. 清除樹苗方塊
        CraftEngineBlocks.remove(baseLoc.getBlock());
        baseLoc.getBlock().setType(Material.AIR);

        // 2. 生成樹幹 (Cinnamon Log)
        for (int y = 0; y < trunkHeight; y++) {
            Location logLoc = baseLoc.clone().add(0, y, 0);
            CraftEngineBlocks.place(logLoc, LOG_KEY, true);
        }

        // 3. 生成肉桂樹葉 (Cinnamon Foliage)
        // 樹冠結構：頂端向上 1 格 (十字或單格)，以及頂端向下 1~3 格的環狀樹葉
        int topY = trunkHeight - 1;

        // 頂層 (topY + 1)
        placeLeavesIfAir(baseLoc.clone().add(0, topY + 1, 0));
        placeLeavesIfAir(baseLoc.clone().add(1, topY + 1, 0));
        placeLeavesIfAir(baseLoc.clone().add(-1, topY + 1, 0));
        placeLeavesIfAir(baseLoc.clone().add(0, topY + 1, 1));
        placeLeavesIfAir(baseLoc.clone().add(0, topY + 1, -1));

        // 第二層 (topY) 與 第三層 (topY - 1)：3x3 樹葉
        for (int dy = topY; dy >= topY - 1; dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    placeLeavesIfAir(baseLoc.clone().add(dx, dy, dz));
                }
            }
        }

        // 第四層 (topY - 2)：十字與角落部分延伸
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                    if (rand.nextBoolean()) continue; // 角落圓角
                }
                placeLeavesIfAir(baseLoc.clone().add(dx, topY - 2, dz));
            }
        }

        // 第五層 (topY - 3)：部分隨機垂葉
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (rand.nextFloat() < 0.6f) {
                    placeLeavesIfAir(baseLoc.clone().add(dx, topY - 3, dz));
                }
            }
        }

        world.playSound(baseLoc, Sound.BLOCK_GRASS_PLACE, 1.0f, 0.8f);
        return true;
    }

    private void placeLeavesIfAir(Location loc) {
        Block block = loc.getBlock();
        String currentId = FarmersDelightBlocks.blockIdOf(block);
        // 如果已經是原木，不覆蓋
        if (currentId != null && currentId.contains("log")) {
            return;
        }
        if (block.isEmpty() || block.isPassable()) {
            CraftEngineBlocks.place(loc, LEAVES_KEY, true);
        }
    }
}
