package com.huidu.expandeddelight.recipe;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public record JuicerRecipe(
        NamespacedKey key,
        List<String> ingredients,
        String container,
        String result,
        int time,
        float experience
) {
}
