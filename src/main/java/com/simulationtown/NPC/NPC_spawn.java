package com.simulationtown.NPC;

import com.simulationtown.SimulationTown;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class NPC_spawn {
    public static NPC spawnNPC(ServerLevel level, BlockPos pos) {
        NPC npc = NPC_register.NPC.get().create(level);
        if (npc !=null){
            npc.setCustomName(Component.literal(NPC_name.randomName()));
            npc.setCustomNameVisible(true);         // 头顶常驻显示
            level.addFreshEntity(npc);
            return npc;
        }else{ return null;}

    }
}
