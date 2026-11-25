package com.simulationtown.AI;

import com.simulationtown.NPC.NPC;
import com.simulationtown.residence.residence_block_entity;
import com.simulationtown.towncenter.centerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;

/**
 * 下班回家：天黑 (!level.isDay()) 走到 NPC_home 旁边待着，天亮自动散班。
 *
 * <p>优先级 0 (压过砍树/捡东西)。NPC_home 是住宅或镇中心坐标；方块本身是实心的，
 * 导航目标直接给方块坐标，寻路会自然停在方块旁边，不用自己找站立点。</p>
 *
 * <p>家方块没了 (住宅被拆/镇中心重建导致坐标失效)：清掉 NPC_home，原地过夜，
 * 天亮后玩家可以用"分配住宅"重新安置。</p>
 */
public class go_home_goal extends Goal {
    private final NPC npc;
    /** 当晚的目标 (开班时从 NPC_home 快照; 发现方块没了会连 NPC_home 一起清) */
    private BlockPos home;
    /** 寻路走完了但人还没到 → 隔 N tick 重试导航 */
    private int repathCooldown;
    /** 是否已经到家 (到了就站桩, 不再动) */
    private boolean arrived;

    public go_home_goal(NPC npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !npc.level().isDay() && npc.NPC_home != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !npc.level().isDay() && npc.NPC_home != null;   // 天亮 goal 结束, 白天的 goal 顶上来
    }

    @Override
    public void start() {
        home = npc.NPC_home;
        arrived = false;
        repathCooldown = 0;
        tryMove();
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
        home = null;
        arrived = false;
    }

    @Override
    public void tick() {
        if (home == null) return;

        // 每 60 tick 校验一次家方块还在不在 (住宅/镇中心两种都算有效)
        if (npc.tickCount % 60 == 0) {
            BlockEntity be = npc.level().getBlockEntity(home);
            if (!(be instanceof residence_block_entity) && !(be instanceof centerBlockEntity)) {
                npc.NPC_home = null;   // 坐标失效, 清掉; 站原地过夜
                home = null;
                return;
            }
        }

        double tx = home.getX() + 0.5, ty = home.getY() + 0.5, tz = home.getZ() + 0.5;

        if (!arrived && npc.blockPosition().closerThan(home, 2.5D)) {
            arrived = true;
            npc.getNavigation().stop();   // 到家, 停下站桩
        }

        if (arrived) {
            // 站桩: 偶尔换个视角, 别一直僵着盯一个点
            if (npc.tickCount % 80 == 0 && npc.getRandom().nextInt(3) == 0) {
                npc.getLookControl().setLookAt(
                        tx + (npc.getRandom().nextDouble() - 0.5) * 4,
                        ty + 1.2,
                        tz + (npc.getRandom().nextDouble() - 0.5) * 4);
            }
            return;
        }

        npc.getLookControl().setLookAt(tx, ty, tz);

        // 路没走完就让它走; 寻路结束但人没到 (失败/被打断) → 隔 40 tick 重试
        if (npc.getNavigation().isDone()) {
            if (--repathCooldown <= 0) {
                repathCooldown = 40;
                tryMove();
            }
        }
    }

    private void tryMove() {
        npc.getNavigation().moveTo(
                home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0D);
    }
}
