package com.huidu.expandeddelight.listener;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import com.huidu.farmersdelight.api.block.FarmersDelightBlocks;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 監聽鹽礦石破壞事件：
 * - 精準採集 (Silk Touch): 掉落礦石方塊本身
 * - 一般破壞: 掉落 1~2 個鹽岩 (salt_rock)，受時運附魔影響，並掉落經驗
 */
public class SaltOreListener implements Listener {

    private final ExpandedDelightPlugin plugin;

    public SaltOreListener(ExpandedDelightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        String blockId = FarmersDelightBlocks.blockIdOf(event.getBlock());
        if (!"expandeddelight:salt_ore".equals(blockId) && !"expandeddelight:deepslate_salt_ore".equals(blockId)) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        // 需為稿子才能有效開採
        if (tool == null || !tool.getType().toString().endsWith("_PICKAXE")) {
            return;
        }

        // 阻止預設掉落 (solid_block_template 預設可能掉落基底石頭或自身)
        event.setDropItems(false);
        Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);

        // 判斷精準採集
        if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            ItemStack oreBlock = FarmersDelightItems.createOrFallback(blockId, 
                    "expandeddelight:deepslate_salt_ore".equals(blockId) ? Material.DEEPSLATE : Material.STONE);
            loc.getWorld().dropItemNaturally(loc, oreBlock);
            return;
        }

        // 一般開採：掉落鹽岩 + 時運附魔加成
        int fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        int baseDrop = ThreadLocalRandom.current().nextInt(1, 3); // 1~2 個
        int extraDrop = 0;
        if (fortuneLevel > 0) {
            extraDrop = ThreadLocalRandom.current().nextInt(fortuneLevel + 1);
        }
        int totalAmount = baseDrop + extraDrop;

        ItemStack saltRock = FarmersDelightItems.createOrFallback("expandeddelight:salt_rock", Material.SUGAR);
        saltRock.setAmount(totalAmount);
        loc.getWorld().dropItemNaturally(loc, saltRock);

        // 掉落少量經驗 (1 ~ 3 exp)
        int exp = ThreadLocalRandom.current().nextInt(1, 4);
        event.setExpToDrop(exp);
    }
}
