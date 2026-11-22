package com.simulationtown;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.simulationtown.NPC.NPC_register;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;



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
        // 开发环境（Gradle run）的 JVM 常常是 headless 的：AWT 认为这台机器没有显示器，
        // JFileChooser.showOpenDialog() 会抛 HeadlessException 直接把游戏带崩。
        // 这个属性必须在任何 AWT 类被加载之前设置，所以放在构造器最前面。
        System.setProperty("java.awt.headless", "false");

        LOGGER.info("[Simulation Town] done loading");
        modEventBus.addListener(this::addCreative);
        block.BLOCKS.register(modEventBus);
        block.ITEMS.register(modEventBus);
        block.BLOCK_ENTITY_TYPES.register(modEventBus);
        NPC_register.ENTITIES.register(modEventBus);

        SimulationTown.ATTACHMENT_TYPES.register(modEventBus);

        Path rootDir = FMLPaths.GAMEDIR.get().resolve("simulation_town_buliding");
        String[] categories = {"residence", "agriculture", "industry", "special", "_incoming"};
        for (String category : categories) {
            Path categoryDir = rootDir.resolve(category);
            if (!Files.exists(categoryDir)) {
                try {
                    Files.createDirectories(categoryDir);
                    System.out.println("已创建分类文件夹: " + categoryDir.toString());
                } catch (Exception e) {
                    e.printStackTrace();}}}

        LOGGER.info("[Simulation Town] AWT headless = {}", java.awt.GraphicsEnvironment.isHeadless());
    }
    @SubscribeEvent
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey()==CreativeModeTabs.BUILDING_BLOCKS){
            event.accept(block.BLOCK_ITEM.get());
            event.accept(block.BUILD_BLOCK_ITEM.get());
            event.accept(block.RESIDENCE_BLOCK_ITEM.get());
            event.accept(block.SPECIAL_BLOCK_ITEM.get());
            event.accept(block.AGRICULTURE_BLOCK_ITEM.get());
            event.accept(block.INDUSTRY_BLOCK_ITEM.get());

        }
    }

    /*
    注册数据附件
    */
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "simulation_town");

    public static final Supplier<AttachmentType<BlockPos>> TOWN_CENTER =
            ATTACHMENT_TYPES.register("town_center", () ->
                    AttachmentType.builder(() -> (BlockPos)null) // 默认坐标0,0,0
                            .serialize(BlockPos.CODEC) // 用原版的序列化
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<String>> TOWN_CENTER_NAME =
            ATTACHMENT_TYPES.register("town_center_name", () ->
                    AttachmentType.builder(() -> (String)null)
                            .serialize(Codec.STRING) // 用原版的序列化
                            .sync(ByteBufCodecs.STRING_UTF8)
                            .copyOnDeath()
                            .build());

    /*这个是玩家自己本身的经济,以后可能会改为double*/
    public static final Supplier<AttachmentType<Integer>> PLAYER_JINGJI =
            ATTACHMENT_TYPES.register("player_jingji", () ->
                    AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT) // 用原版的序列化
                            .sync(ByteBufCodecs.INT)
                            .copyOnDeath()
                            .build());


    /*这个是小镇的GDP*/
    public static final Supplier<AttachmentType<Double>> TOWN_GDP =
            ATTACHMENT_TYPES.register("town_gdp", () ->
                    AttachmentType.builder(() -> 0.0)
                            .serialize(Codec.DOUBLE) // 用原版的序列化
                            .sync(ByteBufCodecs.DOUBLE)
                            .copyOnDeath()
                            .build());

    public static final Supplier<AttachmentType<UUID>> NPC_TALKING =
            ATTACHMENT_TYPES.register("npc_talking", () ->
                    AttachmentType.builder(() -> (UUID) null)
                            .serialize(UUIDUtil.CODEC)
                            .sync(UUIDUtil.STREAM_CODEC)
                            .copyOnDeath()
                            .build());


    public static final Supplier<AttachmentType<List<CompoundTag>>> ALL_NPC =
            ATTACHMENT_TYPES.register("all_npc", () ->
                    AttachmentType.<List<CompoundTag>>builder(() -> new ArrayList<>())
                            .serialize(CompoundTag.CODEC.listOf())   // ListTag 没有Codec, 但 List<CompoundTag> 有现成的
                            .copyOnDeath()
                            .build());



}
