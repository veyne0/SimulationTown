package com.simulationtown.residence;

import com.simulationtown.block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class residence_block_entity extends BlockEntity {
    String resident_name;
    String builder_name;
    Boolean r_build_state;
    double r_Completion_level;
    public residence_block_entity(BlockPos pos , BlockState state){
        super(block.RBE.get(),pos ,state);
    }
}
