package com.simulationtown.towncenter;

import com.buildlib.economy.GdpManager;
import com.simulationtown.SimulationTown;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
                    int    money = mc.player.getData(SimulationTown.PLAYER_JINGJI);
                    String name=mc.player.getData(SimulationTown.TOWN_CENTER_NAME);
                    if(name !=null ){
                        if(!name.isEmpty()){
                            guiGraphics.drawString(Minecraft.getInstance().font,
                                    "小镇GDP " + String.format("%.2f", gdp), 10, 10, 0xFFFFFF, true);
                            guiGraphics.drawString(Minecraft.getInstance().font,
                                    "玩家资金 " + money, 10, 20, 0xFFFFFF, true);

                        }

                    }



                }
        );
    }

    @EventBusSubscriber(modid = "simulation_town")
    public class hud_tick {
        private static int timer = 0;
        @SubscribeEvent
        static void onServerTick(ServerTickEvent.Post e) {
            if (++timer < 20) return;              // 每秒一次
            timer = 0;
            for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
                p.setData(SimulationTown.TOWN_GDP,GdpManager.queryCumulativeGdp(p.serverLevel()));
            }
        }
    }

}
