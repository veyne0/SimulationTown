package com.simulationtown.AI;

import com.simulationtown.NPC.NPC;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

public class pick_thing extends Goal {
    public NPC npc;
    public ItemEntity targetItem;

    public pick_thing(NPC npc) {
        this.npc = npc;
    }

    @Override
    public boolean canUse() {
        // 找周围 3格内的物品
        List<ItemEntity> items = npc.level().getEntitiesOfClass(ItemEntity.class, npc.getBoundingBox().inflate(3.0D));
        if (items.isEmpty()) return false;

        targetItem = items.get(0);
        return true;
    }

    @Override
    public void tick() {
        if (targetItem != null && targetItem.isAlive()) {
            // 寻路走向物品
            npc.getNavigation().moveTo(targetItem, 1.0D);

            // 如果距离足够近（小于1.5格），触发捡起逻辑
            if (npc.distanceToSqr(targetItem) < 2.25D) {
                npc.pickUpItem(targetItem); // 调用你上面重写的方法
            }
        }
    }
}
