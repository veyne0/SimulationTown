package com.simulationtown.building;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** 所有"建筑本体"方块实体的公共读取接口，UI 只认这个，不关心是哪个分类。 */
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
}
