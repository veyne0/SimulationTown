package com.simulationtown.NPC;

import com.simulationtown.towncenter.centerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class NPC_spawn {
    public static NPC spawnNPC(ServerLevel level, BlockPos pos, Player player) {
        NPC npc = NPC_register.NPC.get().create(level);
        if (npc !=null){
            String n_name=NPC_name.randomName();
            if(!centerBlockEntity.get_all_NPC_name( player).isEmpty() &&centerBlockEntity.get_all_NPC_name( player).contains(n_name) ){
                String n1_name=NPC_name.randomName();
                npc.setCustomName(Component.literal(n1_name));
                npc.setCustomNameVisible(true);
                npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        level.getRandom().nextFloat() * 360f, 0);
                level.addFreshEntity(npc);
                npc.setSkinIndex(ThreadLocalRandom.current().nextInt(NPC_render.skincount));
                npc.setPersistenceRequired();
                UUID n_uuid=npc.getUUID();
                centerBlockEntity.sava_all_NPC(n1_name,n_uuid,player);
                return npc;

            }else{
                npc.setCustomName(Component.literal(n_name));
                npc.setCustomNameVisible(true);
                npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        level.getRandom().nextFloat() * 360f, 0);
                level.addFreshEntity(npc);
                npc.setSkinIndex(ThreadLocalRandom.current().nextInt(NPC_render.skincount));
                npc.setPersistenceRequired();
                UUID n_uuid=npc.getUUID();
                centerBlockEntity.sava_all_NPC(n_name,n_uuid,player);
                return npc;
            }


        }else{ return null;}

    }
}
