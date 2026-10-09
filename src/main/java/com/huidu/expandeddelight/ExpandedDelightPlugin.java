package com.huidu.expandeddelight;

import com.huidu.expandeddelight.advancement.ExpandedDelightAdvancements;
import com.huidu.expandeddelight.block.CaskManager;
import com.huidu.expandeddelight.block.JuicerManager;
import com.huidu.expandeddelight.listener.CinnamonStripListener;
import com.huidu.expandeddelight.recipe.RecipeLoader;
import com.huidu.farmersdelight.api.FarmersDelightApi;
import org.bukkit.plugin.java.JavaPlugin;

public class ExpandedDelightPlugin extends JavaPlugin {

    private static ExpandedDelightPlugin instance;
    private JuicerManager juicerManager;
    private CaskManager caskManager;
    private ExpandedDelightAdvancements advancements;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("ExpandedDelight (Addon) 正在載入...");

        saveDefaultConfig();

        // 檢查主插件 API 特性支援
        if (!FarmersDelightApi.get().hasFeature("recipes")) {
            getLogger().warning("FarmersDelight API 特性支援不完整，請確保主插件已更新！");
        }

        FarmersDelightApi.get().registerAddonBlockNamespace("expandeddelight");

        // 註冊 Expanded Delight 進度成就系統
        this.advancements = new ExpandedDelightAdvancements(this);
        getServer().getPluginManager().registerEvents(advancements, this);

        // 註冊暖機與重載事件：當 CraftEngine 物品就緒/重載時，安全注入真實物品配方與重構進度圖示
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onWarmup(com.huidu.farmersdelight.api.event.FarmersDelightWarmupEvent event) {
                RecipeLoader.loadAll(ExpandedDelightPlugin.this);
                if (advancements != null) {
                    advancements.registerAdvancementTree();
                }
            }

            @org.bukkit.event.EventHandler
            public void onReload(com.huidu.farmersdelight.api.event.FarmersDelightReloadEvent event) {
                reloadConfig();
                RecipeLoader.loadAll(ExpandedDelightPlugin.this);
                if (advancements != null) {
                    advancements.registerAdvancementTree();
                }
            }
        }, this);

        // 如果在插件載入時 CE 物品已經就緒（例如伺服器開機完成後單獨 reload/啟用附屬），才立即執行一次
        if (com.huidu.farmersdelight.api.content.FarmersDelightContent.isCraftEngineReady()) {
            RecipeLoader.loadAll(this);
            advancements.registerAdvancementTree();
        }

        // 註冊榨汁機管理系統 (支援資料持久化與自動防刷物同步)
        this.juicerManager = new JuicerManager(this);
        getServer().getPluginManager().registerEvents(juicerManager, this);

        // 每 5 分鐘自動存檔一次方塊狀態 (6000 ticks)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (juicerManager != null) {
                juicerManager.saveData();
            }
            if (caskManager != null) {
                caskManager.saveData();
            }
        }, 6000L, 6000L);

        // 註冊起司桶熟成系統
        this.caskManager = new CaskManager(this);
        getServer().getPluginManager().registerEvents(caskManager, this);

        // 註冊肉桂剝皮事件
        getServer().getPluginManager().registerEvents(new CinnamonStripListener(this), this);

        // 註冊肉桂樹生長與樹葉管理器
        com.huidu.expandeddelight.tree.CinnamonTreeManager cinnamonTreeManager = new com.huidu.expandeddelight.tree.CinnamonTreeManager(this);
        getServer().getPluginManager().registerEvents(cinnamonTreeManager, this);
        // 註冊肉桂樹叢林自然生成
        getServer().getPluginManager().registerEvents(new com.huidu.expandeddelight.tree.CinnamonTreePopulator(cinnamonTreeManager), this);

        // 註冊鹽礦石掉落與挖掘處理
        getServer().getPluginManager().registerEvents(new com.huidu.expandeddelight.listener.SaltOreListener(this), this);

        getLogger().info("ExpandedDelight 載入完成！");

    }

    @Override
    public void onDisable() {
        if (juicerManager != null) {
            juicerManager.saveData();
        }
        if (caskManager != null) {
            caskManager.saveData();
        }
        getLogger().info("ExpandedDelight 已卸載。");
    }

    public static ExpandedDelightPlugin getInstance() {
        return instance;
    }
}
