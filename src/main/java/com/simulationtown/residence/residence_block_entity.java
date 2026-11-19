package com.simulationtown.residence;

import com.simulationtown.block;
import com.simulationtown.building.BuildingData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class residence_block_entity extends BlockEntity implements BuildingData {
    public String r_building_id = "";
    int[] r_building_box ;

    /** 雇佣的 NPC 名字 */
    public String employ_npc = "";
    /** 建造这个建筑的 NPC 名字 */
    public String builder_name = "";
    /** 是否建造完成 */
    public boolean i_build_state = false;
    /** 完成度 0.0 ~ 1.0 */
    public double i_Completion_level = 0.0;

    public residence_block_entity(BlockPos pos, BlockState state) {
        super(block.RBE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("employ_npc", this.employ_npc);
        tag.putString("builder_name", this.builder_name);
        tag.putBoolean("build_state", this.i_build_state);
        tag.putDouble("completion", this.i_Completion_level);
        if  (this.r_building_box!=null){
            tag.putIntArray("r_building_box",this.r_building_box);
        }
        if (this.r_building_id!=null){
            tag.putString("r_building_id",this.r_building_id);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.employ_npc = tag.getString("employ_npc");
        this.builder_name = tag.getString("builder_name");
        this.i_build_state = tag.getBoolean("build_state");
        this.i_Completion_level = tag.getDouble("completion");
        this.r_building_box=tag.getIntArray("r_building_box");
        this.r_building_id=tag.getString("r_building_id");
    }

    public void setBuildingBounds(BoundingBox box) {
        if (box == null) {
            this.r_building_box = null;
            this.setChanged();
        } else {
            this.r_building_box = new int[]{
                    box.minX(), box.minY(), box.minZ(),
                    box.maxX(), box.maxY(), box.maxZ()};
            this.setChanged();
        }

    }
    public BoundingBox getBuildingBounds() {
        if (r_building_box == null || r_building_box.length != 6) return null;
        return new BoundingBox(r_building_box[0], r_building_box[1], r_building_box[2],
                r_building_box[3], r_building_box[4], r_building_box[5]);
    }

    public void setBuildingId(String id) {
        this.r_building_id = id == null ? "" : id;
        this.setChanged();
    }

    public String getBuildingId() {
        return this.r_building_id;
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
