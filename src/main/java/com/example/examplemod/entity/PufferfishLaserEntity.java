package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class PufferfishLaserEntity extends Entity {

    private static final int FULL_BEAM_LIFETIME = 1;
    private static final int SHRINK_LIFETIME = 8;
    private static final Vector3f TRAIL_COLOR = new Vector3f(0.30F, 1.00F, 0.45F);

    private static final EntityDataAccessor<Float> DATA_START_X = SynchedEntityData.defineId(PufferfishLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_START_Y = SynchedEntityData.defineId(PufferfishLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_START_Z = SynchedEntityData.defineId(PufferfishLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_X = SynchedEntityData.defineId(PufferfishLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Y = SynchedEntityData.defineId(PufferfishLaserEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_END_Z = SynchedEntityData.defineId(PufferfishLaserEntity.class, EntityDataSerializers.FLOAT);

    public PufferfishLaserEntity(EntityType<? extends PufferfishLaserEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.noCulling = true;
    }

    public PufferfishLaserEntity(Level level, Vec3 start, Vec3 end) {
        this(ChenMod.PUFFERFISH_LASER.get(), level);
        this.setBeamData(start, end);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_START_X, 0.0F);
        builder.define(DATA_START_Y, 0.0F);
        builder.define(DATA_START_Z, 0.0F);
        builder.define(DATA_END_X, 0.0F);
        builder.define(DATA_END_Y, 0.0F);
        builder.define(DATA_END_Z, 0.0F);
    }

    public void setBeamData(Vec3 start, Vec3 end) {
        this.entityData.set(DATA_START_X, (float) start.x);
        this.entityData.set(DATA_START_Y, (float) start.y);
        this.entityData.set(DATA_START_Z, (float) start.z);
        this.entityData.set(DATA_END_X, (float) end.x);
        this.entityData.set(DATA_END_Y, (float) end.y);
        this.entityData.set(DATA_END_Z, (float) end.z);
        this.refreshCenterPosition();
    }

    public Vec3 getStart() {
        return new Vec3(this.entityData.get(DATA_START_X), this.entityData.get(DATA_START_Y), this.entityData.get(DATA_START_Z));
    }

    public Vec3 getEnd() {
        return new Vec3(this.entityData.get(DATA_END_X), this.entityData.get(DATA_END_Y), this.entityData.get(DATA_END_Z));
    }

    public float getWidthScale(float partialTicks) {
        return 1.0F - Mth.clamp(((this.tickCount + partialTicks) - FULL_BEAM_LIFETIME) / SHRINK_LIFETIME, 0.0F, 1.0F);
    }

    public Vec3 getVisibleStart(float partialTicks) {
        return this.getStart();
    }

    public Vec3 getVisibleEnd(float partialTicks) {
        return this.getEnd();
    }

    private void refreshCenterPosition() {
        Vec3 center = this.getStart().add(this.getEnd()).scale(0.5D);
        this.setPos(center.x, center.y, center.z);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.spawnTrailParticles();
        }
        if (this.tickCount >= FULL_BEAM_LIFETIME + SHRINK_LIFETIME) {
            this.discard();
        }
    }

    private void spawnTrailParticles() {
        Vec3 start = this.getStart();
        Vec3 end = this.getEnd();
        Vec3 diff = end.subtract(start);
        double length = diff.length();
        if (length < 1.0E-5D) {
            return;
        }

        Vec3 direction = diff.scale(1.0D / length);
        int samples = Math.max(10, Mth.ceil(length / 0.9D));
        float widthScale = this.getWidthScale(0.0F);
        float particleSize = 0.38F * Math.max(0.30F, widthScale);
        DustParticleOptions greenSpark = new DustParticleOptions(TRAIL_COLOR, particleSize);
        double jitter = 0.02D + (0.05D * widthScale);
        double sparkSpeed = 0.01D + (0.05D * widthScale);

        for (int i = 0; i <= samples; i++) {
            if (this.random.nextFloat() > 0.82F) {
                continue;
            }

            double progress = (double) i / samples;
            Vec3 pos = start.add(direction.scale(length * progress));
            double offsetX = (this.random.nextDouble() - 0.5D) * jitter;
            double offsetY = (this.random.nextDouble() - 0.5D) * jitter;
            double offsetZ = (this.random.nextDouble() - 0.5D) * jitter;
            double speedX = (this.random.nextDouble() - 0.5D) * sparkSpeed;
            double speedY = (this.random.nextDouble() - 0.5D) * sparkSpeed;
            double speedZ = (this.random.nextDouble() - 0.5D) * sparkSpeed;

            if (this.random.nextFloat() < 0.65F) {
                this.level().addParticle(greenSpark, pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, speedX * 0.4D, speedY * 0.4D, speedZ * 0.4D);
            }
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
