package com.simulationtown;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.simulationtown.block.*;
import static com.simulationtown.block.INDUSTRY_BLOCK_ITEM;
import static com.simulationtown.block.RESIDENCE_BLOCK_ITEM;

public class create_tab {
    // 1. 创建一个专门用于注册创造模式物品栏的 DeferredRegister
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "simulation_town");

    // 2. 注册你专属的物品栏
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOWN_TAB =
            CREATIVE_MODE_TABS.register("town_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("模拟小镇"))
                    .icon(() -> new ItemStack(BUILD_BLOCK_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(block.BLOCK_ITEM.get());
                        output.accept(block.BUILD_BLOCK_ITEM.get());
                        output.accept(AGRICULTURE_BLOCK_ITEM.get());
                        output.accept(SPECIAL_BLOCK_ITEM.get());
                        output.accept(RESIDENCE_BLOCK_ITEM.get());
                        output.accept(INDUSTRY_BLOCK_ITEM.get());

                    })
                    .build());
}
