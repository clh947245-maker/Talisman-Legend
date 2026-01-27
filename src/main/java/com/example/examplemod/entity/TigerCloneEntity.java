package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import java.util.Optional;
import java.util.UUID;

public class TigerCloneEntity extends Zombie {

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(TigerCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public TigerCloneEntity(EntityType<? extends TigerCloneEntity> type, Level level) {
        super(type, level);
    }
    
    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        
        if (!this.level().isClientSide) {
            UUID ownerUUID = getOwnerUUID();
            if (ownerUUID != null) {
                Player owner = this.level().getPlayerByUUID(ownerUUID);
                if (owner != null) {
                    // 计算伤害：当前生命值的一半
                    float damage = owner.getHealth() / 2.0F;
                    // 对主人造成魔法伤害
                    owner.hurt(this.damageSources().magic(), damage);
                    // 移除虎符咒魔法效果
                    owner.removeEffect(ChenMod.TIGER_POWER);
                    // 发送消息提示
                    owner.displayClientMessage(Component.literal("§c你的分身死亡了，你受到了反噬伤害！"), true);
                }
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
    }

    public void setOwnerUUID(UUID ownerUUID) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(ownerUUID));
    }

    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MAX_HEALTH, 20.0D);
    }

    @Override
    protected void registerGoals() {
        // 清除现有的目标，以确保它的行为完全符合我们的预期
        // 注意：在 1.21 版本中，如果没有 Access Transformer，可能无法轻松移除所有目标。
        // 但是，Zombie 的目标是在 super.registerGoals() 中添加的。
        // 如果我们调用 super.registerGoals()，我们会得到僵尸的行为（如攻击村民、在阳光下燃烧等）。
        // 如果我们不调用 super.registerGoals()，我们就没有默认目标。
        // 所以我们不调用 super.registerGoals()，而是自己重新定义一套目标。
        
        // 0 优先级：漂浮（防止溺水）
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 2 优先级：近战攻击
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        // 5 优先级：避开水域随机漫步
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        // 6 优先级：看着玩家（保持关注）
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // 6 优先级：随机四处张望
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // --- 目标选择器 (Target Selector) ---
        // 攻击所有存活的实体（包括玩家、怪物、动物等）
        // NearestAttackableTargetGoal 的第二个参数 LivingEntity.class 表示目标类型为所有生物
        // 第三个参数 true 表示必须看见目标才能攻击
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        // 仅在服务端执行逻辑，且实体未处于死亡状态
        if (!this.level().isClientSide && !this.isDeadOrDying()) {
            UUID ownerUUID = getOwnerUUID();
            if (ownerUUID != null) {
                // 根据 UUID 获取主人实体
                Player owner = this.level().getPlayerByUUID(ownerUUID);
                // 检查主人状态：
                // 1. owner == null: 主人离线或不在当前世界加载范围内
                // 2. !owner.hasEffect: 主人不再拥有虎符咒效果
                // 如果满足任一条件，分身消失
                if (owner == null || !owner.hasEffect(ChenMod.TIGER_POWER)) {
                     this.discard();
                }
            } else {
                // 如果没有分配主人（ownerUUID 为空）
                // 且存在时间超过 20 ticks (1秒)，则自动消失
                // 这是为了防止生成了无主的分身残留
                if (this.tickCount > 20) {
                     this.discard();
                }
            }
        }
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID != null) {
            compound.putUUID("OwnerUUID", ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("OwnerUUID")) {
            setOwnerUUID(compound.getUUID("OwnerUUID"));
        }
    }

    @Override
    public boolean isBaby() {
        return false;
    }
    
    // Prevent burning in sunlight
    @Override
    protected boolean isSunBurnTick() {
        return false;
    }
}
