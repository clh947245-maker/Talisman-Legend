package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

public class PigLaserEntity extends AbstractArrow {

    public PigLaserEntity(EntityType<? extends PigLaserEntity> type, Level level) {
        super(type, level);
    }

    public PigLaserEntity(Level level, LivingEntity shooter, double startX, double startY, double startZ, double accelX, double accelY, double accelZ) {
        super(ChenMod.PIG_LASER.get(), level);
        this.setOwner(shooter);
        this.setPos(startX, startY, startZ);
        this.setDeltaMovement(accelX, accelY, accelZ);
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.END_ROD,
                    this.getX() + (random.nextDouble() - 0.5) * 0.15,
                    this.getY() + (random.nextDouble() - 0.5) * 0.15,
                    this.getZ() + (random.nextDouble() - 0.5) * 0.15,
                    0, 0, 0);
        }
        if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide) {
            Entity entity = result.getEntity();
            entity.hurt(this.damageSources().thrown(this, this.getOwner()), 8.0F);
            this.discard();
        }
    }

    @Override
    public boolean isCritArrow() {
        return true;
    }

    @Override
    public AbstractArrow.Pickup getDefaultPickup() {
        return AbstractArrow.Pickup.DISALLOWED;
    }
}
