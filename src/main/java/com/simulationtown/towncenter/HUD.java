package com.simulationtown.towncenter;

import com.buildlib.economy.GdpManager;
import com.simulationtown.SimulationTown;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.text.DecimalFormat;
@EventBusSubscriber(modid = "speed_display", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class HUD {
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event, Player player, ServerLevel serverLevel){
        int jing_ji_=player.getData(SimulationTown.PLAYER_JINGJI);


        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("simulation_town", "my_hud"),
                (guiGraphics, deltaTracker) -> {
                    DecimalFormat a = new DecimalFormat("0.00");
                    String text1;
                    String text ;
                    text = "小镇GDP"+ GdpManager.queryCurrentTotalGdp(serverLevel);
                    text1 = "玩家资金"+jing_ji_;

                    int x = 10;
                    int y = 10;
                    int color = 0xFFFFFF; // 白色
                    boolean shadow = true;

                    guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            text,
                            x,
                            y,
                            color,
                            shadow
                    );
                    guiGraphics.drawString(
                            Minecraft.getInstance().font,
                            text1,
                            x,
                            y+10,
                            color,
                            shadow
                    );


                }
        );
    }
}
