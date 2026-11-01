package com.agrarianfutures;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** Everything the Ticker Board will buy. Base prices are in emeralds per item. */
public enum Commodity {
    WHEAT(Items.WHEAT, 0.20),
    CARROT(Items.CARROT, 0.15),
    POTATO(Items.POTATO, 0.15),
    BEETROOT(Items.BEETROOT, 0.20),
    PUMPKIN(Items.PUMPKIN, 0.25),
    MELON_SLICE(Items.MELON_SLICE, 0.08),
    SUGAR_CANE(Items.SUGAR_CANE, 0.10),
    LEATHER(Items.LEATHER, 0.35),
    FEATHER(Items.FEATHER, 0.10),
    EGG(Items.EGG, 0.10),
    IRON_INGOT(Items.IRON_INGOT, 0.50);

    public final Item item;
    public final double base;

    Commodity(Item item, double base) {
        this.item = item;
        this.base = base;
    }

    /** Returns the commodity for a plain stack of it, or null. Renamed or enchanted stacks are not accepted. */
    public static Commodity of(ItemStack stack) {
        if (stack.isEmpty() || stack.hasNbt()) {
            return null;
        }
        for (Commodity c : values()) {
            if (stack.isOf(c.item)) {
                return c;
            }
        }
        return null;
    }
}
