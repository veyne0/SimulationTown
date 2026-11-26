package com.simulationtown.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public interface BuildingData {
    String getEmployNpc();
    String getBuilderName();
    boolean isBuildFinished();
    double getCompletionLevel();

    // ---- 建筑绑定：这个方块属于哪张蓝图、占哪些方块 ----
    String getBuildingId();
    BoundingBox getBuildingBounds();
    void setBuildingId(String id);
    void setBuildingBounds(BoundingBox box);
    void setBuildFinished(boolean finished);
    void setCompletion(double value);

    void set_block_count(int i);

    // ---- NPC 重建需要的信息 (buildlib 读取) ----
    /** 建造时的旋转步数 0~3 */
    int getRotationSteps();
    /** 绑定后写回旋转量 (建造完成事件里调) */
    void setRotationSteps(int steps);
    /** 这个管理方块自己的坐标 (任务持久化后按坐标找回 BE) */
    BlockPos getBindPos();



    void settown(BlockPos bp);
}
