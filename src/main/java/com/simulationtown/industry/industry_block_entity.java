package com.simulationtown.industry;

import com.simulationtown.block;
import com.simulationtown.building.BuildingData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class industry_block_entity extends BlockEntity implements BuildingData {
    public String i_building_id = "";
    int[] i_building_box ;

    /** 雇佣的 NPC 名字 */
    public String employ_npc = "";
    /** 建造这个建筑的 NPC 名字 */
    public String builder_name = "";
    /** 是否建造完成 */
    public boolean i_build_state = false;
    /** 完成度 0.0 ~ 1.0 */
    public double i_Completion_level = 0.0;

    public industry_block_entity(BlockPos pos, BlockState state) {
        super(block.IBE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("employ_npc", this.employ_npc);
        tag.putString("builder_name", this.builder_name);
        tag.putBoolean("build_state", this.i_build_state);
        tag.putDouble("completion", this.i_Completion_level);
        if  (this.i_building_box!=null){
            tag.putIntArray("i_building_box",this.i_building_box);
        }
        if (this.i_building_id!=null){
            tag.putString("i_building_id",this.i_building_id);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.employ_npc = tag.getString("employ_npc");
        this.builder_name = tag.getString("builder_name");
        this.i_build_state = tag.getBoolean("build_state");
        this.i_Completion_level = tag.getDouble("completion");
        this.i_building_box=tag.getIntArray("i_building_box");
        this.i_building_id=tag.getString("i_building_id");
    }

    public void setBuildingBounds(BoundingBox box) {
        if (box == null) {
            this.i_building_box = null;
            this.setChanged();
        } else {
            this.i_building_box = new int[]{
                    box.minX(), box.minY(), box.minZ(),
                    box.maxX(), box.maxY(), box.maxZ()};
            this.setChanged();
        }

    }
    public BoundingBox getBuildingBounds() {
        if (i_building_box == null || i_building_box.length != 6) return null;
        return new BoundingBox(i_building_box[0], i_building_box[1], i_building_box[2],
                i_building_box[3], i_building_box[4], i_building_box[5]);
    }

    public void setBuildingId(String id) {
        this.i_building_id = id == null ? "" : id;
        this.setChanged();
    }

    public String getBuildingId() {
        return this.i_building_id;
    }

    @Override
    public String getEmployNpc() {
        return this.employ_npc;
    }

    @Override
    public String getBuilderName() {
        return this.builder_name;
    }

    @Override
    public boolean isBuildFinished() {
        return this.i_build_state;
    }

    @Override
    public double getCompletionLevel() {
        return this.i_Completion_level;
    }

    // ---- 给以后写建造逻辑用的写入方法 ----
    public void setEmployNpc(String name) {
        this.employ_npc = name == null ? "" : name;
        this.setChanged();
    }

    public void setBuilderName(String name) {
        this.builder_name = name == null ? "" : name;
        this.setChanged();
    }

    public void setCompletion(double value) {
        this.i_Completion_level = Math.max(0.0, Math.min(1.0, value));
        this.setChanged();
    }

    public void setBuildFinished(boolean finished) {
        this.i_build_state = finished;
        this.setChanged();
    }
}
