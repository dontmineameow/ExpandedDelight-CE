package com.huidu.expandeddelight.advancement;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import com.huidu.farmersdelight.api.advancement.FarmersDelightAdvancements;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 管理 Expanded Delight 附屬進度 (Advancements)
 * 使用 FarmersDelightAdvancements API 自動構建並注入獨立的 Expanded Delight 進度頁
 */
public class ExpandedDelightAdvancements implements Listener {

    public static final String TAB_ID = "expandeddelight";

    // 進度 ID
    public static final String ADV_ROOT = "root";
    public static final String ADV_JUICE = "juice";
    public static final String ADV_ALL_JUICES = "all_juices";
    public static final String ADV_CASK = "cask";
    public static final String ADV_GOAT_MILK = "goat_milk";
    public static final String ADV_CHEESE = "cheese";
    public static final String ADV_STRIP_CINNAMON = "strip_cinnamon";
    public static final String ADV_SALT = "salt";
    public static final String ADV_SPICY = "spicy";

    private final ExpandedDelightPlugin plugin;

    public ExpandedDelightAdvancements(ExpandedDelightPlugin plugin) {
        this.plugin = plugin;
        registerAdvancementTree();
    }

    public void registerAdvancementTree() {
        if (!FarmersDelightAdvancements.isAvailable()) {
            plugin.getLogger().warning("UltimateAdvancementAPI 未就緒，暫時略過 Expanded Delight 進度註冊。");
            return;
        }

        ItemStack rootIcon = FarmersDelightItems.createOrFallback("expandeddelight:juicer", Material.CAULDRON);
        ItemStack juiceIcon = FarmersDelightItems.createOrFallback("expandeddelight:apple_juice", Material.POTION);
        ItemStack allJuicesIcon = FarmersDelightItems.createOrFallback("expandeddelight:sweet_berry_juice", Material.POTION);
        ItemStack caskIcon = FarmersDelightItems.createOrFallback("expandeddelight:milk_cask", Material.BARREL);
        ItemStack goatMilkIcon = FarmersDelightItems.createOrFallback("expandeddelight:goat_milk_bucket", Material.MILK_BUCKET);
        ItemStack cheeseIcon = FarmersDelightItems.createOrFallback("expandeddelight:cheese_slice", Material.BREAD);
        ItemStack cinnamonIcon = FarmersDelightItems.createOrFallback("expandeddelight:cinnamon_stick", Material.STICK);
        ItemStack saltIcon = FarmersDelightItems.createOrFallback("expandeddelight:salt_rock", Material.QUARTZ);
        ItemStack spicyIcon = FarmersDelightItems.createOrFallback("expandeddelight:chili_pepper", Material.APPLE);

        boolean success = FarmersDelightAdvancements.tree(TAB_ID)
                // 根進度：更多樂事，但樂在哪？ (放置於 x=0, y=3，給上方與下方保留空間)
                .root(
                        ADV_ROOT,
                        rootIcon,
                        "expandeddelight.advancement.root",
                        "expandeddelight.advancement.root.desc",
                        "textures/block/mud_bricks.png"
                )
                // 榨汁分支 (上方: y = 1)
                .advancement(
                        ADV_JUICE,
                        ADV_ROOT,
                        juiceIcon,
                        "expandeddelight.advancement.juice",
                        "expandeddelight.advancement.juice.desc",
                        "task",
                        2.0f,
                        1.0f
                )
                .multiTask(
                        ADV_ALL_JUICES,
                        ADV_JUICE,
                        allJuicesIcon,
                        "expandeddelight.advancement.all_juices",
                        "expandeddelight.advancement.all_juices.desc",
                        "challenge",
                        4.0f,
                        1.0f,
                        List.of("apple", "sweet_berry", "glow_berry", "cranberry", "melon")
                )
                // 起司與發酵分支 (中間: y = 3)
                .advancement(
                        ADV_CASK,
                        ADV_ROOT,
                        caskIcon,
                        "expandeddelight.advancement.cask",
                        "expandeddelight.advancement.cask.desc",
                        "task",
                        2.0f,
                        3.0f
                )
                .advancement(
                        ADV_GOAT_MILK,
                        ADV_CASK,
                        goatMilkIcon,
                        "expandeddelight.advancement.goat_milk",
                        "expandeddelight.advancement.goat_milk.desc",
                        "task",
                        4.0f,
                        2.0f
                )
                .advancement(
                        ADV_CHEESE,
                        ADV_CASK,
                        cheeseIcon,
                        "expandeddelight.advancement.cheese",
                        "expandeddelight.advancement.cheese.desc",
                        "goal",
                        4.0f,
                        4.0f
                )
                // 農業與採集分支 (下方: y = 5, 6, 7)
                .advancement(
                        ADV_STRIP_CINNAMON,
                        ADV_ROOT,
                        cinnamonIcon,
                        "expandeddelight.advancement.strip_cinnamon",
                        "expandeddelight.advancement.strip_cinnamon.desc",
                        "task",
                        2.0f,
                        5.0f
                )
                .advancement(
                        ADV_SALT,
                        ADV_ROOT,
                        saltIcon,
                        "expandeddelight.advancement.salt",
                        "expandeddelight.advancement.salt.desc",
                        "task",
                        2.0f,
                        7.0f
                )
                .advancement(
                        ADV_SPICY,
                        ADV_ROOT,
                        spicyIcon,
                        "expandeddelight.advancement.spicy",
                        "expandeddelight.advancement.spicy.desc",
                        "goal",
                        4.0f,
                        6.0f
                )
                .register();

        if (success) {
            plugin.getLogger().info("已成功註冊 Expanded Delight 專屬進度頁！");
        }
    }

