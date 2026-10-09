package com.huidu.expandeddelight.gui;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import com.huidu.expandeddelight.block.JuicerBlockEntity;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.item.SlotPlaceholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.momirealms.craftengine.bukkit.api.CraftEngineImages;
import net.momirealms.craftengine.core.font.Image;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 忠實還原 ExpandedDelight 原版 Juicer GUI
 */
public class JuicerGui implements InventoryHolder {

    public static final String PLACEHOLDER_KEY = "expandeddelight_juicer_placeholder";

    // 3 行箱子 (27 格) 的槽位對照原版 juicer.png
    public static final int SLOT_INPUT_1 = 2;       // 第 1 列原料 (row 0, col 2)
    public static final int SLOT_INPUT_2 = 11;      // 第 2 列原料 (row 1, col 2)
    public static final int SLOT_PROGRESS = 4;      // 動態進度箭頭 (row 0, col 4，居中對準兩側槽位)
    public static final int SLOT_DRINK_DISPLAY = 6;  // 飲品展示槽 (row 0, col 6，對齊上方大方框)
    public static final int SLOT_RECIPE_BOOK = 9;   // 配方小綠書 (row 1, col 0)
    public static final int SLOT_CONTAINER = 22;    // 容器瓶子槽 (row 2, col 4)
    public static final int SLOT_OUTPUT = 24;       // 最終產出槽 (row 2, col 6)

    private static final Pattern SHIFT_TAG_PATTERN = Pattern.compile("<shift:([+-]?\\d+)>");
    private static final Pattern IMAGE_TAG_PATTERN = Pattern.compile("<image:([a-z0-9_./-]+:[a-z0-9_./-]+)>");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final ExpandedDelightPlugin plugin;
    private final Location location;
    private final JuicerBlockEntity blockEntity;
    private final Inventory inventory;

    // 標記在 GUI 中被點擊/修改但尚未寫回 BlockEntity 的槽位
    private final Set<Integer> dirtySlots = ConcurrentHashMap.newKeySet();
    private int cachedProgress = -1;

    public JuicerGui(ExpandedDelightPlugin plugin, Location location, JuicerBlockEntity blockEntity) {
        this.plugin = plugin;
        this.location = location;
        this.blockEntity = blockEntity;

        int shiftX = plugin.getConfig().getInt("juicer-gui.shift-x", -6);
        String rawTitle = plugin.getConfig().getString("juicer-gui.title", "<white><shift:{shift_x}><image:expandeddelight:juicer_gui>")
                .replace("{shift_x}", String.valueOf(shiftX));
        Component title = resolveTitle(rawTitle);
        this.inventory = Bukkit.createInventory(this, 27, title);

        initializeGui();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Location getLocation() {
        return location;
    }

    public JuicerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    private void initializeGui() {
        ItemStack filler = createPlaceholder();

        // 填充所有非操作槽位，保留原版貼圖視覺
        for (int i = 0; i < 27; i++) {
            if (isInteractiveSlot(i)) {
                inventory.setItem(i, null);
            } else {
                inventory.setItem(i, filler);
            }
        }

        // 同步方塊實體中的物品
        syncFromBlockEntity();
        updateProgressIcon();
        inventory.setItem(SLOT_RECIPE_BOOK, createRecipeBookIcon());
    }

    public boolean isInteractiveSlot(int slot) {
        return slot == SLOT_INPUT_1
                || slot == SLOT_INPUT_2
                || slot == SLOT_RECIPE_BOOK
                || slot == SLOT_DRINK_DISPLAY
                || slot == SLOT_CONTAINER
                || slot == SLOT_OUTPUT;
    }

    public boolean isReadOnlySlot(int slot) {
        return slot == SLOT_PROGRESS || slot == SLOT_DRINK_DISPLAY || slot == SLOT_RECIPE_BOOK;
    }

    public boolean isPlayerWritableSlot(int slot) {
        return slot == SLOT_INPUT_1 || slot == SLOT_INPUT_2 || slot == SLOT_CONTAINER || slot == SLOT_OUTPUT;
    }

    public void markSlotDirty(int slot) {
        if (isPlayerWritableSlot(slot)) {
            dirtySlots.add(slot);
        }
    }

    public void update() {
        // 更新進度條顯示
        int progress = blockEntity.getProgressPercent();
        if (progress != cachedProgress) {
            cachedProgress = progress;
            updateProgressIcon();
        }

        // 唯讀/伺服器端控制槽位：飲品展示槽永遠與方塊實體同步
        ItemStack displayStack = blockEntity.getItem(JuicerBlockEntity.DRINK_DISPLAY);
        ItemStack currentGuiDisplay = inventory.getItem(SLOT_DRINK_DISPLAY);
        if (!isSame(displayStack, currentGuiDisplay)) {
            inventory.setItem(SLOT_DRINK_DISPLAY, cloneOrNull(displayStack));
        }

        // 若可寫入槽位目前處於 dirty 狀態 (玩家剛放入/更動)，由玩家操作優先，不在此覆蓋
        if (!dirtySlots.contains(SLOT_OUTPUT)) {
            ItemStack outputStack = blockEntity.getItem(JuicerBlockEntity.OUTPUT);
            if (!isSame(outputStack, inventory.getItem(SLOT_OUTPUT))) {
                inventory.setItem(SLOT_OUTPUT, cloneOrNull(outputStack));
            }
        }

        if (!dirtySlots.contains(SLOT_INPUT_1)) {
            ItemStack in1 = blockEntity.getItem(JuicerBlockEntity.INPUT_1);
            if (!isSame(in1, inventory.getItem(SLOT_INPUT_1))) {
                inventory.setItem(SLOT_INPUT_1, cloneOrNull(in1));
            }
        }

        if (!dirtySlots.contains(SLOT_INPUT_2)) {
            ItemStack in2 = blockEntity.getItem(JuicerBlockEntity.INPUT_2);
            if (!isSame(in2, inventory.getItem(SLOT_INPUT_2))) {
                inventory.setItem(SLOT_INPUT_2, cloneOrNull(in2));
            }
        }

        if (!dirtySlots.contains(SLOT_CONTAINER)) {
            ItemStack cont = blockEntity.getItem(JuicerBlockEntity.CONTAINER);
            if (!isSame(cont, inventory.getItem(SLOT_CONTAINER))) {
                inventory.setItem(SLOT_CONTAINER, cloneOrNull(cont));
            }
        }
    }

    /**
     * 當玩家在 GUI 內放置/拿取物品後，將 GUI 中的狀態寫回 BlockEntity
     */
    public void syncToBlockEntity() {
        blockEntity.setItem(JuicerBlockEntity.INPUT_1, cloneOrNull(inventory.getItem(SLOT_INPUT_1)));
        blockEntity.setItem(JuicerBlockEntity.INPUT_2, cloneOrNull(inventory.getItem(SLOT_INPUT_2)));
        blockEntity.setItem(JuicerBlockEntity.CONTAINER, cloneOrNull(inventory.getItem(SLOT_CONTAINER)));
        blockEntity.setItem(JuicerBlockEntity.OUTPUT, cloneOrNull(inventory.getItem(SLOT_OUTPUT)));
        dirtySlots.clear();
        blockEntity.markDirty();
    }

    public void syncFromBlockEntity() {
        inventory.setItem(SLOT_INPUT_1, cloneOrNull(blockEntity.getItem(JuicerBlockEntity.INPUT_1)));
        inventory.setItem(SLOT_INPUT_2, cloneOrNull(blockEntity.getItem(JuicerBlockEntity.INPUT_2)));
        inventory.setItem(SLOT_CONTAINER, cloneOrNull(blockEntity.getItem(JuicerBlockEntity.CONTAINER)));
        inventory.setItem(SLOT_DRINK_DISPLAY, cloneOrNull(blockEntity.getItem(JuicerBlockEntity.DRINK_DISPLAY)));
        inventory.setItem(SLOT_OUTPUT, cloneOrNull(blockEntity.getItem(JuicerBlockEntity.OUTPUT)));
        dirtySlots.clear();
    }

    private void updateProgressIcon() {
        if (SLOT_PROGRESS < 0 || SLOT_PROGRESS >= inventory.getSize()) return;
        int progress = blockEntity.getProgressPercent();
        // 映射到 0~20 幀的進度箭頭圖示
        int index = Math.min(20, Math.max(0, (int) Math.round(progress / 5.0)));
        ItemStack progressItem = FarmersDelightItems.create("expandeddelight:juicer_progress_" + index);
        if (progressItem == null || progressItem.getType().isAir()) {
            progressItem = FarmersDelightItems.create("farmersdelight:" + index);
        }
        if (progressItem == null || progressItem.getType().isAir()) {
            progressItem = new ItemStack(Material.ARROW);
        }
        ItemMeta meta = progressItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§e榨汁進度: " + progress + "%"));
            progressItem.setItemMeta(meta);
        }
        inventory.setItem(SLOT_PROGRESS, progressItem);
    }

