package com.simulationtown;

import com.simulationtown.Agriculture.agriculture_block_entity;
import com.simulationtown.building.build_block;
import com.simulationtown.building.build_block_entity;
import com.simulationtown.industry.industry_block_entity;
import com.simulationtown.residence.residence_block_entity;
import com.simulationtown.special.special_block_entity;
import com.simulationtown.towncenter.centerBlockEntity;
import com.simulationtown.towncenter.centerblock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.common.property.Properties;
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
/*注册小镇中心*/
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

/*注册建筑方块*/
    public static final DeferredBlock<Block> BUILD_BLOCK =
            BLOCKS.registerBlock("build_block", build_block::new,BlockBehaviour.Properties.of().strength(1.0f,3600.0f));
    public static final DeferredItem<BlockItem> BUILD_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(BUILD_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<build_block_entity>> BBE =
            BLOCK_ENTITY_TYPES.register("bbe", () ->
                    BlockEntityType.Builder.<build_block_entity>of(
                            build_block_entity::new,
                            BUILD_BLOCK.get()
                    ).build(null));

/*注册住宅方块*/
    public static final DeferredBlock<Block> RESIDENCE_BLOCK =
            BLOCKS.registerBlock("build_block", build_block::new,BlockBehaviour.Properties.of().strength(-1.0f,3600.0f));
    public static final DeferredItem<BlockItem> RESIDENCE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(RESIDENCE_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<residence_block_entity>> RBE =
            BLOCK_ENTITY_TYPES.register("rbe", () ->
                    BlockEntityType.Builder.<residence_block_entity>of(
                            residence_block_entity::new,
                            RESIDENCE_BLOCK.get()
                    ).build(null));

    /*注册工业方块*/
    public static final DeferredBlock<Block> INDUSTRY_BLOCK =
            BLOCKS.registerBlock("build_block", build_block::new,BlockBehaviour.Properties.of().strength(-1.0f,3600.0f));
    public static final DeferredItem<BlockItem> INDUSTRY_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(INDUSTRY_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<industry_block_entity>> IBE =
            BLOCK_ENTITY_TYPES.register("ibe", () ->
                    BlockEntityType.Builder.<industry_block_entity>of(
                            industry_block_entity::new,
                            INDUSTRY_BLOCK.get()
                    ).build(null));

    /*注册特殊方块*/
    public static final DeferredBlock<Block> SPECIAL_BLOCK =
            BLOCKS.registerBlock("build_block", build_block::new,BlockBehaviour.Properties.of().strength(-1.0f,3600.0f));
    public static final DeferredItem<BlockItem> SPECIAL_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(SPECIAL_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<special_block_entity>> SBE =
            BLOCK_ENTITY_TYPES.register("sbe", () ->
                    BlockEntityType.Builder.<special_block_entity>of(
                            special_block_entity::new,
                            SPECIAL_BLOCK.get()
                    ).build(null));


    /*注册农业方块*/
    public static final DeferredBlock<Block> AGRICULTURE_BLOCK =
            BLOCKS.registerBlock("build_block", build_block::new,BlockBehaviour.Properties.of().strength(-1.0f,3600.0f));
    public static final DeferredItem<BlockItem> AGRICULTURE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(AGRICULTURE_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<agriculture_block_entity>> ABE =
            BLOCK_ENTITY_TYPES.register("abe", () ->
                    BlockEntityType.Builder.<agriculture_block_entity>of(
                            agriculture_block_entity::new,
                            AGRICULTURE_BLOCK.get()
                    ).build(null));




}