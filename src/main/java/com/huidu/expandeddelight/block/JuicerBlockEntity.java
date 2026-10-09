package com.huidu.expandeddelight.block;

import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * 代表一個真實世界中運行的榨汁機方塊狀態
 */
public class JuicerBlockEntity {

    // 0, 1: 原料槽
    // 2: 榨出之未裝瓶飲品預覽/暫存 (Drink Display)
    // 3: 容器槽 (如 Glass Bottle)
    // 4: 裝瓶產出槽 (Output)
    public static final int INPUT_1 = 0;
    public static final int INPUT_2 = 1;
    public static final int DRINK_DISPLAY = 2;
    public static final int CONTAINER = 3;
    public static final int OUTPUT = 4;
    public static final int TOTAL_SLOTS = 5;

    private final ItemStack[] inventory = new ItemStack[TOTAL_SLOTS];
    private int juiceTime = 0;
    private int totalJuiceTime = 200;
    private boolean dirty = false;

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) return null;
        return inventory[slot];
    }

    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            inventory[slot] = item;
        }
    }

    public int getJuiceTime() {
        return juiceTime;
    }

    public void setJuiceTime(int juiceTime) {
        this.juiceTime = juiceTime;
    }

    public int getTotalJuiceTime() {
        return totalJuiceTime;
    }

    public void setTotalJuiceTime(int totalJuiceTime) {
        this.totalJuiceTime = Math.max(1, totalJuiceTime);
    }

    public int getProgressPercent() {
        if (totalJuiceTime <= 0) return 0;
        return Math.min(100, Math.max(0, (int) Math.round((double) juiceTime * 100.0 / totalJuiceTime)));
    }

    public void clear() {
        Arrays.fill(inventory, null);
        juiceTime = 0;
    }
}
