package com.simulationtown.NPC;

import com.simulationtown.GUI.npc_GUI;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.nbt.CompoundTag;
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
    public int skinIndex = 0;


    public NPC(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }
    public SimpleContainer getNpcInventory() {
        return inventory;
    }
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }
    @Override
    protected void registerGoals() {
        // 暂时只让它"站着看玩家"——建造逻辑下一阶段接
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
        tag.putInt("skinindex",this.skinIndex);

    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcInventory", 9)) {                      // 9 = TAG_LIST
            this.inventory.fromTag(tag.getList("NpcInventory", 10),this.registryAccess());
        }
        this.job=tag.getString("job");
        this.skinIndex=tag.getInt("skinindex");
    }

    public int getSkinIndex() {
        return this.skinIndex;
    }

    public void openInventoryFor(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, playerInv, p) -> ChestMenu.threeRows(id, playerInv, this.inventory),
                this.getName()));
    }

    public void set_job(String n){
        this.job=n;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return npc_GUI.onInteract(this, player, hand);
    }
}
