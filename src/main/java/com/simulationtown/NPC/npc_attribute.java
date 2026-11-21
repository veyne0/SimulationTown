package com.simulationtown.NPC;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = "simulation_town", bus = EventBusSubscriber.Bus.MOD)   // 必须 MOD 总线!
public class npc_attribute {
    @SubscribeEvent
    static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(NPC_register.NPC.get(), NPC.createAttributes().build());
    }
}
