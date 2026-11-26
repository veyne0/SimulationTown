package com.simulationtown.towncenter;

import com.simulationtown.SimulationTown;
import com.simulationtown.block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import static com.simulationtown.SimulationTown.*;
import static com.simulationtown.towncenter.consume.hasItem;
import static com.simulationtown.towncenter.consume.xiaohao;

public class centerBlockEntity extends BlockEntity {
    public static int init_resident = 2;
    public static int max_resident=100;
    public int now_max_resident;
    public int now_resident;
    private UUID player_UUID;
    public int level=1;
    public int max_level=5;
    BlockPos pos ;
    public String town_name;
    public int resident_building_count;
    public int NPC_count;

    public Map<BlockPos ,Double> GDP_manager= new HashMap<>();

    public centerBlockEntity(BlockPos pos , BlockState state){

        super(block.CBE.get(),pos ,state);
   }



    public static void sava_all_NPC(String name, UUID uuid ,Player player){
        List<CompoundTag> all_NPC = new ArrayList<>(player.getData(ALL_NPC));
        CompoundTag npc = new CompoundTag();
        npc.putString("name",name);
        npc.putUUID("uuid_name",uuid);
        all_NPC.add(npc);
        player.setData(ALL_NPC,all_NPC);
    }
    public static List<String> get_all_NPC_name(Player player){
        List<String> npc_list=new ArrayList<>();
        for(CompoundTag a : player.getData(ALL_NPC) ){
            npc_list.add(a.getString("name"));
        }
        return npc_list;

    }
    public static UUID get_all_NPC_uuid(Player player ,String npc_name){
        if (!player.getData(ALL_NPC).isEmpty()){
            for(CompoundTag a : player.getData(ALL_NPC) ){
                if(a.getString("name").equals(npc_name)){
                    return a.getUUID("uuid_name");
                }
            }
        }
        return null;
    }


    public void set_all_NPC(){

        this.setChanged();
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
       tag.putInt("NPC_count",this.NPC_count);
       tag.putInt("resident_building_count",this.resident_building_count);
       ListTag lt = new ListTag();
       for(Map.Entry<BlockPos,Double> et : GDP_manager.entrySet()){
           CompoundTag ct=new CompoundTag();
           ct.putLong("key",et.getKey().asLong());
           ct.putDouble("value",et.getValue());

           lt.add(ct);
       }
       tag.put("GDP_manager",lt);

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
        this.resident_building_count=tag.getInt("resident_building_count");
        this.NPC_count=tag.getInt("NPC_count");

        GDP_manager.clear();
        if(tag.contains("GDP_manager")){
            ListTag lt =tag.getList("GDP_manager", ListTag.TAG_COMPOUND);
            for(int i=0;i<lt.size();i++){
                CompoundTag ct =lt.getCompound(i);
                BlockPos key=BlockPos.of(ct.getLong("key"));
                Double value=ct.getDouble("value");
                GDP_manager.put(key,value);
            }
        }

    }

    public void add_NPC_count(int count){

        this.NPC_count+=count;
    }
    public void add_resident_building_count(){

        this.resident_building_count+=1;
    }
    /*
    升级镇中心用的
     */
    public void shengji(Player player){
        double jing_ji=player.getData(SimulationTown.PLAYER_JINGJI);

       if(this.level ==1 &&hasItem(player,Items.IRON_INGOT,64)){
           if (jing_ji >=10){
               xiaohao(player, Items.IRON_INGOT,64);
               this.level +=1;
               this.now_max_resident=10;
               player.setData(SimulationTown.PLAYER_JINGJI,jing_ji-10.0);
               this.setChanged();
           }


       }
        if(this.level ==2 &&hasItem(player,Items.GOLD_INGOT ,64)) {
            if (jing_ji >= 500) {
                xiaohao(player, Items.GOLD_INGOT, 64);
                this.level += 1;
                this.now_max_resident=30;
                player.setData(SimulationTown.PLAYER_JINGJI, jing_ji - 500.0);
                this.setChanged();
            }
        }
        if(this.level ==3 &&hasItem(player,Items.DIAMOND ,64)) {
            if (jing_ji >= 5000) {
                xiaohao(player, Items.DIAMOND, 64);
                this.level += 1;
                this.now_max_resident=70;
                player.setData(SimulationTown.PLAYER_JINGJI, jing_ji - 5000.0);
                this.setChanged();
            }


        }

        if(this.level ==4 &&hasItem(player,Items.NETHER_STAR ,1)) {
            if (jing_ji >= 20000) {
                xiaohao(player, Items.NETHER_STAR, 1);
                this.level += 1;
                this.now_max_resident=100;
                player.setData(SimulationTown.PLAYER_JINGJI, jing_ji -20000.0);
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
    public String qingli(Player player ){
        BlockPos a = player.getExistingDataOrNull(SimulationTown.TOWN_CENTER);
        if (a!=null && hasItem(player,Items.NETHER_STAR,1)){
            xiaohao(player,Items.NETHER_STAR,1);
            player.removeData(TOWN_CENTER);
            player.removeData(TOWN_CENTER_NAME);
            player.removeData(TOWN_GDP);
            player.removeData(PLAYER_JINGJI);
            return "已放弃小镇";
        }
        return "你没有下界之星，无法放弃小镇";

    }
    public void save_town_name(Player player,String n1){
        String n=player.getData(TOWN_CENTER_NAME);
        if (n==null ){
            if(n1!=null && !n1.isEmpty()){
                player.setData(TOWN_CENTER_NAME,n1);
            }
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
    public boolean name() {
        if (this.town_name != null) {
            if (this.town_name.isEmpty()) {
                return false;
            } else {
                return true;
            }

        }
        return false;
    }
    /*给启动资金的*/
    public String add_jingji(Player player){
        double p = player.getData(PLAYER_JINGJI);
        if (this.level==1){
            player.setData(PLAYER_JINGJI,p+10.0);
            return "已给你10块小镇启动资金";
        }
        return "";
    }
    /*给buildlib调用的*/
    public void add_jinji(Player player ,double jinji){
        double p1= player.getData(PLAYER_JINGJI);
        player.setData(PLAYER_JINGJI,p1+jinji);
    }
}
