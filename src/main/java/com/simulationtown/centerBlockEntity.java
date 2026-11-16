package com.simulationtown;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class centerBlockEntity extends BlockEntity {
    int resident = 2;
    private UUID player_UUID;
    int level=1;
    int max_level=5;

   public centerBlockEntity(BlockPos pos , BlockState state){
       super(block.CBE.get(),pos ,state);
   }
   @Override
   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
       super.saveAdditional(tag, registries);
       tag.putInt("level", this.level);
       if (player_UUID!=null){
           tag.putUUID("UUID",this.player_UUID);
       }
   }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.level = tag.getInt("level");
        if (tag.hasUUID("UUID")) {
            this.player_UUID = tag.getUUID("UUID"); // 读出来
        }
    }
    public void shengji(){
       if(this.level<=this.max_level){
           this.level+=1;
           this.setChanged();
       }else{
           this.level+=0;
       }

    }
    public  int get_level(){
       return this.level;
    }
    public void bangding(Player player){
       this.player_UUID=player.getUUID();
       this.setChanged();
    }
    public UUID getUUID(){
       return this.player_UUID;
    }
}
