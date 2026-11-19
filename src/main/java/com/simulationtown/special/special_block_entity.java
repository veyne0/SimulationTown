package com.simulationtown.special;

import com.simulationtown.block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class special_block_entity extends BlockEntity {
    public special_block_entity(BlockPos pos , BlockState state){
        super(block.ABE.get(),pos ,state);

    }
}
