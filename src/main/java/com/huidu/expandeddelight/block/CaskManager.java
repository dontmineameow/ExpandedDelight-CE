package com.huidu.expandeddelight.block;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import com.huidu.farmersdelight.api.FarmersDelightApi;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.BlockDefinition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 管理發酵桶 (Cask, Milk Cask, Goat Milk Cask, Cheese Cask, Goat Cheese Cask)
 * 完整還原原版 Mod 的牛奶/山羊奶熟成邏輯、周圍水源/光照/活化劑加速、破壞保存與山羊擠奶。
 */
public class CaskManager implements Listener {

    public static final String CASK_ID = "expandeddelight:cask";
    public static final String MILK_CASK_ID = "expandeddelight:milk_cask";
    public static final String GOAT_MILK_CASK_ID = "expandeddelight:goat_milk_cask";
    public static final String CHEESE_CASK_ID = "expandeddelight:cheese_cask";
    public static final String GOAT_CHEESE_CASK_ID = "expandeddelight:goat_cheese_cask";
    public static final String GOAT_MILK_BUCKET_ID = "expandeddelight:goat_milk_bucket";

    public static final int MAX_STAGE = 7;

    private final ExpandedDelightPlugin plugin;
    private final CaskStorage storage;
    // 記錄發酵中的木桶位置與當前發酵階段 (0 ~ 7)
    private final Map<Location, Integer> fermentingStages = new ConcurrentHashMap<>();

    public CaskManager(ExpandedDelightPlugin plugin) {
        this.plugin = plugin;
        this.storage = new CaskStorage(plugin);
        loadData();
        startFermentationTicker();
    }

    public void loadData() {
        storage.load(fermentingStages);
    }

    public void saveData() {
        storage.save(fermentingStages);
    }

