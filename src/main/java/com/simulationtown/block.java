package com.simulationtown;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class block {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks("simulation_town");
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems("simulation_town");
    public static final DeferredBlock<centerblock> TOWN_CENTER_BLOCK =
            BLOCKS.registerBlock("center_block",centerblock::new,BlockBehaviour.Properties.of().strength(100f,100f).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(TOWN_CENTER_BLOCK);

}