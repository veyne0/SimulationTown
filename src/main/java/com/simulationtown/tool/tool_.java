package com.simulationtown.tool;

import com.simulationtown.Agriculture.agriculture_block_entity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@EventBusSubscriber(modid = "simulation_town")
public  class tool_ {
    public static Map<UUID, BlockPos> pos1 =new HashMap<>();
    public static Map<UUID, BlockPos> pos2=new HashMap<>();

    @SubscribeEvent
    public static void onLeftclick(PlayerInteractEvent.LeftClickBlock event){
        if (!event.getEntity().getMainHandItem().is(Items.STICK)){
            return;
        }
        if(!event.getLevel().isClientSide()){
            UUID playerId = event.getEntity().getUUID();
            BlockPos click1 = event.getPos();

            pos1.put(playerId, click1);
            event.getEntity().displayClientMessage(
                    net.minecraft.network.chat.Component.literal("第一个点已选择: " + click1.toShortString()),
                    false
            );

            event.setCanceled(true);
        }
    }
    @SubscribeEvent
    public static void onRigthclick(PlayerInteractEvent.RightClickBlock event){
        if (!event.getEntity().getMainHandItem().is(Items.STICK)){
            return;
        }
        if(!event.getLevel().isClientSide()){

            UUID playerId = event.getEntity().getUUID();
            BlockPos click2 = event.getPos();
            if(!pos1.containsKey(playerId)){
                event.getEntity().displayClientMessage(
                        net.minecraft.network.chat.Component.literal("先选第一个点: " + click2.toShortString()),
                        false);
                return;
            }

            pos2.put(playerId, click2);
            event.getEntity().displayClientMessage(
                    net.minecraft.network.chat.Component.literal("第2个点已选择: " + click2.toShortString()),
                    false
            );
            BlockPos pos1_=pos1.get(playerId);
            BlockPos pos2_=pos2.get(playerId);

    }
}

}
