package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.ShadowNinjaSquadManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.Nullable;

public class ShengZhuEntity extends Monster implements GeoEntity {
    private static final double BASE_MOVEMENT_SPEED = 0.24D;
    private static final ResourceLocation CHASE_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheng_zhu_chase_speed");
    private static final double CHASE_SPEED_MULTIPLIER = 1.0D;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("sheng_zhu.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("sheng_zhu.walk");
    private static final RawAnimation ATTACK_LEFT_CLAW = RawAnimation.begin().thenPlay("sheng_zhu.attack_left_claw");
    private static final RawAnimation ATTACK_RIGHT_CLAW = RawAnimation.begin().thenPlay("sheng_zhu.attack_right_claw");
    private static final RawAnimation ATTACK_BITE = RawAnimation.begin().thenPlay("sheng_zhu.attack_bite");
    private static final RawAnimation ATTACK_LEFT_REAR_KICK =
            RawAnimation.begin().thenPlay("sheng_zhu.attack_left_rear_kick");
    private static final RawAnimation ATTACK_RIGHT_REAR_KICK =
            RawAnimation.begin().thenPlay("sheng_zhu.attack_right_rear_kick");
    private static final RawAnimation ATTACK_DRAGON_BREATH =
            RawAnimation.begin().thenPlay("sheng_zhu.attack_dragon_breath");
    private static final RawAnimation ATTACK_SHADOW_SUMMON =
            RawAnimation.begin().thenPlay("sheng_zhu.attack_shadow_summon");
    private static final String ATTACK_CONTROLLER = "attack";
    private static final String LEFT_CLAW_ANIMATION = "left_claw";
    private static final String RIGHT_CLAW_ANIMATION = "right_claw";
    private static final String BITE_ANIMATION = "bite";
    private static final String LEFT_REAR_KICK_ANIMATION = "left_rear_kick";
    private static final String RIGHT_REAR_KICK_ANIMATION = "right_rear_kick";
    private static final String DRAGON_BREATH_ANIMATION = "dragon_breath";
    private static final String SHADOW_SUMMON_ANIMATION = "shadow_summon";
    private static final String[] ATTACK_ANIMATIONS = {
            LEFT_CLAW_ANIMATION,
            RIGHT_CLAW_ANIMATION,
            BITE_ANIMATION,
            LEFT_REAR_KICK_ANIMATION,
            RIGHT_REAR_KICK_ANIMATION
    };
    private static final int SHADOW_SUMMON_COOLDOWN_TICKS = 20 * 20;
    private static final int SHADOW_SUMMON_WINDUP_TICKS = 18;
    private static final int SHADOW_SUMMON_RELEASE_TICK = 8;
    private static final double SHADOW_SUMMON_MIN_DISTANCE_SQR = 16.0D;
    private static final double SHADOW_SUMMON_MAX_DISTANCE_SQR = 484.0D;
    private static final int DRAGON_BREATH_COOLDOWN_TICKS = 200;
    private static final int DRAGON_BREATH_WINDUP_TICKS = 16;
    private static final int DRAGON_BREATH_FIRE_TICK = 8;
    private static final double DRAGON_BREATH_MIN_DISTANCE_SQR = 36.0D;
    private static final double DRAGON_BREATH_MAX_DISTANCE_SQR = 576.0D;
    private static final double DRAGON_BREATH_PROJECTILE_SPEED = 0.1D;
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private int shadowSummonCooldownTicks;
    private int shadowSummonWindupTicks;
    private boolean shadowSummonPending;
    private int dragonBreathCooldownTicks;
    private int dragonBreathWindupTicks;
    private boolean dragonBreathShotPending;

    public ShengZhuEntity(EntityType<? extends ShengZhuEntity> type, Level level) {
        super(type, level);
        this.xpReward = 30;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        this.spawnAtLocation(new ItemStack(ChenMod.DRAGON_TALISMAN.get()));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.dragonBreathWindupTicks > 0 || this.shadowSummonWindupTicks > 0) {
            return false;
        }

        boolean didHurt = super.doHurtTarget(target);

        if (didHurt && !this.level().isClientSide()) {
            String attackAnimation = ATTACK_ANIMATIONS[this.getRandom().nextInt(ATTACK_ANIMATIONS.length)];
            this.triggerAnim(ATTACK_CONTROLLER, attackAnimation);
        }

        return didHurt;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setName(this.getDisplayName());

        if (this.dragonBreathCooldownTicks > 0) {
            this.dragonBreathCooldownTicks--;
        }
        if (this.shadowSummonCooldownTicks > 0) {
            this.shadowSummonCooldownTicks--;
        }

        LivingEntity target = this.getTarget();
        this.updateChaseSpeedModifier(target);
        if (this.shadowSummonWindupTicks > 0) {
            this.shadowSummonWindupTicks--;
            this.getNavigation().stop();

            if (target != null && target.isAlive()) {
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.lookAt(target, 30.0F, 30.0F);
            }

            if (this.shadowSummonPending && this.shadowSummonWindupTicks == SHADOW_SUMMON_RELEASE_TICK) {
                this.releaseShadowSummon(target);
            }

            if (this.shadowSummonWindupTicks == 0) {
                this.shadowSummonPending = false;
            }

            return;
        }

        if (this.dragonBreathWindupTicks > 0) {
            this.dragonBreathWindupTicks--;
            this.getNavigation().stop();

            if (target != null && target.isAlive()) {
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.lookAt(target, 30.0F, 30.0F);
            }

            if (this.dragonBreathShotPending && this.dragonBreathWindupTicks == DRAGON_BREATH_FIRE_TICK) {
                this.fireDragonBreath(target);
            }

            if (this.dragonBreathWindupTicks == 0) {
                this.dragonBreathShotPending = false;
            }

            return;
        }

        if (this.canUseShadowSummon(target)) {
            this.startShadowSummon(target);
            return;
        }

        if (this.canUseDragonBreath(target)) {
            this.startDragonBreathAttack(target);
        }
    }

