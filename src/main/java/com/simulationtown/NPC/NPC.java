package com.simulationtown.NPC;

import net.minecraft.nbt.CompoundTag;
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

public class NPC extends PathfinderMob {
    public  SimpleContainer inventory = new SimpleContainer(27);
    public String job;


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
        tag.put("NpcInventory", this.inventory.createTag(this.registryAccess()));        // ListTag, 含槽位号
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcInventory", 9)) {                      // 9 = TAG_LIST
            this.inventory.fromTag(tag.getList("NpcInventory", 10),this.registryAccess());
        }
    }

    public void openInventoryFor(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, playerInv, p) -> ChestMenu.threeRows(id, playerInv, this.inventory),
                this.getName()));
    }

}
