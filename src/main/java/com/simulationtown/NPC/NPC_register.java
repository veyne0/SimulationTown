package com.simulationtown.NPC;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class NPC_register {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, "simulation_town");

    public static final Supplier<EntityType<NPC>> NPC =
            ENTITIES.register("npc", () -> EntityType.Builder
                    .of(NPC::new, MobCategory.MISC)      // MISC: 不自然生成
                    .sized(0.6f, 1.8f)                          // 碰撞箱 = 玩家大小
                    .clientTrackingRange(10)
                    .build("_npc"));
}
