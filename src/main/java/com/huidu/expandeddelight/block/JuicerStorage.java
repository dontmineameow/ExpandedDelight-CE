package com.huidu.expandeddelight.block;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 負責榨汁機方塊狀態的檔案持久化 (juicers.yml)
 */
public class JuicerStorage {

    private final ExpandedDelightPlugin plugin;
    private final File dataFile;

    public JuicerStorage(ExpandedDelightPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "juicers.yml");
    }

    public void load(Map<Location, JuicerBlockEntity> blockEntities) {
        blockEntities.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = config.getConfigurationSection("juicers");
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
                plugin.getLogger().warning("找不到世界 " + worldName + "，跳過榨汁機資料載入: " + key);
                continue;
            }

            int x = sec.getInt("x");
            int y = sec.getInt("y");
            int z = sec.getInt("z");
            Location loc = new Location(world, x, y, z);

            JuicerBlockEntity entity = new JuicerBlockEntity();
            entity.setJuiceTime(sec.getInt("juice_time", 0));
            entity.setTotalJuiceTime(sec.getInt("total_juice_time", 200));

            ConfigurationSection invSec = sec.getConfigurationSection("items");
            if (invSec != null) {
                for (int slot = 0; slot < JuicerBlockEntity.TOTAL_SLOTS; slot++) {
                    if (invSec.contains(String.valueOf(slot))) {
                        ItemStack item = invSec.getItemStack(String.valueOf(slot));
                        entity.setItem(slot, item);
                    }
                }
            }

            blockEntities.put(loc, entity);
            loadedCount++;
        }

        plugin.getLogger().info("已從 juicers.yml 成功載入 " + loadedCount + " 個榨汁機方塊狀態。");
    }

    public void save(Map<Location, JuicerBlockEntity> blockEntities) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("juicers");

        int index = 0;
        for (Map.Entry<Location, JuicerBlockEntity> entry : blockEntities.entrySet()) {
            Location loc = entry.getKey();
            JuicerBlockEntity entity = entry.getValue();
            if (loc.getWorld() == null) continue;

            // 如果榨汁機為空且進度為 0，可略過節省檔案空間
            if (isEmpty(entity) && entity.getJuiceTime() == 0) {
                continue;
            }

            ConfigurationSection sec = root.createSection("juicer_" + (index++));
            sec.set("world", loc.getWorld().getName());
            sec.set("x", loc.getBlockX());
            sec.set("y", loc.getBlockY());
            sec.set("z", loc.getBlockZ());
            sec.set("juice_time", entity.getJuiceTime());
            sec.set("total_juice_time", entity.getTotalJuiceTime());

            ConfigurationSection invSec = sec.createSection("items");
            for (int slot = 0; slot < JuicerBlockEntity.TOTAL_SLOTS; slot++) {
                ItemStack item = entity.getItem(slot);
                if (item != null && !item.getType().isAir()) {
                    invSec.set(String.valueOf(slot), item);
                }
            }
        }

        try {
            config.save(dataFile);
            plugin.getLogger().info("已將 " + index + " 個榨汁機方塊狀態儲存至 juicers.yml。");
        } catch (IOException e) {
            plugin.getLogger().severe("儲存 juicers.yml 時發生例外: " + e.getMessage());
        }
    }

    private boolean isEmpty(JuicerBlockEntity entity) {
        for (ItemStack item : entity.getInventory()) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }
}
