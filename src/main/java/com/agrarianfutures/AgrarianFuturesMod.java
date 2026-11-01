package com.agrarianfutures;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class AgrarianFuturesMod implements ModInitializer {
    public static final String MOD_ID = "agrarianfutures";

    public static final Block TICKER_BOARD =
            new TickerBoardBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS));

    public static final Item TICKER_BOARD_ITEM =
            new BlockItem(TICKER_BOARD, new Item.Settings());

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "ticker_board"), TICKER_BOARD);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "ticker_board"), TICKER_BOARD_ITEM);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register(entries -> entries.add(TICKER_BOARD_ITEM));
    }
}
