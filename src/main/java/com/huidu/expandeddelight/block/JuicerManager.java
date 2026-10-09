package com.huidu.expandeddelight.block;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import com.huidu.expandeddelight.gui.JuicerGui;
import com.huidu.expandeddelight.recipe.JuicerRecipe;
import com.huidu.expandeddelight.recipe.RecipeLoader;
import com.huidu.farmersdelight.api.FarmersDelightApi;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.item.SlotPlaceholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理伺服器中所有放置的榨汁機方塊狀態、GUI 與運作 Tick
 */
public class JuicerManager implements Listener {

    private final ExpandedDelightPlugin plugin;
    private final Map<Location, JuicerBlockEntity> blockEntities = new ConcurrentHashMap<>();
    private final Map<Location, JuicerGui> openGuis = new ConcurrentHashMap<>();
    private final JuicerStorage storage;

    public JuicerManager(ExpandedDelightPlugin plugin) {
        this.plugin = plugin;
        this.storage = new JuicerStorage(plugin);
        loadData();
        startJuicerLoop();
    }

    public void loadData() {
        storage.load(blockEntities);
    }

    public void saveData() {
        // 先同步所有開啟中 GUI 的狀態
        for (JuicerGui gui : openGuis.values()) {
            gui.syncToBlockEntity();
        }
        storage.save(blockEntities);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractJuicer(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;

        String blockId = com.huidu.farmersdelight.api.block.FarmersDelightBlocks.blockIdOf(block);
        if (!"expandeddelight:juicer".equals(blockId)) {
            return;
        }

        Player player = event.getPlayer();
        if (player.isSneaking() && event.getItem() != null) {
            return;
        }

        openJuicer(player, block.getLocation());
        event.setCancelled(true);
    }

    public void openJuicer(Player player, Location loc) {
        JuicerBlockEntity entity = blockEntities.computeIfAbsent(loc, k -> new JuicerBlockEntity());
        JuicerGui gui = openGuis.computeIfAbsent(loc, k -> new JuicerGui(plugin, loc, entity));

        gui.syncFromBlockEntity();
        player.openInventory(gui.getInventory());
        player.playSound(loc, Sound.BLOCK_BARREL_OPEN, 0.8f, 1.2f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof JuicerGui gui)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        ClickType click = event.getClick();
        InventoryAction action = event.getAction();
        Inventory clickedInv = event.getClickedInventory();
        boolean clickedTop = clickedInv != null && clickedInv.equals(gui.getInventory());

        // 1. 禁止在上方 GUI 執行數字鍵熱鍵交換與副手交換
        if (clickedTop && (click == ClickType.NUMBER_KEY || click == ClickType.SWAP_OFFHAND)) {
            event.setCancelled(true);
            return;
        }

        // 2. 處理點擊上方 GUI
        if (clickedTop) {
            // 背景槽位禁止點擊
            if (!gui.isInteractiveSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }

            // 配方小綠書點擊
            if (rawSlot == JuicerGui.SLOT_RECIPE_BOOK) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    gui.syncToBlockEntity();
                    player.closeInventory();
                    FarmersDelightApi.get().openRecipeBook(
                            player,
                            com.huidu.expandeddelight.recipe.JuicerRecipeType.TYPE_ID,
                            new com.huidu.farmersdelight.api.recipe.RecipeFiller() {
                                @Override
                                public boolean fill(Player p, com.huidu.farmersdelight.api.recipe.ViewableRecipe recipe) {
                                    return false;
                                }

                                @Override
                                public boolean onBack(Player p) {
                                    openJuicer(p, gui.getLocation());
                                    return true;
                                }
                            }
                    );
                }
                return;
            }

            // 飲品展示槽是唯讀槽位 (Drink Display)，點擊無法直接拿取或放入
            if (rawSlot == JuicerGui.SLOT_DRINK_DISPLAY) {
                event.setCancelled(true);
                return;
            }

            // 若點擊了可寫入槽位，標記 dirty 並即時排程同步
            if (gui.isPlayerWritableSlot(rawSlot)) {
                gui.markSlotDirty(rawSlot);
            }
        }

