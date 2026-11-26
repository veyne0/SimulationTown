package com.simulationtown.towncenter;

import com.simulationtown.SimulationTown;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.text.DecimalFormat;

import static net.minecraft.client.multiplayer.chat.LoggedChatMessage.player;

@EventBusSubscriber(modid = "simulation_town", value = Dist.CLIENT)
public class HUD {

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath("simulation_town", "my_hud"),
                (guiGraphics, deltaTracker) -> {
                    var mc = Minecraft.getInstance();
                    if (mc.player == null) return;
                    // 附件已 sync, 客户端读到的就是服务端写的真值
                    double gdp   = mc.player.getData(SimulationTown.TOWN_GDP);
                    double   money = mc.player.getData(SimulationTown.PLAYER_JINGJI);
                    String name=mc.player.getData(SimulationTown.TOWN_CENTER_NAME);
                    if(name !=null ){
                        if(!name.isEmpty()){
                            guiGraphics.drawString(Minecraft.getInstance().font,
                                    "小镇GDP " + String.format("%.2f", gdp), 2, 10, 0xFFFFFF, true);
                            guiGraphics.drawString(Minecraft.getInstance().font,
                                    "你的资金 " + (money == Math.floor(money)
                                            ? String.valueOf((long) money)
                                            : String.format("%.2f", money)), 2, 20, 0xFFFFFF, true);

                        }

                    }



                }
        );
    }


}
