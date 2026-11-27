package com.simulationtown.NPC;

import com.simulationtown.AI.go_home_goal;
import com.simulationtown.AI.kanshu_goal;
import com.simulationtown.AI.pick_thing;
import com.simulationtown.GUI.npc_GUI;

import com.simulationtown.residence.residence_block_entity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;

import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import static com.simulationtown.SimulationTown.TOWN_CENTER;


public class NPC extends PathfinderMob {
    public  SimpleContainer inventory = new SimpleContainer(27);
    public String job;
    public String npc_state;
    /** SHOPER 专用: 所属商店建筑的 buildingId (如 "special/杂货铺"), 商品从这里读; 非商店为 null  */
    public String shopBuildingId;
    public int skinIndex = (int)(Math.random()*10);
    public BlockPos NPC_home;
    public BlockPos guanliblock_pos;


    public NPC(EntityType<? extends PathfinderMob> type, Level level) {

        super(type, level);
        this.setCanPickUpLoot(true);

    }
    private static final EntityDataAccessor<Integer> DATA_SKIN =
            SynchedEntityData.defineId(NPC.class, EntityDataSerializers.INT);

    /** job 同步到客户端 (npc_GUI 靠它决定"建造材料"按钮出不出来, 结构两边必须一致) */
    private static final EntityDataAccessor<String> DATA_JOB =
            SynchedEntityData.defineId(NPC.class, EntityDataSerializers.STRING);

    public static  final EntityDataAccessor<String> NPC_STATE=SynchedEntityData.defineId(NPC.class,EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, 0);
        builder.define(DATA_JOB, "");
        builder.define(NPC_STATE,"");
    }

    /** 设置皮肤并同步到客户端 (普通字段客户端拿不到, 必须走 entityData) */
    public void setSkinIndex(int i) {
        this.skinIndex = i;
        this.entityData.set(DATA_SKIN, i);
    }
    public SimpleContainer getNpcInventory() {
        return inventory;
    }
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }
    @Override
    public void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(2,new pick_thing(this));
        this.goalSelector.addGoal(1,new kanshu_goal(this));
        this.goalSelector.addGoal(0, new go_home_goal(this));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1.0D));
    }
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("NpcInventory", this.inventory.createTag(this.registryAccess()));// ListTag, 含槽位号
        if(this.job!=null){
            tag.putString("job",this.job);
        }
        if(this.shopBuildingId!=null){
            tag.putString("shopid",this.shopBuildingId);
        }
        tag.putInt("skinindex",this.skinIndex);
        if(this.npc_state !=null){
            tag.putString("npc_state",this.npc_state);
        }
        if(this.NPC_home!=null){
            tag.putLong("NPC_home",this.NPC_home.asLong());
        }
        if(this.guanliblock_pos!=null){
            tag.putLong("guanli",this.guanliblock_pos.asLong());
        }

    }


    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcInventory", 9)) {
            this.inventory.fromTag(tag.getList("NpcInventory", 10),this.registryAccess());
        }
        this.job=tag.getString("job");
        this.shopBuildingId=tag.contains("shopid")?tag.getString("shopid"):null;
        this.skinIndex=tag.getInt("skinindex");
        this.entityData.set(DATA_SKIN, this.skinIndex);   // 读档也要进 watcher, 客户端才知道
        this.entityData.set(DATA_JOB, this.job == null ? "" : this.job);
        if(this.npc_state !=null){
            this.entityData.set(NPC_STATE,this.npc_state);
        }
        if(tag.contains("NPC_home")){
            this.NPC_home=BlockPos.of(tag.getLong("NPC_home"));

        }
        if(tag.contains("guanli")){
            this.guanliblock_pos=BlockPos.of(tag.getLong("guanli"));
        }
    }

    public int getSkinIndex() {
        return this.entityData.get(DATA_SKIN);
    }

    /** 客户端也能读的职业 (走 watcher 同步; 客户端构建界面时判断 Builder 用) */
    public String getJobSynced() {
        String j = this.entityData.get(DATA_JOB);
        return j == null ? "" : j;
    }
    public void set_NPC_state(){
        if(getJobSynced() !=null &&getJobSynced().isEmpty()){
            this.entityData.set(NPC_STATE,"空闲");
        }
    }

    public void openInventoryFor(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, playerInv, p) -> ChestMenu.threeRows(id, playerInv, this.inventory),
                this.getName()));
    }

    public void set_job(String n){
        this.job=n;
        this.entityData.set(DATA_JOB, n == null ? "" : n);   // 同步到客户端
    }

    public void unemploy_NPC(){
        this.job="";
        this.entityData.set(DATA_JOB, "");
        this.entityData.set(NPC_STATE,"空闲");
        this.guanliblock_pos=null;
        this.shopBuildingId="";
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return npc_GUI.onInteract(this, player, hand);
    }

    @Override
    public  void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemStack = itemEntity.getItem();


        ItemStack remaining = this.inventory.addItem(itemStack);

        // 如果还有剩余的（说明背包满了装不下）
        if (remaining.isEmpty()) {
            // 全部装进去了！
            itemEntity.discard(); // 销毁地上的物品实体
            this.take(itemEntity, itemStack.getCount()); // 播放原版捡东西的音效和动画
        } else {

            itemEntity.setItem(remaining);
        }
    }

    public void set_NPC_home(BlockPos pos ){
        Level l = this.level();
        if(!l.isClientSide()){
            BlockEntity be = l.getBlockEntity(pos);
            if(be instanceof residence_block_entity){
                this.NPC_home=((residence_block_entity) be).getBindPos();

            }
        }

    }
    public void qingli_NPC_home(Player player) {
        BlockPos po =player.getData(TOWN_CENTER);
        if(po !=null){
            this.NPC_home=po;
        }else{
            this.NPC_home = null;
        }
    }

    /*这个应该在雇佣时将NPC绑定管理方块，然后再把NPC的工作范围设置为管理方法里设置的范围，AI写GUI时要先调用这个方法绑定*/
    public void set_guanliblock_pos(BlockPos pos){
        this.guanliblock_pos=pos;
    }
    public void qingli_guanliblock_pos(){
        this.guanliblock_pos=null;
    }


}