        // 3. 處理玩家背包 (Bottom Inventory) 的 Shift 點擊
        if (!clickedTop && event.isShiftClick()) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && !clickedItem.getType().isAir()) {
                event.setCancelled(true);
                handleShiftClickIntoJuicer(gui, clickedItem);
                gui.syncToBlockEntity();
                return;
            }
        }

        // 4. 針對雙擊收集 (COLLECT_TO_CURSOR)：避免將唯讀/展示槽物品吸走
        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            // 立即執行同步回實體，並標記所有可寫入槽位 dirty
            gui.markSlotDirty(JuicerGui.SLOT_INPUT_1);
            gui.markSlotDirty(JuicerGui.SLOT_INPUT_2);
            gui.markSlotDirty(JuicerGui.SLOT_CONTAINER);
            gui.markSlotDirty(JuicerGui.SLOT_OUTPUT);
        }

        // 任何有效點擊操作後，在該 tick 結束前立即排程同步寫回 BlockEntity
        plugin.getServer().getScheduler().runTask(plugin, gui::syncToBlockEntity);
    }

    private void handleShiftClickIntoJuicer(JuicerGui gui, ItemStack item) {
        Inventory topInv = gui.getInventory();

        // 檢查是否為玻璃瓶容器
        boolean isBottle = item.getType() == Material.GLASS_BOTTLE
                || FarmersDelightItems.matchesId(item, "minecraft:glass_bottle");

        if (isBottle) {
            // 優先嘗試堆疊至容器槽 (SLOT_CONTAINER)
            mergeIntoSlot(topInv, JuicerGui.SLOT_CONTAINER, item);
            if (item.getAmount() <= 0) return;
        }

        // 嘗試堆疊或放入原料槽 1
        mergeIntoSlot(topInv, JuicerGui.SLOT_INPUT_1, item);
        if (item.getAmount() <= 0) return;

        // 嘗試堆疊或放入原料槽 2
        mergeIntoSlot(topInv, JuicerGui.SLOT_INPUT_2, item);
        if (item.getAmount() <= 0) return;

        // 若尚未填滿且也是瓶子，再嘗試放入空容器槽
        if (isBottle) {
            mergeIntoSlot(topInv, JuicerGui.SLOT_CONTAINER, item);
        }
    }

    private void mergeIntoSlot(Inventory inv, int slot, ItemStack incoming) {
        ItemStack current = inv.getItem(slot);
        if (current == null || current.getType().isAir()) {
            inv.setItem(slot, incoming.clone());
            incoming.setAmount(0);
        } else if (current.isSimilar(incoming)) {
            int max = current.getMaxStackSize();
            int canAdd = max - current.getAmount();
            if (canAdd > 0) {
                int toAdd = Math.min(canAdd, incoming.getAmount());
                current.setAmount(current.getAmount() + toAdd);
                incoming.setAmount(incoming.getAmount() - toAdd);
                inv.setItem(slot, current);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof JuicerGui gui)) {
            return;
        }

        for (int slot : event.getRawSlots()) {
            if (slot < 27) {
                if (!gui.isInteractiveSlot(slot) || gui.isReadOnlySlot(slot)) {
                    event.setCancelled(true);
                    return;
                }
                gui.markSlotDirty(slot);
            }
        }

        plugin.getServer().getScheduler().runTask(plugin, gui::syncToBlockEntity);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof JuicerGui gui) {
            gui.syncToBlockEntity();
            if (gui.getInventory().getViewers().size() <= 1) {
                openGuis.remove(gui.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String blockId = com.huidu.farmersdelight.api.block.FarmersDelightBlocks.blockIdOf(block);
        if (!"expandeddelight:juicer".equals(blockId)) {
            return;
        }

        Location loc = block.getLocation();
        JuicerBlockEntity entity = blockEntities.remove(loc);
        JuicerGui gui = openGuis.remove(loc);

        if (entity != null) {
            // 掉落方塊內的所有物品
            for (ItemStack item : entity.getInventory()) {
                if (item != null && !item.getType().isAir() && !SlotPlaceholder.is(JuicerGui.PLACEHOLDER_KEY, item)) {
                    loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), item);
                }
            }
        }

        if (gui != null) {
            gui.getInventory().close();
        }
    }

    private void startJuicerLoop() {
        // 每 2 ticks (0.1 秒) 執行一次榨汁機邏輯
        FarmersDelightApi.get().runRepeating(() -> {
            for (Map.Entry<Location, JuicerBlockEntity> entry : blockEntities.entrySet()) {
                Location loc = entry.getKey();
                JuicerBlockEntity entity = entry.getValue();

                tickJuicer(loc, entity);

                // 若該位置有打開的 GUI，通知其更新視覺
                JuicerGui gui = openGuis.get(loc);
                if (gui != null) {
                    if (gui.getInventory().getViewers().isEmpty()) {
                        openGuis.remove(loc);
                    } else {
                        gui.update();
                    }
                }
            }
        }, 2L, 2L);
    }

    private void tickJuicer(Location loc, JuicerBlockEntity entity) {
        JuicerRecipe recipe = findMatchingRecipe(entity);

        if (recipe != null) {
            entity.setTotalJuiceTime(recipe.time());
            // 如果還沒開始榨或進行中
            if (entity.getJuiceTime() < entity.getTotalJuiceTime()) {
                entity.setJuiceTime(entity.getJuiceTime() + 2);

                // 偶爾播放榨汁氣泡音效
                if (entity.getJuiceTime() % 40 == 0) {
                    loc.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 0.5f, 1.2f);
                }
            }

            // 榨汁完成！
            if (entity.getJuiceTime() >= entity.getTotalJuiceTime()) {
                finishJuice(entity, recipe, loc);
            }
        } else {
            // 若原料不符或缺少，重置進度
            if (entity.getJuiceTime() > 0) {
                entity.setJuiceTime(0);
            }
        }

        // 裝瓶邏輯：依據配方或預設瓶子進行裝瓶
        tryBottleDrink(entity, recipe, loc);
    }

    private JuicerRecipe findMatchingRecipe(JuicerBlockEntity entity) {
        ItemStack in1 = entity.getItem(JuicerBlockEntity.INPUT_1);
        ItemStack in2 = entity.getItem(JuicerBlockEntity.INPUT_2);

        List<ItemStack> inputs = new ArrayList<>();
        if (in1 != null && !in1.getType().isAir()) inputs.add(in1);
        if (in2 != null && !in2.getType().isAir()) inputs.add(in2);

        if (inputs.isEmpty()) return null;

        for (JuicerRecipe recipe : RecipeLoader.getJuicerRecipes()) {
            if (matchesInputs(inputs, recipe.ingredients())) {
                // 檢查 Display 槽是否已有不同飲品佔據 (若有且不同，不可榨)
                ItemStack currentDisplay = entity.getItem(JuicerBlockEntity.DRINK_DISPLAY);
                ItemStack recipeResult = FarmersDelightItems.create(recipe.result());
                if (recipeResult == null) continue;

                if (currentDisplay != null && !currentDisplay.getType().isAir()) {
                    if (!currentDisplay.isSimilar(recipeResult)) {
                        continue;
                    }
                    if (currentDisplay.getAmount() >= currentDisplay.getMaxStackSize()) {
                        continue;
                    }
                }
                return recipe;
            }
        }
        return null;
    }

    private boolean matchesInputs(List<ItemStack> inputs, List<String> required) {
        if (inputs.size() != required.size()) return false;

        boolean[] used = new boolean[inputs.size()];
        for (String req : required) {
            boolean matched = false;
            for (int i = 0; i < inputs.size(); i++) {
                if (!used[i] && FarmersDelightItems.matchesId(inputs.get(i), req)) {
                    used[i] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return true;
    }

    private void finishJuice(JuicerBlockEntity entity, JuicerRecipe recipe, Location loc) {
        // 扣除輸入槽原料各 1 個
        shrinkSlot(entity, JuicerBlockEntity.INPUT_1, 1);
        shrinkSlot(entity, JuicerBlockEntity.INPUT_2, 1);

        // 放入 Drink Display 槽
        ItemStack result = FarmersDelightItems.create(recipe.result());
        if (result == null) {
            result = new ItemStack(Material.POTION);
        }

        ItemStack currentDisplay = entity.getItem(JuicerBlockEntity.DRINK_DISPLAY);
        if (currentDisplay == null || currentDisplay.getType().isAir()) {
            entity.setItem(JuicerBlockEntity.DRINK_DISPLAY, result);
        } else if (currentDisplay.isSimilar(result)) {
            currentDisplay.setAmount(currentDisplay.getAmount() + 1);
        }

        entity.setJuiceTime(0);
        entity.markDirty();
        loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.0f);
    }

    private void tryBottleDrink(JuicerBlockEntity entity, JuicerRecipe recipe, Location loc) {
        ItemStack display = entity.getItem(JuicerBlockEntity.DRINK_DISPLAY);
        ItemStack container = entity.getItem(JuicerBlockEntity.CONTAINER);
        ItemStack output = entity.getItem(JuicerBlockEntity.OUTPUT);

        if (display == null || display.getType().isAir()) return;
        if (container == null || container.getType() == Material.AIR) return;

        // 檢查容器是否符合配方要求的 container (若無對應當前 recipe 則預設為 minecraft:glass_bottle)
        String expectedContainer = (recipe != null && recipe.container() != null && !recipe.container().isEmpty())
                ? recipe.container()
                : "minecraft:glass_bottle";

        if (!FarmersDelightItems.matchesId(container, expectedContainer)) {
            // 兼顧原版常規 GLASS_BOTTLE 比對
            if (!("minecraft:glass_bottle".equals(expectedContainer) && container.getType() == Material.GLASS_BOTTLE)) {
                return;
            }
        }

        // 檢查 Output 槽空間
        if (output != null && !output.getType().isAir()) {
            if (!output.isSimilar(display)) return;
            if (output.getAmount() >= output.getMaxStackSize()) return;
        }

        // 成功裝瓶！
        shrinkSlot(entity, JuicerBlockEntity.DRINK_DISPLAY, 1);
        shrinkSlot(entity, JuicerBlockEntity.CONTAINER, 1);

        ItemStack bottled = display.clone();
        bottled.setAmount(1);

        if (output == null || output.getType().isAir()) {
            entity.setItem(JuicerBlockEntity.OUTPUT, bottled);
        } else {
            output.setAmount(output.getAmount() + 1);
        }

        entity.markDirty();
        loc.getWorld().playSound(loc, Sound.ITEM_BOTTLE_FILL, 0.8f, 1.2f);

        // 發放經驗值 (若配方有設定 experience 且 > 0)
        float exp = (recipe != null) ? recipe.experience() : 0.5f;
        if (exp > 0 && loc.getWorld() != null) {
            int intExp = (int) exp;
            float remainder = exp - intExp;
            if (Math.random() < remainder) {
                intExp += 1;
            }
            if (intExp > 0) {
                ExperienceOrb orb = loc.getWorld().spawn(loc.clone().add(0.5, 0.6, 0.5), ExperienceOrb.class);
                orb.setExperience(intExp);
            }
        }
    }

    private void shrinkSlot(JuicerBlockEntity entity, int slot, int count) {
        ItemStack stack = entity.getItem(slot);
        if (stack != null && !stack.getType().isAir()) {
            if (stack.getAmount() <= count) {
                entity.setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - count);
            }
        }
    }
}
