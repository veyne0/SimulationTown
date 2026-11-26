package com.simulationtown.residence;

import com.simulationtown.block;
import com.simulationtown.building.BuildingData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.UUID;

public class residence_block_entity extends BlockEntity implements BuildingData {
    public String r_building_id = "";
    int[] r_building_box ;
    public double r_GDP;
    public int r_block_count;
    public UUID r_builder_UUID;
    public BlockPos r_pos;
    public String resident_name;

    public BlockPos r_town_pos;

    /** 建造时的旋转步数 (0~3), NPC 重建/重建预览用 */
    public int r_rotation_steps = 0;

    public String employ_npc = "";
    public String builder_name = "";
    public boolean i_build_state = false;
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
        tag.putInt("r_block_count",this.r_block_count);
        tag.putDouble("r_GDP",this.r_GDP);
        tag.putInt("r_rotation_steps",this.r_rotation_steps);
        if  (this.r_building_box!=null){
            tag.putIntArray("r_building_box",this.r_building_box);
        }
        if (this.r_building_id!=null){
            tag.putString("r_building_id",this.r_building_id);
        }
        if(r_builder_UUID!=null){
            tag.putUUID("r_builder_UUID",this.r_builder_UUID);
        }
        if(r_pos !=null ){
            tag.putLong("r_pos",r_pos.asLong());
        }
        if(this.resident_name !=null){
            tag.putString("resident_name",this.resident_name);
        }
        if(this.r_town_pos!=null){
            tag.putLong("r_town_pos",this.r_town_pos.asLong());
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
        this.r_block_count=tag.getInt("r_block_count");
        this.r_GDP=tag.getDouble("r_GDP");
        this.r_rotation_steps=tag.getInt("r_rotation_steps");
        if(tag.hasUUID("r_builder_UUID")){
            this.r_builder_UUID=tag.getUUID("r_builder_UUID");
        }
        if(tag.contains("r_pos")){
            this.r_pos=BlockPos.of(tag.getLong("r_pos"));
        }
        if(tag.contains("resident_name")){
            this.resident_name=tag.getString("resident_name");
        }
        if(tag.contains("r_town_pos")){
            this.r_town_pos=BlockPos.of(tag.getLong("r_town_pos"));
        }

    }
    public UUID r_get_builder_UUID(){
        return this.r_builder_UUID;
    }
    public void r_sava_builder_uuid(UUID u3){
        if(u3!=null){
            this.r_builder_UUID=u3;
            this.setChanged();
        }
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

    public int get_r_block_count(){

        return this.r_block_count;
    }
    public void sava_r_block_count(){
        if (this.r_block_count !=0){
            this.setChanged();
        }
    }

    public double get_r_GDP(){
        return this.r_GDP;
    }
    public void sava_r_GDP(double r){
        this.r_GDP=r;
        this.setChanged();

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

    @Override
    public int getRotationSteps() {
        return this.r_rotation_steps;
    }

    @Override
    public void setRotationSteps(int steps) {
        this.r_rotation_steps = ((steps % 4) + 4) % 4;
        this.setChanged();
    }

    @Override
    public BlockPos getBindPos() {
        this.r_pos=this.getBlockPos();
        return  r_pos;
    }
    public String get_resident_name(){
        if(this.resident_name !=null && !this.resident_name.isEmpty()){
            return this.resident_name;
        }
        return "";
    }
    public void set_resident_name(String n){
        if(!n.isEmpty()){
            this.resident_name=n;
        }
    }
    public void qingli_resident_name(){
        this.resident_name="";
    }
    public void set_block_count(int i){
        this.r_block_count=i;
        this.setChanged();

    }
    @Override
    public void settown(BlockPos bp){
        this.r_town_pos=bp;

    }

}
