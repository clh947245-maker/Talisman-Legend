package com.example.examplemod.entity;

import com.example.examplemod.item.OniMaskItem;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ShadowNinjaEntity extends Monster implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("shadow_ninja.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("shadow_ninja.walk");
    private static final RawAnimation KNEEL = RawAnimation.begin().thenLoop("shadow_ninja.kneel");
    private static final RawAnimation SUMMON = RawAnimation.begin().thenPlay("shadow_ninja.summon");
    private static final RawAnimation DISMISS = RawAnimation.begin().thenPlay("shadow_ninja.dismiss");
    private static final RawAnimation LEFT_PUNCH = RawAnimation.begin().thenPlay("shadow_ninja.attack_left_punch");
    private static final RawAnimation RIGHT_PUNCH = RawAnimation.begin().thenPlay("shadow_ninja.attack_right_punch");
    private static final RawAnimation KICK = RawAnimation.begin().thenPlay("shadow_ninja.attack_kick");
    private static final RawAnimation JUMP_KICK = RawAnimation.begin().thenPlay("shadow_ninja.attack_jump_kick");
    private static final String ATTACK_CONTROLLER = "attack";
    private static final String JUMP_KICK_ANIMATION = "jump_kick";
    private static final String[] ATTACK_ANIMATIONS = {"left_punch", "right_punch", "kick"};
    private static final byte COMMAND_STATE_NORMAL = 0;
    private static final byte COMMAND_STATE_SUMMONING = 1;
    private static final byte COMMAND_STATE_DISMISSING = 2;
    private static final byte COMMAND_STATE_SHADOW_RUSH_SINK = 3;
    private static final byte COMMAND_STATE_SHADOW_RUSH_RISE = 4;
    private static final int SUMMON_ANIMATION_TICKS = 30;
    private static final int DISMISS_ANIMATION_TICKS = 30;
    private static final int SHADOW_RUSH_SINK_TICKS = 10;
    private static final int SHADOW_RUSH_RISE_TICKS = 10;
    private static final double BASE_MOVEMENT_SPEED = 0.35D;
    private static final double ATTACK_MOVE_SPEED = 1.25D;
    private static final double FOLLOW_MOVE_SPEED = 1.25D;
    private static final double SUMMON_BURIED_OFFSET = 2.8D;
    private static final double DISMISS_BURIED_OFFSET = 3.0D;
    private static final double SHADOW_RUSH_BURIED_OFFSET = 2.8D;
    private static final float SUMMON_RISE_START_PROGRESS = 0.22F;
    private static final float DISMISS_SINK_END_PROGRESS = 0.68F;
    private static final double SHADOW_RUSH_TARGET_TRIGGER_DISTANCE = 20.0D;
    private static final double SHADOW_RUSH_TARGET_TRIGGER_DISTANCE_SQR =
            SHADOW_RUSH_TARGET_TRIGGER_DISTANCE * SHADOW_RUSH_TARGET_TRIGGER_DISTANCE;
    private static final double SHADOW_RUSH_COMMANDER_TRIGGER_DISTANCE = 10.0D;
    private static final double SHADOW_RUSH_COMMANDER_TRIGGER_DISTANCE_SQR =
            SHADOW_RUSH_COMMANDER_TRIGGER_DISTANCE * SHADOW_RUSH_COMMANDER_TRIGGER_DISTANCE;
    private static final double SHADOW_RUSH_OWNER_OFFSET = 2.5D;
    private static final double SHADOW_RUSH_TARGET_OFFSET = 1.85D;
    private static final int SHADOW_RUSH_COOLDOWN_TICKS = 40;
    private static final int[] SHADOW_RUSH_Y_SEARCH_OFFSETS = {2, 1, 0, -1, -2, -3, -4};
    private static final double[] SHADOW_RUSH_RADIUS_VARIATIONS = {1.0D, 1.45D, 2.0D, 2.6D};
    private static final float[] SHADOW_RUSH_ANGLE_OFFSETS =
            {0.0F, 35.0F, -35.0F, 70.0F, -70.0F, 110.0F, -110.0F, 180.0F};
    private static final double JUMP_KICK_MIN_DISTANCE_SQR = 9.0D;
    private static final double JUMP_KICK_MAX_DISTANCE_SQR = 36.0D;
    private static final double JUMP_KICK_VERTICAL_RANGE = 1.75D;
    private static final int JUMP_KICK_COOLDOWN_TICKS = 45;
    private static final int JUMP_KICK_ACTIVE_TICKS = 14;
    private static final double NINJA_JUMP_Y_MOTION = 0.78D;
    private static final double JUMP_KICK_Y_MOTION = NINJA_JUMP_Y_MOTION;
    private static final double COMMAND_ACQUIRE_RADIUS = 32.0D;
    private static final double COMMAND_ACQUIRE_RADIUS_SQR = COMMAND_ACQUIRE_RADIUS * COMMAND_ACQUIRE_RADIUS;
    private static final double COMMAND_RELEASE_RADIUS = 48.0D;
    private static final double COMMAND_RELEASE_RADIUS_SQR = COMMAND_RELEASE_RADIUS * COMMAND_RELEASE_RADIUS;
    private static final double MUSTER_RADIUS = 8.0D;
    private static final double MUSTER_RADIUS_SQR = MUSTER_RADIUS * MUSTER_RADIUS;
    private static final double PERSONAL_SPACE_RADIUS = 3.5D;
    private static final double PERSONAL_SPACE_RADIUS_SQR = PERSONAL_SPACE_RADIUS * PERSONAL_SPACE_RADIUS;
    private static final double FRIENDLY_ASSIST_RADIUS = 20.0D;
    private static final double FRIENDLY_ASSIST_RADIUS_SQR = FRIENDLY_ASSIST_RADIUS * FRIENDLY_ASSIST_RADIUS;
    private static final int PEACEFUL_TARGET_SCAN_INTERVAL = 10;
    private static final int COMMANDER_COMBAT_MEMORY_TICKS = 120;
    private static final int COMMANDER_IDLE_KNEEL_TICKS = 20 * 8;
    private static final int SELF_DEFENSE_MEMORY_TICKS = 120;
    private static final int COLLECTIVE_HATE_MEMORY_TICKS = 160;
    private static final float SAFE_FALL_DISTANCE = 5.0F;
    private static final double COMMANDER_IDLE_MOVEMENT_EPSILON_SQR = 0.0009D;
    private static final float BASE_SHADOW_RADIUS = 0.5F;
    private static final float BASE_SHADOW_STRENGTH = 1.0F;
    private static final EntityDataAccessor<Optional<UUID>> DATA_COMMANDER_UUID =
            SynchedEntityData.defineId(ShadowNinjaEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> DATA_LEGION_SUMMONER_UUID =
            SynchedEntityData.defineId(ShadowNinjaEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_KNEELING =
            SynchedEntityData.defineId(ShadowNinjaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_MASK_SUMMONED =
            SynchedEntityData.defineId(ShadowNinjaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_COMMAND_STATE =
            SynchedEntityData.defineId(ShadowNinjaEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_STATE_TICKS =
            SynchedEntityData.defineId(ShadowNinjaEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private int jumpKickCooldownTicks;
    private int jumpKickActiveTicks;
    private boolean jumpKickHitApplied;
    private int shadowRushCooldownTicks;
    private boolean summonPoseLocked;
    private int commanderIdleTicks;
    private UUID trackedCommanderUuid;
    private Vec3 lastCommanderPosition = Vec3.ZERO;
    private LivingEntity collectiveAggroTarget;
    private int collectiveAggroTimestamp;
    private double transitionStartY;
    private double transitionTargetY;
    private double shadowRushDestinationX;
    private double shadowRushDestinationY;
    private double shadowRushDestinationZ;

    public ShadowNinjaEntity(EntityType<? extends ShadowNinjaEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public static boolean checkShadowNinjaSpawnRules(EntityType<ShadowNinjaEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return Mob.checkMobSpawnRules(entityType, level, spawnType, pos, random);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    public boolean isFriendlyToPlayers() {
        return this.entityData.get(DATA_COMMANDER_UUID).isPresent();
    }

    public boolean isMaskSummoned() {
        return this.entityData.get(DATA_MASK_SUMMONED);
    }

    public boolean isCommandedBy(Player player) {
        return player != null && this.isCommandedBy(player.getUUID());
    }

    public boolean isCommandedBy(UUID commanderUuid) {
        return commanderUuid != null && commanderUuid.equals(this.getCommanderUUID().orElse(null));
    }

    public void assignCommander(Player commander, boolean maskSummoned) {
        if (commander == null) {
            return;
        }

        this.setCommanderUUID(commander.getUUID());
        this.entityData.set(DATA_MASK_SUMMONED, maskSummoned);
        this.resetCommanderIdleState();
    }

    public void startSummonAnimation() {
        this.startTransition(COMMAND_STATE_SUMMONING, SUMMON_ANIMATION_TICKS);
    }

    public void startDismissAnimation() {
        this.transitionStartY = this.getY();
        this.transitionTargetY = this.getY() - DISMISS_BURIED_OFFSET;
        this.startTransition(COMMAND_STATE_DISMISSING, DISMISS_ANIMATION_TICKS);
    }

    public void prepareSummonFromBelow(double surfaceY) {
        this.transitionStartY = surfaceY - SUMMON_BURIED_OFFSET;
        this.transitionTargetY = surfaceY;
        this.setPos(this.getX(), this.transitionStartY, this.getZ());
        LivingEntity commanderAnchor = this.getCommanderAnchor();
        if (commanderAnchor != null) {
            this.faceCommander(commanderAnchor);
        }
        this.startSummonAnimation();
    }

    public void prepareAmbushAttack(LivingEntity target, double surfaceY) {
        if (target != null && target.isAlive()) {
            this.collectiveAggroTarget = target;
            this.collectiveAggroTimestamp = this.tickCount;
        }
        this.prepareSummonFromBelow(surfaceY);
    }

    public void assignLegionSummoner(LivingEntity summoner) {
        this.entityData.set(DATA_LEGION_SUMMONER_UUID, Optional.ofNullable(summoner == null ? null : summoner.getUUID()));
    }

    public void kneelForCommander(LivingEntity commander) {
        if (commander == null || this.isTransitioning() || this.isDismissing()) {
            return;
        }

        this.setTarget(null);
        this.summonPoseLocked = true;
        this.setKneeling(true);
        this.getNavigation().stop();
        this.getLookControl().setLookAt(commander, 20.0F, 20.0F);
        this.faceCommander(commander);
    }

    public boolean isSummonedByLegion(UUID summonerUuid) {
        return summonerUuid != null && summonerUuid.equals(this.getLegionSummonerUUID().orElse(null));
    }

    public boolean isSummoning() {
        return this.entityData.get(DATA_COMMAND_STATE) == COMMAND_STATE_SUMMONING;
    }

    public boolean isDismissing() {
        return this.entityData.get(DATA_COMMAND_STATE) == COMMAND_STATE_DISMISSING;
    }

    public boolean isShadowRushSinking() {
        return this.entityData.get(DATA_COMMAND_STATE) == COMMAND_STATE_SHADOW_RUSH_SINK;
    }

    public boolean isShadowRushRising() {
        return this.entityData.get(DATA_COMMAND_STATE) == COMMAND_STATE_SHADOW_RUSH_RISE;
    }

    public boolean isShadowRushing() {
        return this.isShadowRushSinking() || this.isShadowRushRising();
    }

    public boolean isTransitioning() {
        return this.entityData.get(DATA_COMMAND_STATE) != COMMAND_STATE_NORMAL;
    }

    public float getTransitionVisualProgress() {
        return this.getTransitionProgress();
    }

    public float getTransitionShadowOffsetY(float partialTick) {
        if (!this.isTransitioning()) {
            return 0.0F;
        }

        double surfaceY = (this.isSummoning() || this.isShadowRushRising()) ? this.transitionTargetY : this.transitionStartY;
        double currentY = Mth.lerp(partialTick, this.yo, this.getY());
        return (float) (surfaceY - currentY);
    }

    public float getShadowRadiusScale() {
        return this.isTransitioning() ? 0.0F : BASE_SHADOW_RADIUS;
    }

    public float getShadowStrengthScale() {
        return this.isTransitioning() ? 0.0F : BASE_SHADOW_STRENGTH;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COMMANDER_UUID, Optional.empty());
        builder.define(DATA_LEGION_SUMMONER_UUID, Optional.empty());
        builder.define(DATA_KNEELING, false);
        builder.define(DATA_MASK_SUMMONED, false);
        builder.define(DATA_COMMAND_STATE, COMMAND_STATE_NORMAL);
        builder.define(DATA_STATE_TICKS, 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            SpawnGroupData spawnGroupData) {
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        this.getCommanderUUID().ifPresent(uuid -> compound.putUUID("CommanderUUID", uuid));
        this.getLegionSummonerUUID().ifPresent(uuid -> compound.putUUID("LegionSummonerUUID", uuid));
        compound.putBoolean("Kneeling", this.isKneeling());
        compound.putBoolean("MaskSummoned", this.isMaskSummoned());
        compound.putByte("CommandState", this.entityData.get(DATA_COMMAND_STATE));
        compound.putInt("StateTicks", this.entityData.get(DATA_STATE_TICKS));
        compound.putInt("ShadowRushCooldownTicks", this.shadowRushCooldownTicks);
        compound.putBoolean("SummonPoseLocked", this.summonPoseLocked);
        compound.putDouble("TransitionStartY", this.transitionStartY);
        compound.putDouble("TransitionTargetY", this.transitionTargetY);
        compound.putDouble("ShadowRushDestinationX", this.shadowRushDestinationX);
        compound.putDouble("ShadowRushDestinationY", this.shadowRushDestinationY);
        compound.putDouble("ShadowRushDestinationZ", this.shadowRushDestinationZ);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("CommanderUUID")) {
            this.entityData.set(DATA_COMMANDER_UUID, Optional.of(compound.getUUID("CommanderUUID")));
        } else {
            this.entityData.set(DATA_COMMANDER_UUID, Optional.empty());
        }
        if (compound.hasUUID("LegionSummonerUUID")) {
            this.entityData.set(DATA_LEGION_SUMMONER_UUID, Optional.of(compound.getUUID("LegionSummonerUUID")));
        } else {
            this.entityData.set(DATA_LEGION_SUMMONER_UUID, Optional.empty());
        }
        this.entityData.set(DATA_KNEELING, compound.getBoolean("Kneeling"));
        this.entityData.set(DATA_MASK_SUMMONED, compound.getBoolean("MaskSummoned"));
        this.entityData.set(DATA_COMMAND_STATE, compound.getByte("CommandState"));
        this.entityData.set(DATA_STATE_TICKS, compound.getInt("StateTicks"));
        this.shadowRushCooldownTicks = compound.getInt("ShadowRushCooldownTicks");
        this.summonPoseLocked = compound.getBoolean("SummonPoseLocked");
        this.transitionStartY = compound.getDouble("TransitionStartY");
        this.transitionTargetY = compound.getDouble("TransitionTargetY");
        this.shadowRushDestinationX = compound.getDouble("ShadowRushDestinationX");
        this.shadowRushDestinationY = compound.getDouble("ShadowRushDestinationY");
        this.shadowRushDestinationZ = compound.getDouble("ShadowRushDestinationZ");
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (super.isAlliedTo(entity)) {
            return true;
        }

        UUID legionSummonerUuid = this.getLegionSummonerUUID().orElse(null);
        if (legionSummonerUuid != null) {
            if (entity != null && legionSummonerUuid.equals(entity.getUUID())) {
                return true;
            }

            if (entity instanceof ShadowNinjaEntity other
                    && legionSummonerUuid.equals(other.getLegionSummonerUUID().orElse(null))) {
                return true;
            }
        }

        if (!this.isFriendlyToPlayers()) {
            return false;
        }

        Player commander = this.getCommander();
        LivingEntity commanderAnchor = this.getCommanderAnchor();
        if (commander != null && entity == commander) {
            return true;
        }

        if (commanderAnchor != null && entity == commanderAnchor) {
            return true;
        }

        if (entity instanceof Player player && OniMaskItem.isWearingOniMask(player)) {
            return true;
        }

        return entity instanceof ShadowNinjaEntity other && other.isFriendlyToPlayers();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, ATTACK_MOVE_SPEED, false) {
            @Override
            public boolean canUse() {
                return !ShadowNinjaEntity.this.isPerformingJumpKick()
                        && !ShadowNinjaEntity.this.isTransitioning()
                        && (!ShadowNinjaEntity.this.isFriendlyToPlayers() || ShadowNinjaEntity.this.hasValidFriendlyAttackTarget())
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !ShadowNinjaEntity.this.isPerformingJumpKick()
                        && !ShadowNinjaEntity.this.isTransitioning()
                        && (!ShadowNinjaEntity.this.isFriendlyToPlayers() || ShadowNinjaEntity.this.hasValidFriendlyAttackTarget())
                        && super.canContinueToUse();
            }

            @Override
            public void tick() {
                if (ShadowNinjaEntity.this.isPerformingJumpKick() || ShadowNinjaEntity.this.isTransitioning()) {
                    ShadowNinjaEntity.this.getNavigation().stop();
                    return;
                }

                super.tick();
            }
        });
        this.goalSelector.addGoal(3, new FriendlyFollowCommanderGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return !ShadowNinjaEntity.this.isFriendlyToPlayers() && !ShadowNinjaEntity.this.isTransitioning() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !ShadowNinjaEntity.this.isFriendlyToPlayers() && !ShadowNinjaEntity.this.isTransitioning() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return !ShadowNinjaEntity.this.isFriendlyToPlayers() && !ShadowNinjaEntity.this.isTransitioning() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !ShadowNinjaEntity.this.isFriendlyToPlayers() && !ShadowNinjaEntity.this.isTransitioning() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !ShadowNinjaEntity.this.isFriendlyToPlayers() && !ShadowNinjaEntity.this.isTransitioning() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !ShadowNinjaEntity.this.isFriendlyToPlayers() && !ShadowNinjaEntity.this.isTransitioning() && super.canContinueToUse();
            }
        });

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !ShadowNinjaEntity.this.isFriendlyToPlayers() && !ShadowNinjaEntity.this.isTransitioning() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !ShadowNinjaEntity.this.isFriendlyToPlayers() && !ShadowNinjaEntity.this.isTransitioning() && super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return !ShadowNinjaEntity.this.isFriendlyToPlayers() && !ShadowNinjaEntity.this.isTransitioning() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !ShadowNinjaEntity.this.isFriendlyToPlayers() && !ShadowNinjaEntity.this.isTransitioning() && super.canContinueToUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            this.tickTransitionState();
            if (!this.isRemoved() && !this.isTransitioning()) {
                this.tickCommanderControl();
                this.tryStartShadowRush();
            } else {
                this.setKneeling(false);
            }
        }

        if (!this.level().isClientSide() && this.shadowRushCooldownTicks > 0) {
            this.shadowRushCooldownTicks--;
        }

        if (this.isTransitioning()) {
            this.getNavigation().stop();
            if (this.shouldClearTargetDuringTransition()) {
                this.setTarget(null);
            }
            this.jumpKickHitApplied = false;
            return;
        }

        if (!this.level().isClientSide() && !this.isFriendlyToPlayers() && this.getTarget() == null) {
            LivingEntity ambushTarget = this.getCollectiveAggroTarget();
            if (this.isRecentCollectiveAggroTarget(ambushTarget)) {
                this.setTarget(ambushTarget);
            } else if (this.level().getDifficulty() == Difficulty.PEACEFUL
                    && this.tickCount % PEACEFUL_TARGET_SCAN_INTERVAL == 0) {
                this.setTarget(this.findPeacefulHostileTarget());
            }
        }

        if (this.jumpKickCooldownTicks > 0) {
            this.jumpKickCooldownTicks--;
        }

        if (this.jumpKickActiveTicks > 0) {
            this.jumpKickActiveTicks--;
            this.tickJumpKickAttack();
        } else {
            this.jumpKickHitApplied = false;
        }

        if (!this.level().isClientSide() && this.canStartJumpKick()) {
            this.startJumpKick();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean didHurt = super.hurt(source, amount);

        if (didHurt && !this.level().isClientSide() && !this.isDeadOrDying()) {
            LivingEntity attacker = this.resolveAggressor(source);
            if (attacker != null && this.canAssistAgainst(attacker)) {
                this.broadcastCollectiveHate(attacker);
            }
        }

        return didHurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean didHurt = this.shouldUsePeacefulPlayerAttack(target)
                ? this.doPeacefulPlayerAttack(target)
                : super.doHurtTarget(target);

        if (didHurt && !this.level().isClientSide() && !this.isPerformingJumpKick() && !this.isTransitioning()) {
            this.triggerAnim(ATTACK_CONTROLLER, ATTACK_ANIMATIONS[this.random.nextInt(ATTACK_ANIMATIONS.length)]);
        }

        return didHurt;
    }

    private boolean shouldUsePeacefulPlayerAttack(Entity target) {
        return this.level().getDifficulty() == Difficulty.PEACEFUL && target instanceof Player;
    }

    private boolean doPeacefulPlayerAttack(Entity target) {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSource damageSource = this.damageSources().mobAttack(this);
        if (this.level() instanceof ServerLevel serverLevel) {
            damage = EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), target, damageSource, damage);
        }

        boolean didHurt = target.hurt(damageSource, damage);
        if (didHurt) {
            float knockback = this.getKnockback(target, damageSource);
            if (knockback > 0.0F && target instanceof LivingEntity livingEntity) {
                livingEntity.knockback(
                        knockback * 0.5F,
                        Mth.sin(this.getYRot() * (float) (Math.PI / 180.0D)),
                        -Mth.cos(this.getYRot() * (float) (Math.PI / 180.0D))
                );
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
            }

            this.setLastHurtMob(target);
            this.playAttackSound();
        }

        return didHurt;
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target == null || this.canAssistAgainst(target) ? target : null);
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        Vec3 currentMotion = this.getDeltaMovement();
        if (currentMotion.y < NINJA_JUMP_Y_MOTION) {
            this.setDeltaMovement(currentMotion.x, NINJA_JUMP_Y_MOTION, currentMotion.z);
            this.hasImpulse = true;
        }
    }

    @Override
    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        float adjustedFallDistance = Math.max(0.0F, fallDistance - (SAFE_FALL_DISTANCE - 3.0F));
        return super.calculateFallDamage(adjustedFallDistance, damageMultiplier);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "base", 5, state -> {
            if (this.isDismissing() || this.isShadowRushSinking()) {
                state.setAndContinue(DISMISS);
            } else if (this.isSummoning() || this.isShadowRushRising()) {
                state.setAndContinue(SUMMON);
            } else if (this.isKneeling() && !this.isPerformingJumpKick()) {
                state.setAndContinue(KNEEL);
            } else if (state.isMoving()) {
                state.setAndContinue(WALK);
            } else {
                state.setAndContinue(IDLE);
            }

            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, ATTACK_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim("left_punch", LEFT_PUNCH)
                .triggerableAnim("right_punch", RIGHT_PUNCH)
                .triggerableAnim("kick", KICK)
                .triggerableAnim(JUMP_KICK_ANIMATION, JUMP_KICK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    private boolean isPerformingJumpKick() {
        return this.jumpKickActiveTicks > 0;
    }

    private Optional<UUID> getCommanderUUID() {
        return this.entityData.get(DATA_COMMANDER_UUID);
    }

    private Optional<UUID> getLegionSummonerUUID() {
        return this.entityData.get(DATA_LEGION_SUMMONER_UUID);
    }

    private void setCommanderUUID(UUID commanderUuid) {
        this.entityData.set(DATA_COMMANDER_UUID, Optional.ofNullable(commanderUuid));
    }

    private void clearCommander() {
        this.entityData.set(DATA_COMMANDER_UUID, Optional.empty());
        this.entityData.set(DATA_MASK_SUMMONED, false);
        this.setKneeling(false);
        this.summonPoseLocked = false;
        this.resetCommanderIdleState();
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
    }

    private Player getCommander() {
        return this.getCommanderUUID().map(uuid -> this.level().getPlayerByUUID(uuid)).orElse(null);
    }

    private LivingEntity getCommanderAnchor() {
        Player commander = this.getCommander();
        return commander == null ? null : OniMaskItem.getMaskAnchor(commander);
    }

    private boolean isKneeling() {
        return this.entityData.get(DATA_KNEELING);
    }

    private void setKneeling(boolean kneeling) {
        this.entityData.set(DATA_KNEELING, kneeling);
    }

    private void startTransition(byte state, int durationTicks) {
        this.startTransition(state, durationTicks, true);
    }

    private void startTransition(byte state, int durationTicks, boolean clearTarget) {
        this.entityData.set(DATA_COMMAND_STATE, state);
        this.entityData.set(DATA_STATE_TICKS, durationTicks);
        this.setKneeling(false);
        this.summonPoseLocked = false;
        if (clearTarget) {
            this.setTarget(null);
        }
        this.getNavigation().stop();
    }

    private float getTransitionProgress() {
        int totalTicks;
        if (this.isSummoning()) {
            totalTicks = SUMMON_ANIMATION_TICKS;
        } else if (this.isDismissing()) {
            totalTicks = DISMISS_ANIMATION_TICKS;
        } else if (this.isShadowRushSinking()) {
            totalTicks = SHADOW_RUSH_SINK_TICKS;
        } else if (this.isShadowRushRising()) {
            totalTicks = SHADOW_RUSH_RISE_TICKS;
        } else {
            totalTicks = 0;
        }
        if (totalTicks <= 0) {
            return 1.0F;
        }

        int remainingTicks = Math.max(0, this.entityData.get(DATA_STATE_TICKS));
        return 1.0F - Math.min(1.0F, remainingTicks / (float) totalTicks);
    }

    private void tickTransitionState() {
        if (!this.isTransitioning()) {
            return;
        }

        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.setKneeling(false);
        this.getNavigation().stop();
        if (this.isSummoning()) {
            LivingEntity commanderAnchor = this.getCommanderAnchor();
            if (commanderAnchor != null) {
                this.faceCommander(commanderAnchor);
            }
        }
        if (this.shouldClearTargetDuringTransition()) {
            this.setTarget(null);
        }

        float progress = this.getTransitionProgress();
        float moveProgress;
        if (this.isSummoning() || this.isShadowRushRising()) {
            moveProgress = Mth.clamp(
                    (progress - SUMMON_RISE_START_PROGRESS) / (1.0F - SUMMON_RISE_START_PROGRESS),
                    0.0F,
                    1.0F
            );
        } else {
            moveProgress = Mth.clamp(progress / DISMISS_SINK_END_PROGRESS, 0.0F, 1.0F);
        }

        double currentY = this.transitionStartY + (this.transitionTargetY - this.transitionStartY) * moveProgress;
        this.setPos(this.getX(), currentY, this.getZ());

        int remainingTicks = this.entityData.get(DATA_STATE_TICKS);
        if (remainingTicks > 0) {
            this.entityData.set(DATA_STATE_TICKS, remainingTicks - 1);
        }

        if (remainingTicks > 1) {
            return;
        }

        boolean wasSummoning = this.isSummoning();
        if (this.isDismissing()) {
            this.discard();
            return;
        }

        if (this.isShadowRushSinking()) {
            this.beginShadowRushRise();
            return;
        }

        this.noPhysics = false;
        this.setNoGravity(false);
        this.setPos(this.getX(), this.transitionTargetY, this.getZ());
        this.entityData.set(DATA_COMMAND_STATE, COMMAND_STATE_NORMAL);
        this.entityData.set(DATA_STATE_TICKS, 0);
        if (wasSummoning) {
            this.beginSummonCeremony();
        }

        LivingEntity collectiveTarget = this.getCollectiveAggroTarget();
        if (this.isRecentCollectiveAggroTarget(collectiveTarget)) {
            this.setTarget(collectiveTarget);
        }
    }

    private void tickCommanderControl() {
        this.updateCommanderAssignment();

        LivingEntity commanderAnchor = this.getCommanderAnchor();
        if (commanderAnchor == null) {
            this.setKneeling(false);
            this.summonPoseLocked = false;
            this.resetCommanderIdleState();
            return;
        }

        this.updateCommanderIdleState(commanderAnchor);

        LivingEntity currentTarget = this.getTarget();
        if (currentTarget != null && !this.canKeepFriendlyAttackTarget(commanderAnchor, currentTarget)) {
            this.setTarget(null);
            currentTarget = null;
        }

        if (currentTarget == null) {
            LivingEntity assistTarget = this.findSelfDefenseTarget();
            if (assistTarget == null) {
                assistTarget = this.findAssistTarget(commanderAnchor);
            }
            if (assistTarget != null) {
                this.setTarget(assistTarget);
                currentTarget = assistTarget;
            }
        }

        if (currentTarget != null) {
            this.summonPoseLocked = false;
        }

        boolean kneeling = currentTarget == null
                && (this.shouldKeepSummonPose(commanderAnchor) || this.shouldKneelForCommander(commanderAnchor));
        this.setKneeling(kneeling);
        if (kneeling) {
            this.getNavigation().stop();
            this.getLookControl().setLookAt(commanderAnchor, 20.0F, 20.0F);
            this.faceCommander(commanderAnchor);
        }
    }

    private void faceCommander(LivingEntity commander) {
        double deltaX = commander.getX() - this.getX();
        double deltaZ = commander.getZ() - this.getZ();
        if (deltaX * deltaX + deltaZ * deltaZ < 1.0E-6D) {
            return;
        }

        // Keep kneeling orientation aligned with the entity's normal facing so the ninja
        // actually faces the commander instead of presenting its back.
        float yaw = (float) (Mth.atan2(deltaZ, deltaX) * (180.0F / Math.PI)) - 90.0F;
        yaw = Mth.wrapDegrees(yaw);
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setYBodyRot(yaw);
        this.yBodyRotO = yaw;
        this.setYHeadRot(yaw);
        this.yHeadRotO = yaw;
    }

    private void updateCommanderAssignment() {
        Player currentCommander = this.getCommander();
        if (this.canServeCommander(currentCommander, COMMAND_RELEASE_RADIUS_SQR)) {
            return;
        }

        if (this.isMaskSummoned()) {
            if (currentCommander != null || this.isFriendlyToPlayers()) {
                this.startDismissAnimation();
            }
            return;
        }

        Player newCommander = this.findNearbyCommander();
        if (newCommander != null) {
            this.setCommanderUUID(newCommander.getUUID());
            return;
        }

        if (currentCommander != null || this.isFriendlyToPlayers()) {
            this.clearCommander();
        }
    }

    private Player findNearbyCommander() {
        Player closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(COMMAND_ACQUIRE_RADIUS))) {
            if (!this.canServeCommander(player, COMMAND_ACQUIRE_RADIUS_SQR)) {
                continue;
            }

            LivingEntity commanderAnchor = OniMaskItem.getMaskAnchor(player);
            if (commanderAnchor == null) {
                continue;
            }

            double distance = this.distanceToSqr(commanderAnchor);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }

        return closest;
    }

    private boolean canServeCommander(Player commander, double maxDistanceSqr) {
        LivingEntity commanderAnchor = commander == null ? null : OniMaskItem.getMaskAnchor(commander);
        return commander != null
                && commander.isAlive()
                && !commander.isSpectator()
                && commanderAnchor != null
                && this.distanceToSqr(commanderAnchor) <= maxDistanceSqr;
    }

    private boolean shouldKneelForCommander(LivingEntity commander) {
        double distanceSqr = this.distanceToSqr(commander);
        return this.onGround()
                && !this.isPerformingJumpKick()
                && distanceSqr <= MUSTER_RADIUS_SQR
                && distanceSqr >= PERSONAL_SPACE_RADIUS_SQR
                && this.commanderIdleTicks >= COMMANDER_IDLE_KNEEL_TICKS
                && this.getNavigation().isDone();
    }

    private boolean hasValidFriendlyAttackTarget() {
        LivingEntity commander = this.getCommanderAnchor();
        LivingEntity target = this.getTarget();
        return commander != null && target != null && this.canKeepFriendlyAttackTarget(commander, target);
    }

    private LivingEntity findAssistTarget(LivingEntity commander) {
        LivingEntity recentAttacker = commander.getLastHurtByMob();
        if (this.isRecentCommanderAttacker(commander, recentAttacker)) {
            return recentAttacker;
        }

        LivingEntity recentVictim = commander.getLastHurtMob();
        if (this.isRecentCommanderVictim(commander, recentVictim)) {
            return recentVictim;
        }

        Mob closestThreat = null;
        double closestDistance = Double.MAX_VALUE;
        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, commander.getBoundingBox().inflate(FRIENDLY_ASSIST_RADIUS))) {
            if (mob.getTarget() != commander || !this.canAssistAgainst(mob)) {
                continue;
            }

            double distance = this.distanceToSqr(mob);
            if (distance < closestDistance) {
                closestThreat = mob;
                closestDistance = distance;
            }
        }

        return closestThreat;
    }

    private LivingEntity findSelfDefenseTarget() {
        LivingEntity recentAttacker = this.getLastHurtByMob();
        if (this.isRecentSelfDefenseTarget(recentAttacker)) {
            return recentAttacker;
        }

        LivingEntity collectiveTarget = this.getCollectiveAggroTarget();
        if (this.isRecentCollectiveAggroTarget(collectiveTarget)) {
            return collectiveTarget;
        }

        return null;
    }

    private LivingEntity findPeacefulHostileTarget() {
        Player closest = null;
        double closestDistance = Double.MAX_VALUE;
        double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        double searchRadius = followRange > 0.0D ? followRange : FRIENDLY_ASSIST_RADIUS;

        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(searchRadius))) {
            if (!this.canAssistAgainst(player) || !this.getSensing().hasLineOfSight(player)) {
                continue;
            }

            double distance = this.distanceToSqr(player);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }

        return closest;
    }

    private boolean isRecentCommanderTarget(LivingEntity commander, LivingEntity target, int timestamp) {
        return target != null
                && commander.tickCount - timestamp <= COMMANDER_COMBAT_MEMORY_TICKS
                && commander.distanceToSqr(target) <= FRIENDLY_ASSIST_RADIUS_SQR
                && this.canAssistAgainst(target);
    }

    private boolean isRecentCommanderAttacker(LivingEntity commander, LivingEntity target) {
        return target == commander.getLastHurtByMob()
                && this.isRecentCommanderTarget(commander, target, commander.getLastHurtByMobTimestamp());
    }

    private boolean isRecentCommanderVictim(LivingEntity commander, LivingEntity target) {
        return target == commander.getLastHurtMob()
                && this.isRecentCommanderTarget(commander, target, commander.getLastHurtMobTimestamp());
    }

    private boolean isRecentSelfDefenseTarget(LivingEntity target) {
        return target == this.getLastHurtByMob()
                && target != null
                && this.tickCount - this.getLastHurtByMobTimestamp() <= SELF_DEFENSE_MEMORY_TICKS
                && this.distanceToSqr(target) <= FRIENDLY_ASSIST_RADIUS_SQR
                && this.canAssistAgainst(target);
    }

    private boolean isRecentCollectiveAggroTarget(LivingEntity target) {
        return target != null
                && target == this.collectiveAggroTarget
                && this.tickCount - this.collectiveAggroTimestamp <= COLLECTIVE_HATE_MEMORY_TICKS
                && this.canAssistAgainst(target);
    }

    private boolean canKeepFriendlyAttackTarget(LivingEntity commander, LivingEntity target) {
        return this.canAssistAgainst(target)
                && (this.isRecentSelfDefenseTarget(target)
                || this.isRecentCollectiveAggroTarget(target)
                || this.isRecentCommanderAttacker(commander, target)
                || this.isRecentCommanderVictim(commander, target)
                || target instanceof Mob mob && (mob.getTarget() == this || mob.getTarget() == commander));
    }

    private void updateCommanderIdleState(LivingEntity commander) {
        UUID commanderUuid = commander.getUUID();
        Vec3 currentPosition = commander.position();
        if (!commanderUuid.equals(this.trackedCommanderUuid)) {
            this.trackedCommanderUuid = commanderUuid;
            this.lastCommanderPosition = currentPosition;
            this.commanderIdleTicks = 0;
            return;
        }

        boolean still = this.lastCommanderPosition.distanceToSqr(currentPosition) <= COMMANDER_IDLE_MOVEMENT_EPSILON_SQR
                && commander.getDeltaMovement().lengthSqr() <= COMMANDER_IDLE_MOVEMENT_EPSILON_SQR;
        this.commanderIdleTicks = still ? this.commanderIdleTicks + 1 : 0;
        this.lastCommanderPosition = currentPosition;
    }

    private void resetCommanderIdleState() {
        this.commanderIdleTicks = 0;
        this.trackedCommanderUuid = null;
        this.lastCommanderPosition = Vec3.ZERO;
    }

    private boolean canAssistAgainst(LivingEntity target) {
        return target != null
                && target.isAlive()
                && target != this
                && this.canHoldGrudgeAgainst(target)
                && !this.isAlliedTo(target);
    }

    private boolean canHoldGrudgeAgainst(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return target != null;
        }

        return !player.isCreative() && !player.isSpectator();
    }

    private LivingEntity resolveAggressor(DamageSource source) {
        Entity directEntity = source.getEntity();
        if (directEntity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        Entity owner = source.getDirectEntity();
        return owner instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    private void broadcastCollectiveHate(LivingEntity attacker) {
        this.acceptCollectiveHate(attacker);

        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        for (Entity entity : serverLevel.getAllEntities()) {
            if (!(entity instanceof ShadowNinjaEntity ninja) || ninja == this || !ninja.isAlive()) {
                continue;
            }

            if (ninja.isFriendlyToPlayers() != this.isFriendlyToPlayers()) {
                continue;
            }

            UUID legionSummonerUuid = this.getLegionSummonerUUID().orElse(null);
            if (legionSummonerUuid != null
                    && !legionSummonerUuid.equals(ninja.getLegionSummonerUUID().orElse(null))) {
                continue;
            }

            ninja.acceptCollectiveHate(attacker);
        }
    }

    private void acceptCollectiveHate(LivingEntity attacker) {
        if (!this.canAssistAgainst(attacker)) {
            return;
        }

        this.collectiveAggroTarget = attacker;
        this.collectiveAggroTimestamp = this.tickCount;
        this.summonPoseLocked = false;
        this.setKneeling(false);
        if (!this.isTransitioning()) {
            this.setTarget(attacker);
        }
    }

    private LivingEntity getCollectiveAggroTarget() {
        if (this.collectiveAggroTarget == null) {
            return null;
        }

        if (!this.collectiveAggroTarget.isAlive() || this.collectiveAggroTarget.level() != this.level()) {
            this.collectiveAggroTarget = null;
            this.collectiveAggroTimestamp = 0;
            return null;
        }

        return this.collectiveAggroTarget;
    }

    private void beginSummonCeremony() {
        if (this.hasImmediateCombatPressure()) {
            this.summonPoseLocked = false;
            return;
        }

        this.summonPoseLocked = true;
    }

    private boolean isInSummonCeremony() {
        return this.summonPoseLocked;
    }

    private boolean shouldKeepSummonPose(LivingEntity commander) {
        if (!this.summonPoseLocked) {
            return false;
        }

        if (commander == null
                || this.distanceToSqr(commander) > MUSTER_RADIUS_SQR
                || this.hasImmediateCombatPressure()) {
            this.summonPoseLocked = false;
            return false;
        }

        this.getNavigation().stop();
        return true;
    }

    private boolean hasImmediateCombatPressure() {
        LivingEntity commander = this.getCommanderAnchor();
        return this.findSelfDefenseTarget() != null
                || (commander != null && this.findAssistTarget(commander) != null);
    }

    private boolean shouldClearTargetDuringTransition() {
        return !this.isShadowRushing();
    }

    private void tryStartShadowRush() {
        if (this.shadowRushCooldownTicks > 0
                || this.isTransitioning()
                || this.isInSummonCeremony()
                || this.isPerformingJumpKick()
                || this.isInWater()
                || !this.onGround()) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null
                && target.isAlive()
                && this.distanceToSqr(target) > SHADOW_RUSH_TARGET_TRIGGER_DISTANCE_SQR) {
            Vec3 destination = this.findShadowRushPositionNearTarget(target);
            if (destination != null) {
                this.startShadowRush(destination);
            }
            return;
        }

        LivingEntity commander = this.getCommanderAnchor();
        if (commander != null
                && target == null
                && this.distanceToSqr(commander) > SHADOW_RUSH_COMMANDER_TRIGGER_DISTANCE_SQR) {
            Vec3 destination = this.findShadowRushPositionNearCommander(commander);
            if (destination != null) {
                this.startShadowRush(destination);
            }
        }
    }

    private void startShadowRush(Vec3 destination) {
        this.shadowRushDestinationX = destination.x;
        this.shadowRushDestinationY = destination.y;
        this.shadowRushDestinationZ = destination.z;
        this.transitionStartY = this.getY();
        this.transitionTargetY = this.getY() - SHADOW_RUSH_BURIED_OFFSET;
        this.shadowRushCooldownTicks = SHADOW_RUSH_COOLDOWN_TICKS;
        this.startTransition(COMMAND_STATE_SHADOW_RUSH_SINK, SHADOW_RUSH_SINK_TICKS, false);
    }

    private void beginShadowRushRise() {
        this.setPos(this.shadowRushDestinationX, this.shadowRushDestinationY - SUMMON_BURIED_OFFSET, this.shadowRushDestinationZ);
        this.transitionStartY = this.getY();
        this.transitionTargetY = this.shadowRushDestinationY;
        this.orientTowardsShadowRushFocus();
        this.startTransition(COMMAND_STATE_SHADOW_RUSH_RISE, SHADOW_RUSH_RISE_TICKS, false);
    }

    private void orientTowardsShadowRushFocus() {
        Entity focus = this.getTarget();
        if (!(focus instanceof LivingEntity) || !focus.isAlive()) {
            focus = this.getCommanderAnchor();
        }

        if (focus == null) {
            return;
        }

        double dx = focus.getX() - this.getX();
        double dz = focus.getZ() - this.getZ();
        if (dx * dx + dz * dz < 1.0E-4D) {
            return;
        }

        float yaw = (float) (Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    private Vec3 findShadowRushPositionNearTarget(LivingEntity target) {
        Vec3 preferredDirection = this.position().subtract(target.position());
        if (preferredDirection.horizontalDistanceSqr() < 1.0E-4D) {
            preferredDirection = target.getLookAngle().reverse();
        }

        return this.findShadowRushPositionAround(target, SHADOW_RUSH_TARGET_OFFSET, preferredDirection);
    }

    private Vec3 findShadowRushPositionNearCommander(LivingEntity commander) {
        Vec3 look = commander.getLookAngle();
        Vec3 preferredDirection = new Vec3(-look.x, 0.0D, -look.z);
        return this.findShadowRushPositionAround(commander, SHADOW_RUSH_OWNER_OFFSET, preferredDirection);
    }

    private Vec3 findShadowRushPositionAround(Entity anchor, double preferredDistance, Vec3 preferredDirection) {
        Vec3 originalPos = this.position();
        float originalYRot = this.getYRot();
        float originalXRot = this.getXRot();
        Vec3 flatDirection = new Vec3(preferredDirection.x, 0.0D, preferredDirection.z);
        if (flatDirection.lengthSqr() < 1.0E-4D) {
            float angle = this.random.nextFloat() * Mth.TWO_PI;
            flatDirection = new Vec3(Mth.cos(angle), 0.0D, Mth.sin(angle));
        } else {
            flatDirection = flatDirection.normalize();
        }

        try {
            for (double radiusScale : SHADOW_RUSH_RADIUS_VARIATIONS) {
                double radius = preferredDistance * radiusScale;
                for (float angleOffset : SHADOW_RUSH_ANGLE_OFFSETS) {
                    Vec3 direction = flatDirection.yRot((float) Math.toRadians(angleOffset));
                    double x = anchor.getX() + direction.x * radius;
                    double z = anchor.getZ() + direction.z * radius;
                    int baseY = Mth.floor(anchor.getY());
                    for (int yOffset : SHADOW_RUSH_Y_SEARCH_OFFSETS) {
                        double y = baseY + yOffset;
                        this.moveTo(x, y, z, originalYRot, originalXRot);

                        BlockPos feetPos = BlockPos.containing(x, y, z);
                        if (!this.level().getBlockState(feetPos.below()).blocksMotion()) {
                            continue;
                        }

                        if (this.level().noCollision(this)) {
                            return new Vec3(x, y, z);
                        }
                    }
                }
            }

            return null;
        } finally {
            this.moveTo(originalPos.x, originalPos.y, originalPos.z, originalYRot, originalXRot);
        }
    }

    private boolean canStartJumpKick() {
        if (this.jumpKickCooldownTicks > 0 || this.isPerformingJumpKick() || this.isTransitioning() || !this.onGround() || this.isInWater()) {
            return false;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        double distanceToTargetSqr = this.distanceToSqr(target);
        if (distanceToTargetSqr < JUMP_KICK_MIN_DISTANCE_SQR || distanceToTargetSqr > JUMP_KICK_MAX_DISTANCE_SQR) {
            return false;
        }

        if (Math.abs(target.getY() - this.getY()) > JUMP_KICK_VERTICAL_RANGE) {
            return false;
        }

        return this.getSensing().hasLineOfSight(target);
    }

    private void startJumpKick() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }

        Vec3 horizontalOffset = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        if (horizontalOffset.lengthSqr() < 1.0E-4D) {
            return;
        }

        Vec3 leapDirection = horizontalOffset.normalize();
        double leapStrength = 0.8D + Math.min(horizontalOffset.length() * 0.08D, 0.35D);

        this.getNavigation().stop();
        this.setKneeling(false);
        this.setDeltaMovement(leapDirection.x * leapStrength, JUMP_KICK_Y_MOTION, leapDirection.z * leapStrength);
        this.hasImpulse = true;
        this.jumpKickActiveTicks = JUMP_KICK_ACTIVE_TICKS;
        this.jumpKickCooldownTicks = JUMP_KICK_COOLDOWN_TICKS;
        this.jumpKickHitApplied = false;
        this.triggerAnim(ATTACK_CONTROLLER, JUMP_KICK_ANIMATION);
    }

    private void tickJumpKickAttack() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (!this.jumpKickHitApplied && this.distanceToSqr(target) <= this.getJumpKickHitDistanceSqr(target)) {
            this.jumpKickHitApplied = this.doHurtTarget(target);
        }
    }

    private double getJumpKickHitDistanceSqr(LivingEntity target) {
        double reach = 1.2D + this.getBbWidth() + target.getBbWidth();
        return reach * reach;
    }

    private class FriendlyFollowCommanderGoal extends Goal {
        private FriendlyFollowCommanderGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!ShadowNinjaEntity.this.isFriendlyToPlayers()
                    || ShadowNinjaEntity.this.getTarget() != null
                    || ShadowNinjaEntity.this.isInSummonCeremony()
                    || ShadowNinjaEntity.this.isPerformingJumpKick()
                    || ShadowNinjaEntity.this.isTransitioning()) {
                return false;
            }

            LivingEntity commander = ShadowNinjaEntity.this.getCommanderAnchor();
            if (commander == null) {
                return false;
            }

            double distanceSqr = ShadowNinjaEntity.this.distanceToSqr(commander);
            return distanceSqr > MUSTER_RADIUS_SQR;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            ShadowNinjaEntity.this.setKneeling(false);
        }

        @Override
        public void stop() {
            ShadowNinjaEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity commander = ShadowNinjaEntity.this.getCommanderAnchor();
            if (commander == null) {
                return;
            }

            ShadowNinjaEntity.this.getLookControl().setLookAt(commander, 20.0F, 20.0F);
            double distanceSqr = ShadowNinjaEntity.this.distanceToSqr(commander);
            if (distanceSqr > MUSTER_RADIUS_SQR) {
                ShadowNinjaEntity.this.getNavigation().moveTo(commander, FOLLOW_MOVE_SPEED);
            } else {
                ShadowNinjaEntity.this.getNavigation().stop();
            }
        }
    }
}
