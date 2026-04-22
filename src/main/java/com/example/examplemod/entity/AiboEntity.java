package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class AiboEntity extends Chicken {
    private static final int ESCAPE_FLIGHT_TICKS = 70;
    private static final double ESCAPE_HORIZONTAL_SPEED = 0.48D;
    private static final double ESCAPE_VERTICAL_BOOST = 0.08D;
    private static final double ESCAPE_VERTICAL_BOOST_END = 0.025D;
    private static final double ESCAPE_VERTICAL_SPEED_CAP = 0.55D;
    private static final double ESCAPE_SAFE_DISTANCE = 24.0D;
    private static final int ESCAPE_REPLENISH_TICKS = 14;
    private static final EntityDataAccessor<Boolean> DATA_ESCAPE_FLYING =
            SynchedEntityData.defineId(AiboEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
    private int escapeFlightTicks;
    private boolean avoidPlayersAfterHurt;
    private @Nullable UUID escapeAttackerUuid;
    private Vec3 escapeDirection = Vec3.ZERO;

    public AiboEntity(EntityType<? extends AiboEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ESCAPE_FLYING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Chicken.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean didHurt = super.hurt(source, amount);
        if (didHurt && !this.level().isClientSide() && !this.isDeadOrDying()) {
            this.startEscapeFlight(this.resolveAttacker(source));
        }
        return didHurt;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isFusionRiderMounted()) {
            // Keep the fusion rider visually fixed on Mo Di Cai instead of flapping.
            this.flap = 0.0F;
            this.oFlap = 0.0F;
            this.flapSpeed = 0.0F;
            this.oFlapSpeed = 0.0F;
            this.flapping = 0.0F;
            this.walkAnimation.setSpeed(0.0F);
        }

        if (this.isEscapeFlying()) {
            // Keep wings folded while escaping in flight.
            this.flap = 0.0F;
            this.oFlap = 0.0F;
            this.flapSpeed = 0.0F;
            this.oFlapSpeed = 0.0F;
            this.flapping = 1.0F;
            // Prevent vanilla chicken leg-walk animation while flying.
            this.walkAnimation.setSpeed(0.0F);
        }

        if (this.level().isClientSide()) {
            return;
        }

        if (this.avoidPlayersAfterHurt) {
            Player threateningPlayer = this.getNearbyThreateningPlayer();
            if (threateningPlayer != null) {
                this.escapeAttackerUuid = threateningPlayer.getUUID();
                this.escapeFlightTicks = Math.max(this.escapeFlightTicks, ESCAPE_REPLENISH_TICKS);
                this.setEscapeFlying(true);
            } else if (this.escapeFlightTicks <= 0) {
                this.avoidPlayersAfterHurt = false;
            }
        }

        if (this.escapeFlightTicks <= 0) {
            this.setEscapeFlying(false);
            this.setSprinting(false);
            return;
        }

        this.escapeFlightTicks--;
        if (this.escapeFlightTicks <= 0) {
            this.setEscapeFlying(false);
        }
        this.getNavigation().stop();
        this.setSprinting(true);
        this.fallDistance = 0.0F;

        LivingEntity attacker = this.getEscapeAttacker();
        if (attacker != null) {
            Vec3 awayFromAttacker = normalizeHorizontal(this.position().subtract(attacker.position()));
            if (awayFromAttacker != Vec3.ZERO) {
                this.escapeDirection = awayFromAttacker;
            }
        }
        if (this.escapeDirection == Vec3.ZERO) {
            this.escapeDirection = this.getRandomEscapeDirection();
        }

        if (this.onGround()) {
            this.jumpFromGround();
        }

        Vec3 motion = this.getDeltaMovement();
        double boostScale = this.escapeFlightTicks / (double) ESCAPE_FLIGHT_TICKS;
        double verticalBoost = Mth.lerp(boostScale, ESCAPE_VERTICAL_BOOST_END, ESCAPE_VERTICAL_BOOST);
        double targetX = this.escapeDirection.x * ESCAPE_HORIZONTAL_SPEED;
        double targetZ = this.escapeDirection.z * ESCAPE_HORIZONTAL_SPEED;
        double newX = Mth.lerp(0.3D, motion.x, targetX);
        double newY = Math.min(ESCAPE_VERTICAL_SPEED_CAP, motion.y + verticalBoost);
        double newZ = Mth.lerp(0.3D, motion.z, targetZ);

        this.setDeltaMovement(newX, newY, newZ);
        this.hasImpulse = true;

        float yaw = (float) (Mth.atan2(-this.escapeDirection.x, this.escapeDirection.z) * (180.0F / Math.PI));
        this.setYRot(yaw);
        this.yBodyRot = yaw;
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide()
                && this.getPersistentData().getBoolean(AiboMoDiCaiFusionEntity.FUSION_RIDER_TAG)
                && this.getVehicle() instanceof AiboMoDiCaiFusionEntity fusion) {
            fusion.onFusionRiderDefeated();
        }
        super.die(damageSource);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        this.spawnAtLocation(ChenMod.ROOSTER_TALISMAN.get());
    }

    public void triggerImmediateEscape(@Nullable LivingEntity attacker) {
        if (this.level().isClientSide() || this.isDeadOrDying()) {
            return;
        }
        this.startEscapeFlight(attacker);
    }

    private void startEscapeFlight(@Nullable LivingEntity attacker) {
        this.escapeFlightTicks = ESCAPE_FLIGHT_TICKS;
        this.avoidPlayersAfterHurt = true;
        this.setEscapeFlying(true);

        if (attacker != null) {
            this.escapeAttackerUuid = attacker.getUUID();
            Vec3 awayFromAttacker = normalizeHorizontal(this.position().subtract(attacker.position()));
            this.escapeDirection = awayFromAttacker == Vec3.ZERO ? this.getRandomEscapeDirection() : awayFromAttacker;
        } else {
            this.escapeAttackerUuid = null;
            Vec3 look = this.getLookAngle();
            Vec3 opposite = normalizeHorizontal(new Vec3(-look.x, 0.0D, -look.z));
            this.escapeDirection = opposite == Vec3.ZERO ? this.getRandomEscapeDirection() : opposite;
        }
    }

    private @Nullable LivingEntity resolveAttacker(DamageSource source) {
        Entity trueSource = source.getEntity();
        if (trueSource instanceof LivingEntity living) {
            return living;
        }

        Entity directSource = source.getDirectEntity();
        if (directSource instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    private @Nullable LivingEntity getEscapeAttacker() {
        if (this.escapeAttackerUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Entity attacker = serverLevel.getEntity(this.escapeAttackerUuid);
        if (attacker instanceof LivingEntity living && living.isAlive()) {
            return living;
        }

        this.escapeAttackerUuid = null;
        return null;
    }

    private @Nullable Player getNearbyThreateningPlayer() {
        Player nearest = this.level().getNearestPlayer(this, ESCAPE_SAFE_DISTANCE);
        if (nearest == null || !this.isThreateningPlayer(nearest)) {
            return null;
        }
        return nearest;
    }

    private Vec3 getRandomEscapeDirection() {
        float yawDegrees = this.getYRot() + 140.0F + this.getRandom().nextFloat() * 80.0F;
        float yawRadians = yawDegrees * ((float) Math.PI / 180.0F);
        return new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians));
    }

    private static Vec3 normalizeHorizontal(Vec3 vec) {
        double horizontalLengthSqr = vec.x * vec.x + vec.z * vec.z;
        if (horizontalLengthSqr < 1.0E-6D) {
            return Vec3.ZERO;
        }
        double invLength = 1.0D / Math.sqrt(horizontalLengthSqr);
        return new Vec3(vec.x * invLength, 0.0D, vec.z * invLength);
    }

    private boolean isThreateningPlayer(Player player) {
        return player != null && player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private boolean isEscapeFlying() {
        return this.entityData.get(DATA_ESCAPE_FLYING);
    }

    private boolean isFusionRiderMounted() {
        return this.getVehicle() instanceof AiboMoDiCaiFusionEntity;
    }

    private void setEscapeFlying(boolean value) {
        this.entityData.set(DATA_ESCAPE_FLYING, value);
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
