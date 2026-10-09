package com.huidu.expandeddelight.recipe;

import com.huidu.expandeddelight.ExpandedDelightPlugin;
import com.huidu.farmersdelight.api.FarmersDelightApi;
import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.recipe.ChanceResult;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import org.bukkit.NamespacedKey;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RecipeLoader {

    private static final List<JuicerRecipe> JUICER_RECIPES = new ArrayList<>();

    public static List<JuicerRecipe> getJuicerRecipes() {
        return Collections.unmodifiableList(JUICER_RECIPES);
    }

    public static void loadAll(ExpandedDelightPlugin plugin) {
        loadCommonTags(plugin);
        loadCookingPotRecipes(plugin);
        loadCuttingBoardRecipes(plugin);
        loadJuicerRecipes(plugin);
    }

    private static void loadJuicerRecipes(ExpandedDelightPlugin plugin) {
        JUICER_RECIPES.clear();
        try (InputStream in = plugin.getResource("recipes/juicer_recipes.yml")) {
            if (in == null) return;
            YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            ConfigurationSection sec = config.getConfigurationSection("juicer_recipes");
            if (sec == null) return;

            for (String key : sec.getKeys(false)) {
                ConfigurationSection rSec = sec.getConfigurationSection(key);
                if (rSec == null) continue;

                List<String> ingredients = rSec.getStringList("ingredients");
                String container = rSec.getString("container", "minecraft:glass_bottle");
                String result = rSec.getString("result");
                int time = rSec.getInt("time", 200);
                float exp = (float) rSec.getDouble("experience", 1.0);

                JUICER_RECIPES.add(new JuicerRecipe(
                        new NamespacedKey("expandeddelight", key),
                        ingredients,
                        container,
                        result,
                        time,
                        exp
                ));
            }
            plugin.getLogger().info("已載入 " + JUICER_RECIPES.size() + " 個榨汁機配方。");
            FarmersDelightApi.get().registerRecipeType(new JuicerRecipeType(JUICER_RECIPES));
            plugin.getLogger().info("已將榨汁機配方類型註冊進 FarmersDelight 配方書系統。");
        } catch (Exception e) {
            plugin.getLogger().warning("加載 juicer_recipes.yml 時出錯: " + e.getMessage());
        }
    }

    private static void loadCommonTags(ExpandedDelightPlugin plugin) {
        try (InputStream in = plugin.getResource("common-tags.yml")) {
            if (in == null) return;
            YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            ConfigurationSection tagsSec = config.getConfigurationSection("tags");
            if (tagsSec != null) {
                Map<String, List<String>> tagMap = new HashMap<>();
                for (String tag : tagsSec.getKeys(false)) {
                    tagMap.put(tag, tagsSec.getStringList(tag));
                }
                FarmersDelightApi.get().registerCommonTags("expandeddelight", tagMap);
                plugin.getLogger().info("已向 FarmersDelight 註冊 " + tagMap.size() + " 個共用標籤。");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加載 common-tags.yml 時出錯: " + e.getMessage());
        }
    }

    private static void loadCookingPotRecipes(ExpandedDelightPlugin plugin) {
        try (InputStream in = plugin.getResource("recipes/cooking_pot_recipes.yml")) {
            if (in == null) return;
            YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            ConfigurationSection sec = config.getConfigurationSection("cooking_pot_recipes");
            if (sec == null) return;

            int count = 0;
            for (String key : sec.getKeys(false)) {
                ConfigurationSection rSec = sec.getConfigurationSection(key);
                if (rSec == null) continue;

                List<String> ingredients = rSec.getStringList("ingredients");
                String container = rSec.getString("container", "minecraft:bowl");
                String result = rSec.getString("result");
                int cookTime = rSec.getInt("cook-time", 200);
                float exp = (float) rSec.getDouble("experience", 0.0);

                ItemStack containerItem = container.equalsIgnoreCase("none") ? null : FarmersDelightItems.createOrFallback(container, Material.BOWL);
                ItemStack resultItem = FarmersDelightItems.createOrFallback(result, Material.RABBIT_STEW);

                FarmersDelightApi.get().registerCookingPotRecipe(
                        "expandeddelight:" + key,
                        ingredients,
                        containerItem,
                        resultItem,
                        exp,
                        cookTime,
                        "meals"
                );
                count++;
            }
            plugin.getLogger().info("已註冊 " + count + " 個烹飪鍋配方。");
        } catch (Exception e) {
            plugin.getLogger().warning("加載 cooking_pot_recipes.yml 時出錯: " + e.getMessage());
        }
    }

    private static void loadCuttingBoardRecipes(ExpandedDelightPlugin plugin) {
        try (InputStream in = plugin.getResource("recipes/cutting_board_recipes.yml")) {
            if (in == null) return;
            YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            ConfigurationSection sec = config.getConfigurationSection("cutting_board_recipes");
            if (sec == null) return;

            int count = 0;
            for (String key : sec.getKeys(false)) {
                ConfigurationSection rSec = sec.getConfigurationSection(key);
                if (rSec == null) continue;

                String ingredient = rSec.getString("ingredient");
                String tool = rSec.getString("tool", "tool:knife");
                String sound = rSec.getString("sound", "farmersdelight:block.cutting_board.knife");

                List<Map<?, ?>> resultsList = rSec.getMapList("results");
                List<ChanceResult> results = new ArrayList<>();
                for (Map<?, ?> m : resultsList) {
                    float chance = m.containsKey("chance") ? ((Number) m.get("chance")).floatValue() : 1.0f;
                    int amt = m.containsKey("count") ? ((Number) m.get("count")).intValue() : 1;
                    String itemId = m.containsKey("item") ? String.valueOf(m.get("item")) : "minecraft:bread";
                    ItemStack itemStack = FarmersDelightItems.createOrFallback(itemId, Material.BREAD);
                    itemStack.setAmount(amt);
                    results.add(new ChanceResult(itemStack, chance));
                }

                FarmersDelightApi.get().registerCuttingBoardRecipeWithChances(
                        "expandeddelight:" + key,
                        ingredient,
                        tool,
                        results,
                        sound
                );
                count++;
            }
            plugin.getLogger().info("已註冊 " + count + " 個砧板配方。");
        } catch (Exception e) {
            plugin.getLogger().warning("加載 cutting_board_recipes.yml 時出錯: " + e.getMessage());
        }
    }
}
