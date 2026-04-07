package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MouseBeamEntity extends Entity {

    private static final int FULL_BEAM_LIFETIME = 2;
    private static final int IMPACT_DELAY_TICKS = 1;
    private static final int EXTRA_LIFETIME = 2;

    private static final EntityDataAccessor<Float> DATA_START_X = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_START_Y = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_START_Z = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_X = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Y = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Z = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TRAVEL_TICKS = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_IMPACT_TYPE = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BLOCK_X = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BLOCK_Y = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BLOCK_Z = SynchedEntityData.defineId(MouseBeamEntity.class, EntityDataSerializers.INT);

    private boolean impactResolved;

    public MouseBeamEntity(EntityType<? extends MouseBeamEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public MouseBeamEntity(Level level, Vec3 start, Vec3 end, int impactType, BlockPos blockPos) {
        this(ChenMod.MOUSE_BEAM.get(), level);
        this.setBeamData(start, end, impactType, blockPos);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_START_X, 0.0F);
        builder.define(DATA_START_Y, 0.0F);
        builder.define(DATA_START_Z, 0.0F);
        builder.define(DATA_END_X, 0.0F);
        builder.define(DATA_END_Y, 0.0F);
        builder.define(DATA_END_Z, 0.0F);
        builder.define(DATA_TRAVEL_TICKS, FULL_BEAM_LIFETIME);
        builder.define(DATA_IMPACT_TYPE, 0);
        builder.define(DATA_BLOCK_X, 0);
        builder.define(DATA_BLOCK_Y, 0);
        builder.define(DATA_BLOCK_Z, 0);
    }

    public void setBeamData(Vec3 start, Vec3 end, int impactType, BlockPos blockPos) {
        this.entityData.set(DATA_START_X, (float) start.x);
        this.entityData.set(DATA_START_Y, (float) start.y);
        this.entityData.set(DATA_START_Z, (float) start.z);
        this.entityData.set(DATA_END_X, (float) end.x);
        this.entityData.set(DATA_END_Y, (float) end.y);
        this.entityData.set(DATA_END_Z, (float) end.z);
        this.entityData.set(DATA_TRAVEL_TICKS, FULL_BEAM_LIFETIME);
        this.entityData.set(DATA_IMPACT_TYPE, impactType);
        if (blockPos != null) {
            this.entityData.set(DATA_BLOCK_X, blockPos.getX());
            this.entityData.set(DATA_BLOCK_Y, blockPos.getY());
            this.entityData.set(DATA_BLOCK_Z, blockPos.getZ());
        } else {
            this.entityData.set(DATA_BLOCK_X, 0);
            this.entityData.set(DATA_BLOCK_Y, 0);
            this.entityData.set(DATA_BLOCK_Z, 0);
        }
        this.refreshCenterPosition();
    }

    public Vec3 getStart() {
        return new Vec3(this.entityData.get(DATA_START_X), this.entityData.get(DATA_START_Y), this.entityData.get(DATA_START_Z));
    }

    public Vec3 getEnd() {
        return new Vec3(this.entityData.get(DATA_END_X), this.entityData.get(DATA_END_Y), this.entityData.get(DATA_END_Z));
    }

    public int getTravelTicks() {
        return Math.max(1, this.entityData.get(DATA_TRAVEL_TICKS));
    }

    public float getTravelProgress(float partialTicks) {
        return 1.0F;
    }

    public float getRetractProgress(float partialTicks) {
        return Mth.clamp(((this.tickCount + partialTicks) - this.getTravelTicks()) / EXTRA_LIFETIME, 0.0F, 1.0F);
    }

    public Vec3 getVisibleStart(float partialTicks) {
        return this.getStart().lerp(this.getEnd(), this.getRetractProgress(partialTicks));
    }

    public Vec3 getVisibleEnd(float partialTicks) {
        return this.getStart().lerp(this.getEnd(), this.getTravelProgress(partialTicks));
    }

    public int getImpactType() {
        return this.entityData.get(DATA_IMPACT_TYPE);
    }

    public BlockPos getImpactBlockPos() {
        return new BlockPos(this.entityData.get(DATA_BLOCK_X), this.entityData.get(DATA_BLOCK_Y), this.entityData.get(DATA_BLOCK_Z));
    }

    private void refreshCenterPosition() {
        Vec3 center = this.getStart().add(this.getEnd()).scale(0.5D);
        this.setPos(center.x, center.y, center.z);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && !this.impactResolved && this.tickCount >= IMPACT_DELAY_TICKS) {
            this.applyImpact((ServerLevel) this.level());
            this.impactResolved = true;
        }

        if (this.tickCount >= this.getTravelTicks() + EXTRA_LIFETIME) {
            this.discard();
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        this.refreshCenterPosition();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    private void applyImpact(ServerLevel level) {
        if (this.getImpactType() == 2) {
            BlockPos blockPos = this.getImpactBlockPos();
            BlockState state = level.getBlockState(blockPos);
            if (canAnimateBlock(level, blockPos, state) && level.destroyBlock(blockPos, false)) {
                LivingBlockEntity livingBlock = LivingBlockEntity.createFromBlock(level, blockPos, state);
                if (livingBlock != null && level.noCollision(livingBlock)) {
                    level.addFreshEntity(livingBlock);
                }
            }
        }

        if (this.getImpactType() != 0) {
            Vec3 impactPos = this.getEnd();
            level.sendParticles(ParticleTypes.END_ROD, impactPos.x, impactPos.y, impactPos.z, 10, 0.06D, 0.06D, 0.06D, 0.015D);
            level.sendParticles(ParticleTypes.END_ROD, impactPos.x, impactPos.y, impactPos.z, 6, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    private static boolean canAnimateBlock(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && !state.getCollisionShape(level, pos).isEmpty()
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }
}
