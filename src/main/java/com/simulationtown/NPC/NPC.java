package com.simulationtown.NPC;

import com.simulationtown.GUI.npc_GUI;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class NPC extends PathfinderMob {
    public  SimpleContainer inventory = new SimpleContainer(27);
    public String job;
    /** SHOPER 专用: 所属商店建筑的 buildingId (如 "special/杂货铺"), 商品从这里读; 非商店为 null  注：此字段是AI加的*/
    public String shopBuildingId;
    public int skinIndex = (int)(Math.random()*10);


    public NPC(EntityType<? extends PathfinderMob> type, Level level) {

        super(type, level);
    }
    private static final EntityDataAccessor<Integer> DATA_SKIN =
            SynchedEntityData.defineId(NPC.class, EntityDataSerializers.INT);
    /** job 同步到客户端 (npc_GUI 靠它决定"建造材料"按钮出不出来, 结构两边必须一致) */
    private static final EntityDataAccessor<String> DATA_JOB =
            SynchedEntityData.defineId(NPC.class, EntityDataSerializers.STRING);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, 0);   // 默认第 0 套皮肤; 真值由 spawn/读档写入
        builder.define(DATA_JOB, "");   // 默认无职业; 真值由 set_job/读档写入
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
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }
    @Override
    protected void registerGoals() {

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
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
    }

    public int getSkinIndex() {
        return this.entityData.get(DATA_SKIN);
    }

    /** 客户端也能读的职业 (走 watcher 同步; 客户端构建界面时判断 Builder 用) */
    public String getJobSynced() {
        String j = this.entityData.get(DATA_JOB);
        return j == null ? "" : j;
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
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return npc_GUI.onInteract(this, player, hand);
    }
}
