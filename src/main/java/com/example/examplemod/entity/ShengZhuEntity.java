package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class ShengZhuEntity extends Monster implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("sheng_zhu.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("sheng_zhu.walk");
    private static final RawAnimation LEFT_CLAW = RawAnimation.begin().thenPlay("sheng_zhu.attack_claw");
    private static final RawAnimation RIGHT_CLAW = RawAnimation.begin().thenPlay("sheng_zhu.attack_claw_right");
    private static final RawAnimation DRAGON_BLAST = RawAnimation.begin().thenPlay("sheng_zhu.dragon_blast");
    private static final RawAnimation SHIELD_BREAK_CHARGE = RawAnimation.begin().thenPlay("sheng_zhu.shield_break_charge");
    private static final String ATTACK_CONTROLLER = "attack";
    private static final String LEFT_ATTACK_ANIMATION = "claw_left";
    private static final String RIGHT_ATTACK_ANIMATION = "claw_right";
    private static final String DRAGON_BLAST_ANIMATION = "dragon_blast";
    private static final String SHIELD_BREAK_CHARGE_ANIMATION = "shield_break_charge";
    private static final int DRAGON_BLAST_COOLDOWN_TICKS = 20 * 10;
    private static final int DRAGON_BLAST_ANIMATION_TICKS = 18;
    private static final int DRAGON_BLAST_FIRE_TICK = 7;
    private static final double DRAGON_BLAST_MIN_DISTANCE_SQR = 16.0D;
    private static final double DRAGON_BLAST_MAX_DISTANCE_SQR = 196.0D;
    private static final int SHIELD_BREAK_CHARGE_TICKS = 16;
    private static final int SHIELD_BREAK_CHARGE_HIT_TICK = 5;
    private static final int SHIELD_BREAK_DISABLE_TICKS = 100;
    private static final double SHIELD_BREAK_CHARGE_SPEED = 0.68D;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS
    );
    private boolean useRightClawNext;
    private int dragonBlastCooldownTicks;
    private int dragonBlastAnimationTicks;
    private boolean dragonBlastFired;
    private int shieldBreakChargeTicks;
    private boolean shieldBreakChargeHitApplied;
    private boolean queuedShieldBreakCharge;

    public ShengZhuEntity(EntityType<? extends ShengZhuEntity> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.65D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new DragonBlastGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false) {
            @Override
            public boolean canUse() {
                return !ShengZhuEntity.this.isPerformingDragonBlast()
                        && !ShengZhuEntity.this.isPerformingShieldBreakCharge()
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !ShengZhuEntity.this.isPerformingDragonBlast()
                        && !ShengZhuEntity.this.isPerformingShieldBreakCharge()
                        && super.canContinueToUse();
            }

            @Override
            public void tick() {
                if (ShengZhuEntity.this.isPerformingDragonBlast() || ShengZhuEntity.this.isPerformingShieldBreakCharge()) {
                    ShengZhuEntity.this.getNavigation().stop();
                    return;
                }

                super.tick();
            }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean didHurt = super.doHurtTarget(target);

        if (didHurt && !this.level().isClientSide() && !this.isPerformingShieldBreakCharge()) {
            String animation = this.useRightClawNext ? RIGHT_ATTACK_ANIMATION : LEFT_ATTACK_ANIMATION;
            this.triggerAnim(ATTACK_CONTROLLER, animation);
            this.useRightClawNext = !this.useRightClawNext;
        }

        return didHurt;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.dragonBlastCooldownTicks > 0) {
            this.dragonBlastCooldownTicks--;
        }

        if (!this.level().isClientSide()) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            this.tickDragonBlastAttack();
            this.tickShieldBreakChargeAttack();
        }
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        super.die(damageSource);
        this.bossEvent.removeAllPlayers();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        this.spawnAtLocation(new ItemStack(ChenMod.DRAGON_TALISMAN.get()));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("DragonBlastCooldownTicks", this.dragonBlastCooldownTicks);
        compound.putInt("DragonBlastAnimationTicks", this.dragonBlastAnimationTicks);
        compound.putBoolean("DragonBlastFired", this.dragonBlastFired);
        compound.putInt("ShieldBreakChargeTicks", this.shieldBreakChargeTicks);
        compound.putBoolean("ShieldBreakChargeHitApplied", this.shieldBreakChargeHitApplied);
        compound.putBoolean("QueuedShieldBreakCharge", this.queuedShieldBreakCharge);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.dragonBlastCooldownTicks = compound.getInt("DragonBlastCooldownTicks");
        this.dragonBlastAnimationTicks = compound.getInt("DragonBlastAnimationTicks");
        this.dragonBlastFired = compound.getBoolean("DragonBlastFired");
        this.shieldBreakChargeTicks = compound.getInt("ShieldBreakChargeTicks");
        this.shieldBreakChargeHitApplied = compound.getBoolean("ShieldBreakChargeHitApplied");
        this.queuedShieldBreakCharge = compound.getBoolean("QueuedShieldBreakCharge");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 4, state -> {
            if (state.isMoving()) {
                state.setAndContinue(WALK);
            } else {
                state.setAndContinue(IDLE);
            }

            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim(LEFT_ATTACK_ANIMATION, LEFT_CLAW)
                .triggerableAnim(RIGHT_ATTACK_ANIMATION, RIGHT_CLAW)
                .triggerableAnim(DRAGON_BLAST_ANIMATION, DRAGON_BLAST)
                .triggerableAnim(SHIELD_BREAK_CHARGE_ANIMATION, SHIELD_BREAK_CHARGE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    private boolean isPerformingDragonBlast() {
        return this.dragonBlastAnimationTicks > 0;
    }

    private boolean isPerformingShieldBreakCharge() {
        return this.shieldBreakChargeTicks > 0;
    }

    private boolean canStartDragonBlast(LivingEntity target) {
        return target != null
                && target.isAlive()
                && !this.isPerformingDragonBlast()
                && !this.isPerformingShieldBreakCharge()
                && this.dragonBlastCooldownTicks <= 0
                && !this.isInWater()
                && this.onGround()
                && this.getSensing().hasLineOfSight(target)
                && this.distanceToSqr(target) >= DRAGON_BLAST_MIN_DISTANCE_SQR
                && this.distanceToSqr(target) <= DRAGON_BLAST_MAX_DISTANCE_SQR;
    }

    private void startDragonBlast() {
        this.dragonBlastAnimationTicks = DRAGON_BLAST_ANIMATION_TICKS;
        this.dragonBlastFired = false;
        this.queuedShieldBreakCharge = false;
        this.getNavigation().stop();
        this.triggerAnim(ATTACK_CONTROLLER, DRAGON_BLAST_ANIMATION);
    }

    private void tickDragonBlastAttack() {
        if (!this.isPerformingDragonBlast()) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.lookAt(target, 30.0F, 30.0F);
        }

        this.getNavigation().stop();

        int elapsedTicks = DRAGON_BLAST_ANIMATION_TICKS - this.dragonBlastAnimationTicks + 1;
        if (!this.dragonBlastFired && elapsedTicks >= DRAGON_BLAST_FIRE_TICK) {
            if (target != null && target.isAlive()) {
                this.fireDragonBlast(target);
                this.dragonBlastCooldownTicks = DRAGON_BLAST_COOLDOWN_TICKS;
                this.queuedShieldBreakCharge = this.canQueueShieldBreakCharge(target);
            }
            this.dragonBlastFired = true;
        }

        this.dragonBlastAnimationTicks--;
        if (this.dragonBlastAnimationTicks <= 0 && this.queuedShieldBreakCharge) {
            this.startShieldBreakCharge();
        }
    }

    private void fireDragonBlast(LivingEntity target) {
        Vec3 direction = target.getEyePosition().subtract(this.getEyePosition());
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = this.getLookAngle();
        } else {
            direction = direction.normalize();
        }

        double startX = this.getX() + direction.x * 2.2D;
        double startY = this.getEyeY() - 0.3D + direction.y * 2.2D;
        double startZ = this.getZ() + direction.z * 2.2D;

        DragonFireballEntity fireball = new DragonFireballEntity(
                this.level(),
                this,
                direction.x * 0.1D,
                direction.y * 0.1D,
                direction.z * 0.1D
        );
        fireball.setPos(startX, startY, startZ);
        this.level().addFreshEntity(fireball);
    }

    private boolean canStartShieldBreakCharge(LivingEntity target) {
        return target != null
                && target.isAlive()
                && !this.isPerformingDragonBlast()
                && !this.isPerformingShieldBreakCharge()
                && !this.isInWater()
                && this.onGround()
                && this.getSensing().hasLineOfSight(target);
    }

    private boolean canQueueShieldBreakCharge(LivingEntity target) {
        return target != null
                && target.isAlive()
                && !this.isPerformingShieldBreakCharge()
                && !this.isInWater()
                && this.onGround()
                && this.getSensing().hasLineOfSight(target);
    }

    private void startShieldBreakCharge() {
        LivingEntity target = this.getTarget();
        this.queuedShieldBreakCharge = false;
        if (!this.canStartShieldBreakCharge(target)) {
            return;
        }

        this.shieldBreakChargeTicks = SHIELD_BREAK_CHARGE_TICKS;
        this.shieldBreakChargeHitApplied = false;
        this.getNavigation().stop();
        this.triggerAnim(ATTACK_CONTROLLER, SHIELD_BREAK_CHARGE_ANIMATION);
    }

    private void tickShieldBreakChargeAttack() {
        if (!this.isPerformingShieldBreakCharge()) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.stopShieldBreakCharge();
            return;
        }

        this.getNavigation().stop();
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.lookAt(target, 30.0F, 30.0F);

        Vec3 horizontalOffset = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        if (horizontalOffset.lengthSqr() > 1.0E-4D) {
            Vec3 chargeDirection = horizontalOffset.normalize();
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(chargeDirection.x * SHIELD_BREAK_CHARGE_SPEED, motion.y, chargeDirection.z * SHIELD_BREAK_CHARGE_SPEED);
            this.hasImpulse = true;
        }

        int elapsedTicks = SHIELD_BREAK_CHARGE_TICKS - this.shieldBreakChargeTicks + 1;
        if (!this.shieldBreakChargeHitApplied
                && elapsedTicks >= SHIELD_BREAK_CHARGE_HIT_TICK
                && this.distanceToSqr(target) <= this.getShieldBreakChargeHitDistanceSqr(target)) {
            boolean shieldBroken = this.disableShieldIfBlocking(target);
            boolean didHurt = this.doHurtTarget(target);
            if (didHurt || shieldBroken) {
                this.shieldBreakChargeHitApplied = true;
                target.knockback(2.2D, this.getX() - target.getX(), this.getZ() - target.getZ());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.35D));
            }
        }

        this.shieldBreakChargeTicks--;
        if (this.shieldBreakChargeTicks <= 0) {
            this.stopShieldBreakCharge();
        }
    }

    private void stopShieldBreakCharge() {
        this.shieldBreakChargeTicks = 0;
        this.shieldBreakChargeHitApplied = false;
        this.queuedShieldBreakCharge = false;
        this.getNavigation().stop();
    }

    private double getShieldBreakChargeHitDistanceSqr(LivingEntity target) {
        double reach = 1.4D + this.getBbWidth() + target.getBbWidth();
        return reach * reach;
    }

    private boolean disableShieldIfBlocking(LivingEntity target) {
        if (!(target instanceof Player player) || !player.isBlocking()) {
            return false;
        }

        ItemStack useItem = player.getUseItem();
        if (!useItem.isEmpty()) {
            player.getCooldowns().addCooldown(useItem.getItem(), SHIELD_BREAK_DISABLE_TICKS);
        }
        player.stopUsingItem();
        this.level().broadcastEntityEvent(player, (byte) 30);
        return true;
    }

    private class DragonBlastGoal extends Goal {
        private DragonBlastGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return ShengZhuEntity.this.canStartDragonBlast(ShengZhuEntity.this.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            if (!ShengZhuEntity.this.isPerformingDragonBlast()) {
                return false;
            }

            LivingEntity target = ShengZhuEntity.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            ShengZhuEntity.this.startDragonBlast();
        }

        @Override
        public void stop() {
            ShengZhuEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = ShengZhuEntity.this.getTarget();
            if (target != null) {
                ShengZhuEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
        }
    }
}
