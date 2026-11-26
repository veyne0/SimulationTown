package com.simulationtown.towncenter;



import com.simulationtown.SimulationTown;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.apache.logging.log4j.message.StructuredDataMessage;

@EventBusSubscriber(modid = "simulation_town")
public class GDP {

    public static double day_GDP(Player player,Level level){
         double d_GDP =0.0;
         if(player.getData(SimulationTown.TOWN_CENTER)==null) return 0.0;
        BlockPos bp=player.getData(SimulationTown.TOWN_CENTER);
        BlockEntity be=level.getBlockEntity(bp);
        if(be instanceof  centerBlockEntity cbe){
            for (double dl : cbe.GDP_manager.values()){
                d_GDP +=dl;
            }
            return  d_GDP;

        }
        return 0.0;
    }
    @SubscribeEvent

    public static void onLevelTick(LevelTickEvent.Post event) {

        if (event.getLevel() instanceof ServerLevel serverLevel) {


            if (serverLevel.getGameTime() % 20 == 0) {

                long currentDay = serverLevel.getDayTime() / 24000;

                long dayTime = serverLevel.getDayTime() % 24000;


                if (dayTime >= 0 &&dayTime <=100) {

                    for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                        int ld =player.getData(SimulationTown.LAST_DAY);
                        if(player.getData(SimulationTown.TOWN_CENTER)==null) continue;
                        if (currentDay < ld) {                       // /time set 回拨: 天数变小 → 重置基线, 本轮跳过
                            player.setData(SimulationTown.LAST_DAY, -1);
                            continue;
                        }
                        if(currentDay >ld){
                            player.setData(SimulationTown.LAST_DAY,(int) currentDay);
                            double p=player.getData(SimulationTown.PLAYER_JINGJI);
                            player.setData(SimulationTown.PLAYER_JINGJI,p+day_GDP(player,event.getLevel())*0.1);
                            double gp = player.getData(SimulationTown.TOWN_GDP);
                            player.setData(SimulationTown.TOWN_GDP,gp+day_GDP(player,event.getLevel()));
                            player.sendSystemMessage(Component.literal("小镇GDP结算"+String.format("%.2f",day_GDP(player,event.getLevel()))+"     玩家资金结算"+ String.format("%.2f",day_GDP(player,event.getLevel())*0.1)));

                        }


                    }

                }
            }
        }
    }
}