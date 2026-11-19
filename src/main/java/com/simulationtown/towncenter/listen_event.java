package com.simulationtown.towncenter;

import com.buildlib.api.BuildCompletedEvent;
import com.simulationtown.Agriculture.agriculture_block_entity;
import com.simulationtown.SimulationTown;
import com.simulationtown.block;
import com.simulationtown.industry.industry_block_entity;
import com.simulationtown.residence.residence_block_entity;
import com.simulationtown.special.special_block_entity;
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
            if (level.getBlockEntity(pos) instanceof residence_block_entity be) {
                be.setBuildingId(buildingId);
                be.setBuildingBounds(box);      // ← 同一个 box 写进去
            }
            if (level.getBlockEntity(pos) instanceof special_block_entity be) {
                be.setBuildingId(buildingId);
                be.setBuildingBounds(box);      // ← 同一个 box 写进去
            }
            if (level.getBlockEntity(pos) instanceof industry_block_entity be) {
                be.setBuildingId(buildingId);
                be.setBuildingBounds(box);      // ← 同一个 box 写进去
            }
            if (level.getBlockEntity(pos) instanceof agriculture_block_entity be) {
                be.setBuildingId(buildingId);
                be.setBuildingBounds(box);      // ← 同一个 box 写进去
            }

            // 其它三个分类同理


        }
    }

}
