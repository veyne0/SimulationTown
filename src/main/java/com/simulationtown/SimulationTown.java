package com.simulationtown;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.util.function.Supplier;

import static com.simulationtown.block.BLOCK_ITEM;

/**
 * 模组入口类。
 *
 * <p>它做什么：NeoForge 启动时构造这个类的实例，模组的一切从这里开始。
 * <p>什么时候被调用：游戏启动、加载模组列表的时候，早于任何世界加载。
 * <p>依赖谁：mods.toml 里声明了 modId，NeoForge 按 modId 找到这个类。
 */
@Mod(SimulationTown.MOD_ID)
public class SimulationTown {

    /** modid，和 neoforge.mods.toml 里的 modId、以及所有资源路径前缀必须一致 */
    public static final String MOD_ID = "simulation_town";

    public static final Logger LOGGER = LogUtils.getLogger();

    public SimulationTown(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("[Simulation Town] done loading");
        modEventBus.addListener(this::addCreative);
        block.BLOCKS.register(modEventBus);
        block.ITEMS.register(modEventBus);
        block.BLOCK_ENTITY_TYPES.register(modEventBus);
        SimulationTown.ATTACHMENT_TYPES.register(modEventBus);


        // 阶段 1 的注册写在这里（方块、物品、创造模式标签页……）
    }
    @SubscribeEvent
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey()==CreativeModeTabs.BUILDING_BLOCKS){
            event.accept(block.BLOCK_ITEM.get());

        }


    }
    /*
    注册数据附件
    */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "simulation_town");

    public static final Supplier<AttachmentType<BlockPos>> TOWN_CENTER =
            ATTACHMENT_TYPES.register("town_center", () ->
                    AttachmentType.builder(() -> BlockPos.ZERO) // 默认坐标0,0,0
                            .serialize(BlockPos.CODEC) // 用原版的序列化
                            .copyOnDeath()
                            .build());
}
