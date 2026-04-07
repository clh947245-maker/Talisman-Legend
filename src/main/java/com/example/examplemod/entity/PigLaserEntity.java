package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PigLaserEntity extends Entity {

    private static final int FULL_BEAM_LIFETIME = 2;
    private static final int RETRACT_LIFETIME = 2;

    private static final EntityDataAccessor<Float> DATA_LEFT_START_X = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LEFT_START_Y = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LEFT_START_Z = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LEFT_END_X = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LEFT_END_Y = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LEFT_END_Z = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIGHT_START_X = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIGHT_START_Y = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIGHT_START_Z = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIGHT_END_X = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIGHT_END_Y = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIGHT_END_Z = SynchedEntityData.defineId(PigLaserEntity.class, EntityDataSerializers.FLOAT);

    public PigLaserEntity(EntityType<? extends PigLaserEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public PigLaserEntity(Level level, Vec3 leftStart, Vec3 leftEnd, Vec3 rightStart, Vec3 rightEnd) {
        this(ChenMod.PIG_LASER.get(), level);
        this.setBeamData(leftStart, leftEnd, rightStart, rightEnd);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LEFT_START_X, 0.0F);
        builder.define(DATA_LEFT_START_Y, 0.0F);
        builder.define(DATA_LEFT_START_Z, 0.0F);
        builder.define(DATA_LEFT_END_X, 0.0F);
        builder.define(DATA_LEFT_END_Y, 0.0F);
        builder.define(DATA_LEFT_END_Z, 0.0F);
        builder.define(DATA_RIGHT_START_X, 0.0F);
        builder.define(DATA_RIGHT_START_Y, 0.0F);
        builder.define(DATA_RIGHT_START_Z, 0.0F);
        builder.define(DATA_RIGHT_END_X, 0.0F);
        builder.define(DATA_RIGHT_END_Y, 0.0F);
        builder.define(DATA_RIGHT_END_Z, 0.0F);
    }

    public void setBeamData(Vec3 leftStart, Vec3 leftEnd, Vec3 rightStart, Vec3 rightEnd) {
        this.entityData.set(DATA_LEFT_START_X, (float) leftStart.x);
        this.entityData.set(DATA_LEFT_START_Y, (float) leftStart.y);
        this.entityData.set(DATA_LEFT_START_Z, (float) leftStart.z);
        this.entityData.set(DATA_LEFT_END_X, (float) leftEnd.x);
        this.entityData.set(DATA_LEFT_END_Y, (float) leftEnd.y);
        this.entityData.set(DATA_LEFT_END_Z, (float) leftEnd.z);
        this.entityData.set(DATA_RIGHT_START_X, (float) rightStart.x);
        this.entityData.set(DATA_RIGHT_START_Y, (float) rightStart.y);
        this.entityData.set(DATA_RIGHT_START_Z, (float) rightStart.z);
        this.entityData.set(DATA_RIGHT_END_X, (float) rightEnd.x);
        this.entityData.set(DATA_RIGHT_END_Y, (float) rightEnd.y);
        this.entityData.set(DATA_RIGHT_END_Z, (float) rightEnd.z);
        this.refreshCenterPosition();
    }

    public Vec3 getLeftStart() {
        return new Vec3(this.entityData.get(DATA_LEFT_START_X), this.entityData.get(DATA_LEFT_START_Y), this.entityData.get(DATA_LEFT_START_Z));
    }

    public Vec3 getLeftEnd() {
        return new Vec3(this.entityData.get(DATA_LEFT_END_X), this.entityData.get(DATA_LEFT_END_Y), this.entityData.get(DATA_LEFT_END_Z));
    }

    public float getRetractProgress(float partialTicks) {
        return Mth.clamp(((this.tickCount + partialTicks) - FULL_BEAM_LIFETIME) / RETRACT_LIFETIME, 0.0F, 1.0F);
    }

    public Vec3 getVisibleLeftStart(float partialTicks) {
        return this.getLeftStart().lerp(this.getLeftEnd(), this.getRetractProgress(partialTicks));
    }

    public Vec3 getVisibleLeftEnd(float partialTicks) {
        return this.getLeftEnd();
    }

    public Vec3 getRightStart() {
        return new Vec3(this.entityData.get(DATA_RIGHT_START_X), this.entityData.get(DATA_RIGHT_START_Y), this.entityData.get(DATA_RIGHT_START_Z));
    }

    public Vec3 getRightEnd() {
        return new Vec3(this.entityData.get(DATA_RIGHT_END_X), this.entityData.get(DATA_RIGHT_END_Y), this.entityData.get(DATA_RIGHT_END_Z));
    }

    public Vec3 getVisibleRightStart(float partialTicks) {
        return this.getRightStart().lerp(this.getRightEnd(), this.getRetractProgress(partialTicks));
    }

    public Vec3 getVisibleRightEnd(float partialTicks) {
        return this.getRightEnd();
    }

    private void refreshCenterPosition() {
        Vec3 center = this.getLeftStart()
                .add(this.getLeftEnd())
                .add(this.getRightStart())
                .add(this.getRightEnd())
                .scale(0.25D);
        this.setPos(center.x, center.y, center.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount >= FULL_BEAM_LIFETIME + RETRACT_LIFETIME) {
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
}
