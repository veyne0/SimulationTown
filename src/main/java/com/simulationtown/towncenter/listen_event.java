package com.simulationtown.towncenter;

import com.simulationtown.SimulationTown;
import com.simulationtown.block;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

}
