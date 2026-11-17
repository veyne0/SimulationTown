package com.simulationtown.towncenter;

import com.simulationtown.SimulationTown;
import com.simulationtown.block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

import static com.simulationtown.SimulationTown.TOWN_CENTER;
import static com.simulationtown.SimulationTown.TOWN_CENTER_NAME;

public class centerBlockEntity extends BlockEntity {
    int init_resident = 2;
    int max_resident=100;
    private UUID player_UUID;
    int level=1;
    int max_level=5;
    BlockPos pos ;
    String town_name;

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
       if (this.town_name !=null){
           tag.putString("name",this.town_name);
       }
   }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.level = tag.getInt("level");
        if (tag.hasUUID("UUID")) {
            this.player_UUID = tag.getUUID("UUID"); // 读出来
        }
        this.town_name=tag.getString("name");
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
    public BlockPos get_pos(){
       return this.pos;
    }
    public void zuobiao(){
       this.pos=getBlockPos();


    }
    public void qingli(Player player ){
        BlockPos a = player.getExistingDataOrNull(SimulationTown.TOWN_CENTER);
        if (a!=null){
            player.removeData(TOWN_CENTER);
        }

    }
    public void jianli(Player player,String name){
       this.town_name =name;
       if (this.town_name !=null){
           player.setData(TOWN_CENTER_NAME,name);

       }
    }

}
