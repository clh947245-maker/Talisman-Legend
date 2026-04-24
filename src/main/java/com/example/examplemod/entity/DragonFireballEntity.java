package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

public class DragonFireballEntity extends LargeFireball {

    private static final EntityDataAccessor<Integer> DATA_OWNER_ENTITY_ID = SynchedEntityData.defineId(DragonFireballEntity.class, EntityDataSerializers.INT);

    public DragonFireballEntity(EntityType<? extends LargeFireball> type, Level level) {
        super(type, level);
    }

    public DragonFireballEntity(Level level, LivingEntity shooter, double accelX, double accelY, double accelZ) {
        super(ChenMod.DRAGON_FIREBALL.get(), level);
        this.setOwner(shooter);
        // 设置初始位置
        this.setPos(shooter.getX(), shooter.getEyeY(), shooter.getZ());
        
        // 使用 setDeltaMovement 设置初始速度
        // 在 1.21+ 中，xPower/yPower/zPower 可能需要通过 accelerationPower 或者 assignPower 方法设置
        // 或者直接通过 setDeltaMovement 设置速度
        
        // 尝试设置 DeltaMovement (速度)
        this.setDeltaMovement(accelX, accelY, accelZ);
        
        // 为了让火球持续飞行，通常需要设置加速度 (power)
        // 如果 xPower 字段不可用，可能是使用了不同的映射名称或者需要使用 Access Transformer
        // 作为一个临时的解决方案，我们可以通过 setDeltaMovement 让它飞起来，并在 tick 中维持速度
        
        // 尝试猜测的字段名 (如果这是 Mojang 映射)
        // this.xPower = accelX; // 已知报错
        
        // 尝试使用 Vec3 设置加速度 (如果是新版逻辑)
        // this.accelerationPower = new Vec3(accelX, accelY, accelZ);
        
        // this.explosionPower = 2; // 默认爆炸威力 (private 无法访问，在 onHit 中自定义爆炸逻辑即可)
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_ENTITY_ID, -1);
    }

    @Override
    public void setOwner(@Nullable Entity owner) {
        super.setOwner(owner);
        if (owner != null) {
            this.entityData.set(DATA_OWNER_ENTITY_ID, owner.getId());
        } else {
            this.entityData.set(DATA_OWNER_ENTITY_ID, -1);
        }
    }

    @Nullable
    public Entity getOwnerForRender() {
        Entity owner = this.getOwner();
        if (owner != null) {
            return owner;
        }
        int ownerId = this.entityData.get(DATA_OWNER_ENTITY_ID);
        if (ownerId != -1 && this.level() != null) {
            return this.level().getEntity(ownerId);
        }
        return null;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }
    
    // 如果没有 xPower 字段，我们需要手动维持飞行速度
    @Override
    public void tick() {
        // 手动维持速度，并使其更快
        if (!this.level().isClientSide) {
             // 简单的维持速度逻辑：获取当前速度并重新设置，防止阻力减速太快
             // 稍微加速一点，倍率调整为 1.1 或更高，或者强制设定一个最小速度
             net.minecraft.world.phys.Vec3 current = this.getDeltaMovement();
             // 只有当速度不为0时才加速
             if (current.lengthSqr() > 1.0E-7D) {
                 // 保持一个较高的速度
                 this.setDeltaMovement(current.normalize().scale(2.0)); // 速度设定为 2.0 (相当快)
             }
        }
        
        super.tick();
    }

    @Override
    protected void onHit(HitResult result) {
        // 先调用 super.onHit 会触发默认的爆炸逻辑
        // 但我们想要自定义爆炸和火柱，所以我们可以选择不调用 super 或者在 super 之后添加额外逻辑
        // LargeFireball.onHit 主要是调用 explode。
        
        if (result instanceof EntityHitResult entityHitResult) {
            this.onHitEntity(entityHitResult);
        } else if (result instanceof BlockHitResult blockHitResult) {
            this.onHitBlock(blockHitResult);
        }

        if (!this.level().isClientSide) {
            // 自定义爆炸：造成伤害并破坏方块
            // 4.0F 是爆炸半径 (与TNT一致)，true 表示产生火焰
            this.level().explode(
                    this,
                    this.damageSources().explosion(this, this.getOwner()),
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    4.0F,
                    true,
                    Level.ExplosionInteraction.MOB
            );

            // 生成火柱
            createFirePillar(this.blockPosition());
            
            // 销毁实体
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity target = result.getEntity();
            DamageSource damageSource = this.damageSources().fireball(this, this.getOwner());
            target.hurt(damageSource, 6.0F);
            EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
        }
    }

    private void createFirePillar(BlockPos center) {
        // 在爆炸中心生成垂直的火焰柱
        int height = 4; // 火柱高度
        for (int i = 0; i < height; i++) {
            BlockPos pos = center.above(i);
            // 如果方块是空气，则放置火焰
            if (this.level().isEmptyBlock(pos)) {
                this.level().setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }
        
        // 也在地面生成一些火焰
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos groundPos = center.offset(x, 0, z);
                if (this.level().isEmptyBlock(groundPos) && !this.level().isEmptyBlock(groundPos.below())) {
                    this.level().setBlockAndUpdate(groundPos, Blocks.FIRE.defaultBlockState());
                }
            }
        }
    }
}
