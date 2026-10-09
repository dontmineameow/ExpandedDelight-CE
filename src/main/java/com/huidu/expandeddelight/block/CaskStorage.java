package com.huidu.expandeddelight.block;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 負責發酵桶（Cask）狀態與進度的持久化儲存 (casks.yml)
 */
public class CaskStorage {

    private final ExpandedDelightPlugin plugin;
    private final File dataFile;

    public CaskStorage(ExpandedDelightPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "casks.yml");
    }

    public void load(Map<Location, Integer> fermentingStages) {
        fermentingStages.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = config.getConfigurationSection("casks");
        if (root == null) {
            return;
        }

        int loadedCount = 0;
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;

            String worldName = sec.getString("world");
            if (worldName == null) continue;
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("找不到世界 " + worldName + "，跳過發酵桶資料載入: " + key);
                continue;
            }

            int x = sec.getInt("x");
            int y = sec.getInt("y");
            int z = sec.getInt("z");
            int stage = sec.getInt("stage", 0);

            Location loc = new Location(world, x, y, z);
            fermentingStages.put(loc, stage);
            loadedCount++;
        }

        plugin.getLogger().info("已從 casks.yml 成功載入 " + loadedCount + " 個發酵桶狀態。");
    }

    public void save(Map<Location, Integer> fermentingStages) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("casks");

        int index = 0;
        for (Map.Entry<Location, Integer> entry : fermentingStages.entrySet()) {
            Location loc = entry.getKey();
            int stage = entry.getValue();
            if (loc.getWorld() == null) continue;

            ConfigurationSection sec = root.createSection("cask_" + (index++));
            sec.set("world", loc.getWorld().getName());
            sec.set("x", loc.getBlockX());
            sec.set("y", loc.getBlockY());
            sec.set("z", loc.getBlockZ());
            sec.set("stage", stage);
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("儲存 casks.yml 時發生錯誤: " + e.getMessage());
        }
    }
}
