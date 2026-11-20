package com.simulationtown.building;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.simulationtown.GUI.build_block_GUI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.property.Properties;

public class build_block extends Block implements BlockUIMenuType.BlockUI {

    public build_block(Properties properties) {
        super(properties);
    }

    /** 让下一次扫描重新读盘（导入完成后调用）。实现在界面类里。 */
    public static void refresh() {
        build_block_GUI.refresh();
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockUIMenuType.openUI((ServerPlayer) player, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** 界面在 com.simulationtown.GUI.build_block_GUI，这里只负责转交。 */
    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return build_block_GUI.create(holder);
    }
}
