package com.simulationtown.towncenter;

import com.buildlib.api.BuildCompletedEvent;
import com.simulationtown.Agriculture.agriculture_block_entity;
import com.simulationtown.SimulationTown;
import com.simulationtown.block;
import com.simulationtown.building.BuildingData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import static com.simulationtown.block.TOWN_CENTER_BLOCK;

@EventBusSubscriber(modid = "simulation_town")
public class listen_event {
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event){
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if(event.getPlacedBlock().is(TOWN_CENTER_BLOCK.get())){
            BlockPos a = player.getExistingDataOrNull(SimulationTown.TOWN_CENTER);
            if(a!=null){
                event.setCanceled(true);
            }

        }
    }

    @SubscribeEvent
    static void onBuildDone(BuildCompletedEvent e) {
        String buildingId = e.getContext();
        BoundingBox box = e.getWorldBounds();
        Level level = e.getLevel();

        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ())) {
            // 统一走 BuildingData 接口 (4 类管理方块同一套绑定写入)
            if (level.getBlockEntity(pos) instanceof BuildingData be) {
                be.setBuildingId(buildingId);
                be.setBuildingBounds(box);
                be.setRotationSteps(e.getRotationSteps());   // 旋转量持久化, NPC 重建用
                be.setBuildFinished(false);
                be.setCompletion(0.0);
            }
        }
    }

}
