package com.simulationtown.NPC;

import com.simulationtown.SimulationTown;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.ThreadLocalRandom;

public class NPC_spawn {
    public static NPC spawnNPC(ServerLevel level, BlockPos pos) {
        NPC npc = NPC_register.NPC.get().create(level);
        if (npc !=null){
            npc.setCustomName(Component.literal(NPC_name.randomName()));
            npc.setCustomNameVisible(true);// 头顶常驻显示
            npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    level.getRandom().nextFloat() * 360f, 0);
            level.addFreshEntity(npc);
            npc.skinIndex = ThreadLocalRandom.current().nextInt(NPC_render.skincount);
            return npc;

        }else{ return null;}

    }
}
