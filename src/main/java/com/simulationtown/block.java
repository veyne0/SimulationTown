package com.simulationtown;

import com.simulationtown.towncenter.centerBlockEntity;
import com.simulationtown.towncenter.centerblock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class block {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks("simulation_town");
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems("simulation_town");
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "simulation_town");
    public static final DeferredBlock<centerblock> TOWN_CENTER_BLOCK =
            BLOCKS.registerBlock("center_block",centerblock::new,BlockBehaviour.Properties.of().strength(-1.0f,3600000.0f).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(TOWN_CENTER_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<centerBlockEntity>> CBE =
            BLOCK_ENTITY_TYPES.register("cbe", () ->
                    BlockEntityType.Builder.<centerBlockEntity>of(
                            centerBlockEntity::new,
                            TOWN_CENTER_BLOCK.get()
                    ).build(null));
}