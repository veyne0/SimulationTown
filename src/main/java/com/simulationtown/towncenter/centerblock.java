package com.simulationtown.towncenter;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.simulationtown.GUI.centerblock_GUI;
import com.simulationtown.SimulationTown;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class centerblock extends Block implements BlockUIMenuType.BlockUI, EntityBlock {

    public centerblock(Properties properties) {

        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new centerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof centerBlockEntity be) {
            if (be.getUUID() == null) {
                be.bangding(player);

            }
            if (be.get_pos() == null) {
                BlockPos TownPos = player.getData(SimulationTown.TOWN_CENTER);
                player.setData(SimulationTown.TOWN_CENTER, new BlockPos(pos));
                be.zuobiao();
            }

            if (!level.isClientSide) {
                if (be.getUUID().equals(player.getUUID()) && pos.equals(player.getData(SimulationTown.TOWN_CENTER))) {
                    BlockUIMenuType.openUI((ServerPlayer) player, pos);
                } else {
                    player.displayClientMessage(Component.literal("你已经有小镇了"), true);

                }

            }

        }
        return InteractionResult.SUCCESS;
    }

    /** 界面在 com.simulationtown.GUI.centerblock_GUI，这里只负责转交。 */
    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return centerblock_GUI.create(holder);
    }
}
