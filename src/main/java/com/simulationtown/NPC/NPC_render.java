package com.simulationtown.NPC;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "simulation_town", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class NPC_render {
    public static int skincount=10;
    @SubscribeEvent
    static void onRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NPC_register.NPC.get(), ctx ->
                new HumanoidMobRenderer<>(ctx,
                        new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)),   // vanilla 人形模型
                        0.5f) {                                                    // 阴影大小
                    @Override
                    public ResourceLocation getTextureLocation(NPC entity) {
                        return ResourceLocation.fromNamespaceAndPath("simulation_town",
                                "textures/entity/npc_" + entity.getSkinIndex() + ".png");
                    }
                });
    }
}
