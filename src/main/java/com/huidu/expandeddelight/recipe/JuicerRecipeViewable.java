package com.huidu.expandeddelight.recipe;

import com.huidu.farmersdelight.api.item.FarmersDelightItems;
import com.huidu.farmersdelight.api.recipe.ViewableRecipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 將 ExpandedDelight 的 JuicerRecipe 適配為 FarmersDelight 內建配方書可讀取的 ViewableRecipe
 */
public class JuicerRecipeViewable implements ViewableRecipe {

    private final JuicerRecipe recipe;
    private final List<ItemStack> inputs;
    private final ItemStack result;
    private final ItemStack container;

    public JuicerRecipeViewable(JuicerRecipe recipe) {
        this.recipe = recipe;
        this.inputs = new ArrayList<>();
        for (String ing : recipe.ingredients()) {
            ItemStack item = FarmersDelightItems.createOrFallback(ing, Material.AIR);
            if (item != null && !item.getType().isAir()) {
                inputs.add(item);
            }
        }
        this.result = FarmersDelightItems.createOrFallback(recipe.result(), Material.POTION);
        this.container = recipe.container() != null && !recipe.container().equalsIgnoreCase("none")
                ? FarmersDelightItems.createOrFallback(recipe.container(), Material.GLASS_BOTTLE)
                : null;
    }

    @Override
    public String id() {
        return recipe.key().getKey();
    }

    @Override
    public List<ItemStack> inputs() {
        return inputs;
    }

    @Override
    public ItemStack result() {
        return result;
    }

    @Override
    public List<Component> infoLines(Player viewer) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("🍹 榨汁配方", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lines.add(Component.text("⏱ 榨汁時間: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text((recipe.time() / 20) + " 秒", NamedTextColor.WHITE)));
        if (recipe.experience() > 0) {
            lines.add(Component.text("✦ 經驗值: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(recipe.experience()), NamedTextColor.GREEN)));
        }
        if (container != null && !container.getType().isAir()) {
            lines.add(Component.text("🫙 需要容器: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.translatable(container.translationKey(), NamedTextColor.AQUA)));
        }
        return lines;
    }

    @Override
    public Map<String, List<ItemStack>> displaySlots() {
        if (container != null && !container.getType().isAir()) {
            return Map.of("container", List.of(container));
        }
        return Map.of();
    }

    public JuicerRecipe getRecipe() {
        return recipe;
    }

    public ItemStack getContainer() {
        return container;
    }
}
