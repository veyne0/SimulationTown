package com.simulationtown.AI;

import com.simulationtown.NPC.NPC;
import com.simulationtown.industry.industry_block_entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;
/*  这个是构石 */
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
        if (find_tree(this.npc.level())!=null){
            this.work_pos=find_tree(this.npc.level());
            if(!npc.blockPosition().closerThan(work_pos, 4.5D)){
                npc.getNavigation().moveTo(work_pos.getX(),work_pos.getY(), work_pos.getZ(), 1.0D);

            }

            }
        }
    @Override
    public void tick(){
        if (work_pos ==null) return;
        npc.getLookControl().setLookAt(work_pos.getX(), work_pos.getY(), work_pos.getZ());

        if (npc.blockPosition().closerThan(work_pos, 4.5D)) {
            if (!npc.getNavigation().isDone()) {
                npc.getNavigation().stop();
            }

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
        if(work_pos!=null){
            return work_pos != null && !npc.getNavigation().isDone()|| npc.blockPosition().closerThan(work_pos, 4.5D);
        }
        return  false;

    }
    @Override
    public void stop() {
        this.work_pos = null;
        npc.getNavigation().stop();
    }

    public BlockPos find_tree(Level level){
        if(npc.guanliblock_pos!=null){
            BlockEntity be = level.getBlockEntity(npc.guanliblock_pos);
            BlockPos npc_pos =this.npc.blockPosition();
            if(be instanceof industry_block_entity ibe){
                if(ibe.i_work_pos_first !=null && ibe.i_work_pos_end !=null){
                    for (BlockPos pos : BlockPos.betweenClosed(ibe.i_work_pos_first,ibe.i_work_pos_end)){
                        if (level.isLoaded(pos) && npc.level().getBlockState(pos).is(Blocks.OAK_LOG)) {
                            return pos.immutable();
                        }
                    }
                }

            }

        }
        return null;

    }
}
