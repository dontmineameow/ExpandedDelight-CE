package com.huidu.expandeddelight.listener;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CinnamonStripListener implements Listener {

    private final ExpandedDelightPlugin plugin;

    public CinnamonStripListener(ExpandedDelightPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAxeStripLog(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.getType().toString().endsWith("_AXE")) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;

        String blockId = com.huidu.farmersdelight.api.block.FarmersDelightBlocks.blockIdOf(block);
        if (blockId == null) return;

        String targetId = null;
        if ("expandeddelight:cinnamon_log".equals(blockId)) {
            targetId = "expandeddelight:stripped_cinnamon_log";
        } else if ("expandeddelight:cinnamon_wood".equals(blockId)) {
            targetId = "expandeddelight:stripped_cinnamon_wood";
        }

        if (targetId != null) {
            Player player = event.getPlayer();
            Location loc = block.getLocation();

            // 轉化為去皮肉桂原木/木頭
            CraftEngineBlocks.place(loc, Key.of(targetId), true);

            // 播放原版斧頭剝皮音效與揮手
            loc.getWorld().playSound(loc, Sound.ITEM_AXE_STRIP, 1.0f, 1.0f);
            player.swingMainHand();

            // 損耗斧頭耐久（非創造模式）
            if (player.getGameMode() != GameMode.CREATIVE) {
                FarmersDelightItems.damage(item, 1, loc);
            }

            // 必定掉落 1 根肉桂棒 (cinnamon_stick)
            ItemStack stick = FarmersDelightItems.createOrFallback("expandeddelight:cinnamon_stick", Material.STICK);
            loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), stick);

            // 解鎖進度
            com.huidu.expandeddelight.advancement.ExpandedDelightAdvancements.award(player, com.huidu.expandeddelight.advancement.ExpandedDelightAdvancements.ADV_STRIP_CINNAMON);

            event.setCancelled(true);
        }
    }
}