    public static void award(Player player, String advancementId) {
        if (player == null || advancementId == null) return;
        FarmersDelightAdvancements.award(TAB_ID, player, advancementId);
    }

    public static void awardCriteria(Player player, String advancementId, String criterion) {
        if (player == null || advancementId == null || criterion == null) return;
        FarmersDelightAdvancements.awardCriteria(TAB_ID, player, advancementId, criterion);
    }

    /**
     * 撿起榨汁機自動解鎖根進度
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (FarmersDelightItems.matchesId(item, "expandeddelight:juicer")) {
            award(player, ADV_ROOT);
        } else if (FarmersDelightItems.matchesId(item, "expandeddelight:salt_rock") || FarmersDelightItems.matchesId(item, "expandeddelight:salt")) {
            award(player, ADV_SALT);
        }
    }

    /**
     * 玩家飲用果汁或食用辣味料理時解鎖進度
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 果汁檢測
        if (FarmersDelightItems.matchesId(item, "expandeddelight:apple_juice")) {
            award(player, ADV_JUICE);
            awardCriteria(player, ADV_ALL_JUICES, "apple");
        } else if (FarmersDelightItems.matchesId(item, "expandeddelight:sweet_berry_juice")) {
            award(player, ADV_JUICE);
            awardCriteria(player, ADV_ALL_JUICES, "sweet_berry");
        } else if (FarmersDelightItems.matchesId(item, "expandeddelight:glow_berry_juice")) {
            award(player, ADV_JUICE);
            awardCriteria(player, ADV_ALL_JUICES, "glow_berry");
        } else if (FarmersDelightItems.matchesId(item, "expandeddelight:cranberry_juice")) {
            award(player, ADV_JUICE);
            awardCriteria(player, ADV_ALL_JUICES, "cranberry");
        } else if (FarmersDelightItems.matchesId(item, "farmersdelight:melon_juice")) {
            award(player, ADV_JUICE);
            awardCriteria(player, ADV_ALL_JUICES, "melon");
        }

        // 辣味料理檢測
        if (FarmersDelightItems.matchesId(item, "expandeddelight:chili_pepper")
                || FarmersDelightItems.matchesId(item, "expandeddelight:chili_pepper_salmon")
                || FarmersDelightItems.matchesId(item, "expandeddelight:peperonata")) {
            award(player, ADV_SPICY);
        }
    }
}