    private ItemStack createRecipeBookIcon() {
        ItemStack book = FarmersDelightItems.createOrFallback("farmersdelight:recipe_book", Material.KNOWLEDGE_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§6查看榨汁機配方"));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7點擊開啟配方書瀏覽所有果汁製作方式"));
            meta.lore(lore);
            book.setItemMeta(meta);
        }
        return SlotPlaceholder.mark(PLACEHOLDER_KEY, book);
    }

    private ItemStack createPlaceholder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.setItemModel(org.bukkit.NamespacedKey.minecraft("air"));
            meta.setHideTooltip(true);
            item.setItemMeta(meta);
        }
        return SlotPlaceholder.mark(PLACEHOLDER_KEY, item);
    }

    private boolean isSame(ItemStack a, ItemStack b) {
        if ((a == null || a.getType().isAir()) && (b == null || b.getType().isAir())) return true;
        if (a == null || b == null) return false;
        return a.isSimilar(b) && a.getAmount() == b.getAmount();
    }

    private ItemStack cloneOrNull(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        return item.clone();
    }

    private Component resolveTitle(String text) {
        Matcher shiftMatcher = SHIFT_TAG_PATTERN.matcher(text);
        StringBuilder shiftBuffer = new StringBuilder();
        while (shiftMatcher.find()) {
            int offset = Integer.parseInt(shiftMatcher.group(1));
            String replacement = "";
            try {
                replacement = net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine.instance().fontManager().createMiniMessageOffsets(offset);
            } catch (Throwable ignored) {}
            shiftMatcher.appendReplacement(shiftBuffer, Matcher.quoteReplacement(replacement));
        }
        shiftMatcher.appendTail(shiftBuffer);

        String afterShift = shiftBuffer.toString();
        Matcher imgMatcher = IMAGE_TAG_PATTERN.matcher(afterShift);
        StringBuilder imgBuffer = new StringBuilder();
        while (imgMatcher.find()) {
            Image image = CraftEngineImages.byId(Key.of(imgMatcher.group(1)));
            String replacement = imgMatcher.group(0);
            if (image != null) {
                replacement = image.miniMessageAt(0, 0);
            }
            imgMatcher.appendReplacement(imgBuffer, Matcher.quoteReplacement(replacement));
        }
        imgMatcher.appendTail(imgBuffer);

        return MINI_MESSAGE.deserialize(imgBuffer.toString());
    }
}
