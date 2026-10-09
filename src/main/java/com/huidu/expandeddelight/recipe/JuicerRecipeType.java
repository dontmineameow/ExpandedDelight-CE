package com.huidu.expandeddelight.recipe;

import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.recipe.RecipeBookLayout;
import com.huidu.farmersdelight.api.recipe.RecipeType;
import com.huidu.farmersdelight.api.recipe.SimpleRecipeBookLayout;
import com.huidu.farmersdelight.api.recipe.ViewableRecipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 榨汁機配方類型定義，整合進 FarmersDelight 內建配方瀏覽系統
 */
public class JuicerRecipeType implements RecipeType {

    public static final String TYPE_ID = "expandeddelight:juicing";

    private final List<ViewableRecipe> viewableRecipes = new ArrayList<>();

    public JuicerRecipeType(List<JuicerRecipe> recipes) {
        for (JuicerRecipe r : recipes) {
            viewableRecipes.add(new JuicerRecipeViewable(r));
        }
    }

    @Override
    public String id() {
        return TYPE_ID;
    }

    @Override
    public Component title() {
        return Component.text("榨汁機配方").color(NamedTextColor.GOLD);
    }

    @Override
    public ItemStack icon() {
        ItemStack item = FarmersDelightItems.createOrFallback("expandeddelight:juicer", Material.BREWING_STAND);
        return item;
    }

    @Override
    public List<ViewableRecipe> recipes() {
        return viewableRecipes;
    }

    @Override
    public RecipeBookLayout listLayout() {
        Map<Character, String> legend = new HashMap<>();
        legend.put('R', "recipe");
        legend.put('P', "prev_page");
        legend.put('N', "next_page");
        legend.put('B', "back");
        legend.put(' ', "empty");

        Map<String, ItemStack> decorations = new HashMap<>();
        decorations.put("prev_page", SimpleRecipeBookLayout.namedItem(Material.ARROW, "上一頁", false));
        decorations.put("next_page", SimpleRecipeBookLayout.namedItem(Material.ARROW, "下一頁", false));
        decorations.put("back", SimpleRecipeBookLayout.namedItem(Material.BARRIER, "返回", false));

        // 選擇頁完全不用玻璃片，直接從第 1 格 (Slot 0) 開始整齊放滿
        List<String> layout = List.of(
                "RRRRRRRRR",
                "RRRRRRRRR",
                "P   B   N"
        );

        return new SimpleRecipeBookLayout(title(), 3, layout, legend, decorations);
    }

    @Override
    public RecipeBookLayout detailLayout() {
        Map<Character, String> legend = new HashMap<>();
        legend.put('I', "ingredient");
        legend.put('C', "container");
        legend.put('R', "result");
        legend.put('P', "progress");
        legend.put('F', "fill");
        legend.put('B', "back");
        legend.put('S', "separator"); // 中間分隔玻璃片
        legend.put(' ', "empty");     // 其餘全乾淨空白

        Map<String, ItemStack> decorations = new HashMap<>();
        decorations.put("separator", SimpleRecipeBookLayout.namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", true));
        decorations.put("fill", SimpleRecipeBookLayout.namedItem(Material.HOPPER, "填入材料", false));
        decorations.put("back", SimpleRecipeBookLayout.namedItem(Material.ARROW, "返回", false));

        // 榨汁機配方頁：全乾淨背景，中柱 (Col 4) 用玻璃片乾淨俐落隔開左側原料與右側產物/容器
        // 左側原料：Col 2 垂直兩格 (原料1 在 Row 0, 原料2 在 Row 1)
        // 中間隔板：Col 4 垂直三格 (分隔玻璃片 S)
        // 右側產出：Col 6 垂直兩格 (果汁成品 R 在 Row 0, 容器瓶子 C 在 Row 1)
        // 底部工具：Col 0 (返回 B), Col 8 (快速填入 F)
        List<String> layout = List.of(
                "  I S R  ",
                "  I S C  ",
                "B   S   F"
        );

        return new SimpleRecipeBookLayout(title(), 3, layout, legend, decorations);
    }
}
