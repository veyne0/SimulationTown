package com.simulationtown.towncenter;

import com.simulationtown.SimulationTown;
import com.simulationtown.block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

import static com.simulationtown.SimulationTown.TOWN_CENTER;
import static com.simulationtown.SimulationTown.TOWN_CENTER_NAME;
import static com.simulationtown.towncenter.consume.hasItem;
import static com.simulationtown.towncenter.consume.xiaohao;

public class centerBlockEntity extends BlockEntity {
    int init_resident = 2;
    int max_resident=100;
    int now_max_resident;
    int now_resident;
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
       tag.putInt("now_resident",this.now_resident);
       tag.putInt("now_max_resident",this.now_max_resident);
   }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.level = tag.getInt("level");
        if (tag.hasUUID("UUID")) {
            this.player_UUID = tag.getUUID("UUID"); // 读出来
        }
        this.town_name=tag.getString("name");
        this.now_resident=tag.getInt("now_resident");
        this.now_max_resident=tag.getInt("now_max_resident");
    }
    /*
    升级镇中心用的
     */
    public void shengji(Player player){
        int jing_ji=player.getData(SimulationTown.PLAYER_JINGJI);

       if(this.level ==1 &&hasItem(player,Items.IRON_INGOT,64)){
           if (jing_ji >=10){
               xiaohao(player, Items.IRON_INGOT,64);
               this.level +=1;
               player.setData(SimulationTown.PLAYER_JINGJI,jing_ji-10);
               this.setChanged();
           }


       }
        if(this.level ==2 &&hasItem(player,Items.GOLD_INGOT ,64)) {
            if (jing_ji >= 500) {
                xiaohao(player, Items.GOLD_INGOT, 64);
                this.level += 1;
                player.setData(SimulationTown.PLAYER_JINGJI, jing_ji - 500);
                this.setChanged();
            }
        }
        if(this.level ==3 &&hasItem(player,Items.DIAMOND ,64)) {
            if (jing_ji >= 5000) {
                xiaohao(player, Items.DIAMOND, 64);
                this.level += 1;
                player.setData(SimulationTown.PLAYER_JINGJI, jing_ji - 5000);
                this.setChanged();
            }


        }

        if(this.level ==4 &&hasItem(player,Items.NETHER_STAR ,1)) {
            if (jing_ji >= 20000) {
                xiaohao(player, Items.NETHER_STAR, 1);
                this.level += 1;
                player.setData(SimulationTown.PLAYER_JINGJI, jing_ji -20000);
                this.setChanged();
            }
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
    public int get_now_resident(){
       return this.now_resident;
    }
    public void save_now_resident(int nr){
       this.now_resident=nr;
       this.setChanged();
    }

}
