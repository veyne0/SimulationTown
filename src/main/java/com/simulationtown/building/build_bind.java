package com.simulationtown.building;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.Event;

public class build_bind extends Event {
    private final ServerPlayer player;
    private final ServerLevel level;
    private final BlockPos origin;
    private final int rotationSteps;
    private final int placedCount;
    private final int totalBlocks;
    private final String context;
    private final BoundingBox worldBounds;
    public Double GDP;

    public build_bind(ServerPlayer player, ServerLevel level, BlockPos origin, int rotationSteps, int placedCount, int totalBlocks, String context, BoundingBox worldBounds) {
        this.player = player;
        this.level = level;
        this.origin = origin;
        this.rotationSteps = rotationSteps;
        this.placedCount = placedCount;
        this.totalBlocks = totalBlocks;
        this.context = context == null ? "" : context;
        this.worldBounds = worldBounds;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public BlockPos getOrigin() {
        return this.origin;
    }

    public int getRotationSteps() {
        return this.rotationSteps;
    }

    public int getPlacedCount() {
        return this.placedCount;
    }

    public int getTotalBlocks() {
        return this.totalBlocks;
    }

    public String getContext() {
        return this.context;
    }

    public BoundingBox getWorldBounds() {
        return this.worldBounds;
    }

    public  Double set_GDP(){
        return this.GDP;

    }
}

