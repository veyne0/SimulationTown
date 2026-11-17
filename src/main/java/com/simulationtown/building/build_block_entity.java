package com.simulationtown.building;

import com.simulationtown.block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class build_block_entity extends BlockEntity {
    public build_block_entity(BlockPos pos , BlockState state){
        super(block.BBE.get(),pos ,state);

    }
}