    /**
     * 山羊擠奶機制：玩家持空鐵桶右鍵成年山羊獲得山羊奶桶
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onGoatInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Goat goat)) return;
        if (!goat.isAdult()) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItem(EquipmentSlot.HAND);
        if (held.getType() != Material.BUCKET) return;

        // 擠奶音效
        player.playSound(goat.getLocation(), Sound.ENTITY_GOAT_MILK, 1.0f, 1.0f);
        event.setCancelled(true);

        ItemStack goatMilk = FarmersDelightItems.createOrFallback(GOAT_MILK_BUCKET_ID, Material.MILK_BUCKET);
        com.huidu.expandeddelight.advancement.ExpandedDelightAdvancements.award(player, com.huidu.expandeddelight.advancement.ExpandedDelightAdvancements.ADV_GOAT_MILK);
        if (!player.getGameMode().toString().equals("CREATIVE")) {
            held.setAmount(held.getAmount() - 1);
            if (held.getAmount() <= 0) {
                player.getInventory().setItem(EquipmentSlot.HAND, goatMilk);
                return;
            }
        }

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(goatMilk);
        } else {
            goat.getWorld().dropItemNaturally(player.getLocation(), goatMilk);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;

        String blockId = com.huidu.farmersdelight.api.block.FarmersDelightBlocks.blockIdOf(block);
        if (blockId == null || !blockId.startsWith("expandeddelight:")) {
            return;
        }

        ItemStack item = event.getItem();
        Location loc = block.getLocation();
        Player player = event.getPlayer();

        // 1. 空發酵桶 (cask) 倒入牛奶
        if (CASK_ID.equals(blockId) && item != null) {
            boolean isCowMilk = item.getType() == Material.MILK_BUCKET;
            boolean isGoatMilk = FarmersDelightItems.matchesId(item, GOAT_MILK_BUCKET_ID);

            if (isCowMilk || isGoatMilk) {
                String targetCaskId = isCowMilk ? MILK_CASK_ID : GOAT_MILK_CASK_ID;
                BlockDefinition targetDef = CraftEngineBlocks.byId(net.momirealms.craftengine.core.util.Key.of(targetCaskId));
                if (targetDef != null) {
                    CraftEngineBlocks.place(loc, targetDef.defaultState(), true);
                }

                player.playSound(loc, Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f);
                if (!player.getGameMode().toString().equals("CREATIVE")) {
                    item.setAmount(item.getAmount() - 1);
                    ItemStack emptyBucket = new ItemStack(Material.BUCKET);
                    if (item.getAmount() <= 0) {
                        player.getInventory().setItem(EquipmentSlot.HAND, emptyBucket);
                    } else {
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(emptyBucket);
                        } else {
                            loc.getWorld().dropItemNaturally(player.getLocation(), emptyBucket);
                        }
                    }
                }

                fermentingStages.put(loc, 0);
                com.huidu.expandeddelight.advancement.ExpandedDelightAdvancements.award(player, com.huidu.expandeddelight.advancement.ExpandedDelightAdvancements.ADV_CASK);
                event.setCancelled(true);
                return;
            }
        }

        // 2. 右鍵成熟起司桶採收起司 (熟成起司桶或熟成山羊起司桶)
        if (CHEESE_CASK_ID.equals(blockId) || GOAT_CHEESE_CASK_ID.equals(blockId)) {
            fermentingStages.remove(loc);
            boolean isGoat = GOAT_CHEESE_CASK_ID.equals(blockId);
            String cheeseId = isGoat ? "expandeddelight:goat_cheese_wheel" : "expandeddelight:cheese_wheel";

            ItemStack cheeseWheel = FarmersDelightItems.createOrFallback(cheeseId, Material.BREAD);
            cheeseWheel.setAmount(2);
            loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), cheeseWheel);
            player.playSound(loc, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f);
            com.huidu.expandeddelight.advancement.ExpandedDelightAdvancements.award(player, com.huidu.expandeddelight.advancement.ExpandedDelightAdvancements.ADV_CHEESE);

            // 還原為空發酵桶 (cask)
            BlockDefinition caskDef = CraftEngineBlocks.byId(net.momirealms.craftengine.core.util.Key.of(CASK_ID));
            if (caskDef != null) {
                CraftEngineBlocks.place(loc, caskDef.defaultState(), true);
            }

            event.setCancelled(true);
            return;
        }

        // 3. 玩家空手右鍵檢查未成熟桶進度（提示音效或粒子）
        if (MILK_CASK_ID.equals(blockId) || GOAT_MILK_CASK_ID.equals(blockId)) {
            if (item == null || item.getType().isAir()) {
                int stage = fermentingStages.getOrDefault(loc, 0);
                loc.getWorld().spawnParticle(Particle.EGG_CRACK, loc.clone().add(0.5, 1.05, 0.5), 3, 0.15, 0.1, 0.15, 0.0);
                player.playSound(loc, Sound.BLOCK_WOOD_STEP, 0.8f, 1.2f);
                event.setCancelled(true);
            }
        }
    }

    /**
     * 當發酵桶被破壞時，清理快取記錄
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String blockId = com.huidu.farmersdelight.api.block.FarmersDelightBlocks.blockIdOf(block);
        if (blockId != null && blockId.startsWith("expandeddelight:") && blockId.contains("cask")) {
            fermentingStages.remove(block.getLocation());
        }
    }

    /**
     * 發酵循環：完美比照原版 MilkCaskBlock.randomTick
     * - 基礎機率：光照 < 5 增加機率 (0.1F vs 0.05F)
     * - 周圍 3x3x3 含有水源增加 0.1F 機率
     * - 周圍含有活化劑 (如乾草塊、蘑菇等有機質) 各增加 0.02F 機率
     * - 每階段達 MAX_STAGE (7) 時，將牛奶桶轉換為起司桶
     */
    private void startFermentationTicker() {
        FarmersDelightApi.get().runRepeating(() -> {
            for (Map.Entry<Location, Integer> entry : fermentingStages.entrySet()) {
                Location loc = entry.getKey();
                if (loc.getWorld() == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    continue;
                }

                Block block = loc.getBlock();
                String blockId = com.huidu.farmersdelight.api.block.FarmersDelightBlocks.blockIdOf(block);
                if (blockId == null || (!MILK_CASK_ID.equals(blockId) && !GOAT_MILK_CASK_ID.equals(blockId))) {
                    fermentingStages.remove(loc);
                    continue;
                }

                int stage = entry.getValue();

                // 隨機發酵粒子 (原版 EGG_CRACK 冒泡)
                if (ThreadLocalRandom.current().nextInt(5) == 0) {
                    loc.getWorld().spawnParticle(
                            Particle.EGG_CRACK,
                            loc.getX() + 0.2 + ThreadLocalRandom.current().nextDouble() * 0.6,
                            loc.getY() + 1.05,
                            loc.getZ() + 0.2 + ThreadLocalRandom.current().nextDouble() * 0.6,
                            1, 0.0, 0.01, 0.0, 0.0
                    );
                }

                // 計算成長機率 (參照原版 MilkCaskBlock)
                float chance = 0.05F;
                int light = block.getLightLevel();
                if (light < 5) {
                    chance += 0.05F; // 黑暗環境加速
                }

                // 檢測周圍 3x3x3 方塊環境
                boolean hasWater = false;
                int activatorCount = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            Block neighbor = block.getRelative(dx, dy, dz);
                            Material type = neighbor.getType();
                            if (type == Material.WATER) {
                                hasWater = true;
                            } else if (type == Material.HAY_BLOCK || type == Material.BROWN_MUSHROOM || type == Material.RED_MUSHROOM || type == Material.MYCELIUM) {
                                activatorCount++;
                            }
                        }
                    }
                }

                if (hasWater) chance += 0.1F;
                chance += Math.min(0.2F, activatorCount * 0.02F);

                // 每次定時檢查 (每 40 ticks = 2 秒執行一次檢測)
                if (ThreadLocalRandom.current().nextFloat() <= chance) {
                    if (stage >= MAX_STAGE) {
                        // 熟成完成！替換方塊為對應的起司桶
                        fermentingStages.remove(loc);
                        String targetCaskId = MILK_CASK_ID.equals(blockId) ? CHEESE_CASK_ID : GOAT_CHEESE_CASK_ID;
                        BlockDefinition targetDef = CraftEngineBlocks.byId(net.momirealms.craftengine.core.util.Key.of(targetCaskId));
                        if (targetDef != null) {
                            CraftEngineBlocks.place(loc, targetDef.defaultState(), true);
                        }
                        loc.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_PLACE, 1.0f, 1.0f);
                        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 1.1, 0.5), 5, 0.2, 0.1, 0.2, 0.0);
                    } else {
                        entry.setValue(stage + 1);
                    }
                }
            }
        }, 40L, 40L);
    }
}
