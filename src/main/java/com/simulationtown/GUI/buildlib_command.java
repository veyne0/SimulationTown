package com.simulationtown.GUI;

import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /buildlib 指令: OP 打开 BuildLib 配置界面 (建造速度 / 材料消耗 / 给自己加钱).
 *
 * <p>非 OP 玩家看不到这条指令 (requires hasPermission(2)); 界面内的每次修改
 * 还会再验一遍权限, 双保险. 具体界面见 {@link buildlib_config_gui}.</p>
 */
@EventBusSubscriber(modid = "simulation_town")
public class buildlib_command {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("buildlib")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    buildlib_config_gui.open(ctx.getSource().getPlayerOrException());
                    return 1;
                }));
    }
}
