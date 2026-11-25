package com.simulationtown.AI;

import com.simulationtown.NPC.NPC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumSet;

public class kanshu_goal extends Goal {
    public NPC npc;
    public BlockPos work_pos;
    public BlockPos pos;
    public kanshu_goal(NPC npc){
        this.npc =npc ;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE,Goal.Flag.LOOK));
    }
    public BlockPos set_work_pos(BlockPos pos){
        if (pos !=null){
            this.work_pos =pos;
            return work_pos;
        }
        return null;
    }
    @Override
    public boolean canUse(){
        if (this.npc.getJobSynced().equals("lumberjack")){
            return true;
        }
        return false;
    }
    @Override
    public void start(){
        if (find_tree()!=null){
            this.work_pos=find_tree();
            npc.getNavigation().moveTo(work_pos.getX(),work_pos.getY(), work_pos.getZ(), 1.0D);
            }
        }
    @Override
    public void tick(){
        if (work_pos ==null) return;
        npc.getLookControl().setLookAt(work_pos.getX(), work_pos.getY(), work_pos.getZ());

        if (npc.blockPosition().closerThan(work_pos, 2.0D)) {

            npc.swing(InteractionHand.MAIN_HAND);


            if (npc.tickCount % 20 == 0) {

                npc.level().destroyBlock(work_pos, false);

                ItemEntity item = new ItemEntity(npc.level(), work_pos.getX(), work_pos.getY(), work_pos.getZ(), new ItemStack(Items.OAK_LOG));
                npc.level().addFreshEntity(item);

                this.work_pos = null;
            }
        }
    }
    @Override
    public boolean canContinueToUse() {

        return work_pos != null && !npc.getNavigation().isDone();
    }
    @Override
    public void stop() {
        this.work_pos = null;
        npc.getNavigation().stop();
    }

    public BlockPos find_tree(){
        BlockPos npc_pos =this.npc.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(npc_pos.offset(0, 0, 0), npc_pos.offset(10, 3, 10))){
            if (npc.level().getBlockState(pos).is(Blocks.OAK_LOG)) {
                return pos.immutable();
            }
        }
        return null;

    }
}
