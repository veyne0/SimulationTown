package com.simulationtown.towncenter;


import com.buildlib.api.BuildCompletedEvent;
import com.buildlib.api.BuildingFinishedEvent;
import com.simulationtown.Agriculture.agriculture_block_entity;
import com.simulationtown.SimulationTown;

import com.simulationtown.building.BuildingData;
import com.simulationtown.building.build_bind;
import com.simulationtown.industry.industry_block_entity;
import com.simulationtown.residence.residence_block_entity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import static com.simulationtown.SimulationTown.TOWN_CENTER;
import static com.simulationtown.block.TOWN_CENTER_BLOCK;

@EventBusSubscriber(modid = "simulation_town")
public class listen_event {
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event){
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if(event.getPlacedBlock().is(TOWN_CENTER_BLOCK.get())){
            BlockPos a = player.getExistingDataOrNull(TOWN_CENTER);
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
        int block_count=e.getTotalBlocks();

        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ())) {
            // 统一走 BuildingData 接口 (4 类管理方块同一套绑定写入)
            if (level.getBlockEntity(pos) instanceof BuildingData be) {
                be.set_block_count(block_count);
                be.setBuildingId(buildingId);
                be.setBuildingBounds(box);
                be.setRotationSteps(e.getRotationSteps());   // 旋转量持久化, NPC 重建用
                be.setBuildFinished(false);
                be.setCompletion(0.0);
                if(e.getPlayer().getData(TOWN_CENTER)!=null){
                    be.settown(e.getPlayer().getData(TOWN_CENTER));
                }

                if(level.getBlockEntity(pos) instanceof residence_block_entity rbe){
                    rbe.sava_r_GDP(0.0);

                }
                if(level.getBlockEntity(pos) instanceof industry_block_entity ibe){
                    ibe.sava_i_GDP(0.0);
                }
                if(level.getBlockEntity(pos) instanceof agriculture_block_entity abe){
                    abe.sava_a_GDP(0.0);
                }


            }
        }
    }


    @SubscribeEvent
    static void onBuildingFinished(BuildingFinishedEvent e) {
        var be = e.getLevel().getBlockEntity(e.getManagerPos());
        BlockPos bp =e.getOwnerPlayer().getData(TOWN_CENTER);
        if (e.getOwnerPlayer()==null) return;


        BlockEntity cbe=e.getLevel().getBlockEntity(bp);

        if (be instanceof BuildingData bd && !bd.isBuildFinished()) return;

        if (be instanceof residence_block_entity rbe ){
            if(cbe instanceof centerBlockEntity ce){
                rbe.sava_r_GDP(rbe.r_block_count * 0.2 * 0.1);
                ce.GDP_manager.put(e.getManagerPos(),rbe.r_GDP);
            }


        }
        if (be instanceof industry_block_entity ibe){
            if(cbe instanceof centerBlockEntity ce){
                ibe.sava_i_GDP(ibe.i_block_count * 0.5 * 0.1);
                ce.GDP_manager.put(e.getManagerPos(),ibe.i_GDP);
            }

        }
        if (be instanceof agriculture_block_entity abe) {
           if(cbe instanceof centerBlockEntity ce){
               abe.sava_a_GDP(abe.a_block_count * 0.5 * 0.1);
               ce.GDP_manager.put(e.getManagerPos(),abe.a_GDP);
           }

        }
    }

}