    private void updateChaseSpeedModifier(LivingEntity target) {
        AttributeInstance movementSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        boolean shouldChase = target != null && target.isAlive();
        if (shouldChase) {
            if (!movementSpeed.hasModifier(CHASE_SPEED_MODIFIER_ID)) {
                movementSpeed.addTransientModifier(new AttributeModifier(
                        CHASE_SPEED_MODIFIER_ID,
                        CHASE_SPEED_MULTIPLIER,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ));
            }
            return;
        }

        movementSpeed.removeModifier(CHASE_SPEED_MODIFIER_ID);
    }

    private double getWalkAnimationSpeed() {
        double currentMovementSpeed = this.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (currentMovementSpeed <= 0.0D) {
            return 1.0D;
        }

        return Math.max(1.0D, currentMovementSpeed / BASE_MOVEMENT_SPEED);
    }

    private boolean canUseShadowSummon(LivingEntity target) {
        if (target == null
                || !target.isAlive()
                || this.shadowSummonCooldownTicks > 0
                || this.shadowSummonWindupTicks > 0
                || this.dragonBreathWindupTicks > 0) {
            return false;
        }

        double distanceSqr = this.distanceToSqr(target);
        return distanceSqr >= SHADOW_SUMMON_MIN_DISTANCE_SQR
                && distanceSqr <= SHADOW_SUMMON_MAX_DISTANCE_SQR
                && this.hasLineOfSight(target);
    }

    private void startShadowSummon(LivingEntity target) {
        this.shadowSummonWindupTicks = SHADOW_SUMMON_WINDUP_TICKS;
        this.shadowSummonPending = true;
        this.getNavigation().stop();
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.lookAt(target, 30.0F, 30.0F);
        this.triggerAnim(ATTACK_CONTROLLER, SHADOW_SUMMON_ANIMATION);
    }

    private void releaseShadowSummon(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel) || target == null || !target.isAlive()) {
            this.shadowSummonPending = false;
            return;
        }

        ShadowNinjaSquadManager.summonAssault(serverLevel, this, target);
        this.shadowSummonCooldownTicks = SHADOW_SUMMON_COOLDOWN_TICKS;
        this.shadowSummonPending = false;
    }

    private boolean canUseDragonBreath(LivingEntity target) {
        if (target == null
                || !target.isAlive()
                || this.dragonBreathCooldownTicks > 0
                || this.dragonBreathWindupTicks > 0
                || this.shadowSummonWindupTicks > 0) {
            return false;
        }

        double distanceSqr = this.distanceToSqr(target);
        return distanceSqr >= DRAGON_BREATH_MIN_DISTANCE_SQR
                && distanceSqr <= DRAGON_BREATH_MAX_DISTANCE_SQR
                && this.hasLineOfSight(target);
    }

    private void startDragonBreathAttack(LivingEntity target) {
        this.dragonBreathWindupTicks = DRAGON_BREATH_WINDUP_TICKS;
        this.dragonBreathShotPending = true;
        this.getNavigation().stop();
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.lookAt(target, 30.0F, 30.0F);
        this.triggerAnim(ATTACK_CONTROLLER, DRAGON_BREATH_ANIMATION);
    }

    private void fireDragonBreath(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            this.dragonBreathShotPending = false;
            return;
        }

        Vec3 aimPoint = target.getEyePosition().subtract(0.0D, target.getBbHeight() * 0.25D, 0.0D);
        Vec3 mouthOrigin = this.position().add(0.0D, this.getBbHeight() * 0.78D, 0.0D);
        Vec3 direction = aimPoint.subtract(mouthOrigin).normalize();
        if (direction.lengthSqr() < 1.0E-6D) {
            this.dragonBreathShotPending = false;
            return;
        }

        DragonFireballEntity fireball = new DragonFireballEntity(
                this.level(),
                this,
                direction.x * DRAGON_BREATH_PROJECTILE_SPEED,
                direction.y * DRAGON_BREATH_PROJECTILE_SPEED,
                direction.z * DRAGON_BREATH_PROJECTILE_SPEED);
        Vec3 spawnPos = mouthOrigin.add(direction.scale(2.2D));
        fireball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.level().addFreshEntity(fireball);

        this.dragonBreathCooldownTicks = DRAGON_BREATH_COOLDOWN_TICKS;
        this.dragonBreathShotPending = false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            state.getController().setAnimationSpeed(state.isMoving() ? this.getWalkAnimationSpeed() : 1.0D);
            if (state.isMoving()) {
                state.setAndContinue(WALK);
            } else {
                state.setAndContinue(IDLE);
            }

            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim(LEFT_CLAW_ANIMATION, ATTACK_LEFT_CLAW)
                .triggerableAnim(RIGHT_CLAW_ANIMATION, ATTACK_RIGHT_CLAW)
                .triggerableAnim(BITE_ANIMATION, ATTACK_BITE)
                .triggerableAnim(LEFT_REAR_KICK_ANIMATION, ATTACK_LEFT_REAR_KICK)
                .triggerableAnim(RIGHT_REAR_KICK_ANIMATION, ATTACK_RIGHT_REAR_KICK)
                .triggerableAnim(DRAGON_BREATH_ANIMATION, ATTACK_DRAGON_BREATH)
                .triggerableAnim(SHADOW_SUMMON_ANIMATION, ATTACK_SHADOW_SUMMON));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
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
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }
}
