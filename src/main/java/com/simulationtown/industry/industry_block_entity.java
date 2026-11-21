package com.simulationtown.industry;

import com.simulationtown.block;
import com.simulationtown.building.BuildingData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.UUID;

public class industry_block_entity extends BlockEntity  {
    public String i_building_id = "";
    int[] i_building_box ;
    public int i_GDP;
    public int i_block_count;
    public UUID i_builder_UUID;
    /** 建造时的旋转步数 (0~3), NPC 重建/重建预览用 */
    public int i_rotation_steps = 0;

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
        tag.putInt("i_GDP",this.i_GDP);
        tag.putInt("i_block_count",this.i_block_count);
        tag.putInt("i_rotation_steps",this.i_rotation_steps);
        if  (this.i_building_box!=null){
            tag.putIntArray("i_building_box",this.i_building_box);
        }
        if (this.i_building_id!=null){
            tag.putString("i_building_id",this.i_building_id);
        }
        tag.putUUID("i_builder_UUID",this.i_builder_UUID);

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
        this.i_GDP=tag.getInt("i_GDP");
        this.i_block_count=tag.getInt("i_block_count");   // 原来误写成 "i_block_acount", 键名对不上永远读到 0
        this.i_rotation_steps=tag.getInt("i_rotation_steps");
        if (tag.hasUUID("i_builder_UUID")) {
            this.i_builder_UUID=tag.getUUID("i_builder_UUID");   // 旧存档没有此键时保持 null, 不炸
        }
    }
    public UUID i_get_builder_UUID(){
        return this.i_builder_UUID;
    }
    public void i_sava_builder_uuid(UUID u2){
        if(u2!=null){
            this.i_builder_UUID=u2;
            this.setChanged();
        }
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

    public int get_i_GDP(){
        return this.i_GDP;
    }
    public void sava_i_GDP(){
        if (this.i_GDP !=0){
            this.setChanged();
        }
    }
    public int get_i_block_count(){
        return this.i_block_count;
    }
    public void sava_i_block_count(){
        if (this.i_block_count !=0){
            this.setChanged();
        }
    }


    public String getEmployNpc() {
        return this.employ_npc;
    }


    public String getBuilderName() {
        return this.builder_name;
    }


    public boolean isBuildFinished() {
        return this.i_build_state;
    }


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
