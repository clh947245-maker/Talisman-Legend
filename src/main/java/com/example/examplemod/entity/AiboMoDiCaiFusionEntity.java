package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class AiboMoDiCaiFusionEntity extends MoDiCaiEntity {
    public static final String FUSION_RIDER_TAG = "ChenModFusionAiboRider";
    private static final String PROVOKED_PLAYER_UUID_TAG = "ProvokedPlayerUuid";
    private static final double LASER_RANGE = 36.0D;
    private static final float LASER_DAMAGE = 9.0F;
    private static final double LASER_KNOCKBACK = 0.42D;
    private static final double BLOCK_BREAK_STEP = 0.30D;
    private static final int LASER_COOLDOWN_TICKS = 20 * 8;
    private static final int LASER_COOLDOWN_JITTER_TICKS = 12;
    private static final double FLIGHT_BASE_ALTITUDE = 8.5D;
    private static final double FLIGHT_ALTITUDE_SWAY = 2.1D;
    private static final double FLIGHT_ORBIT_RADIUS = 10.0D;
    private static final double FLIGHT_HORIZONTAL_SPEED = 0.24D;
    private static final double FLIGHT_VERTICAL_SPEED = 0.165D;
    private static final int TACTIC_REFRESH_MIN_TICKS = 20;
    private static final int TACTIC_REFRESH_MAX_TICKS = 55;
    private static final int IDLE_REPATH_MIN_TICKS = 35;
    private static final int IDLE_REPATH_MAX_TICKS = 90;
    private static final int THREAT_SCAN_INTERVAL_TICKS = 10;
    private static final double IDLE_WALK_SPEED = 0.8D;
    private static final double IDLE_WALK_RADIUS = 8.0D;
    private static final double IDLE_FLIGHT_RADIUS = 10.0D;
    private static final double IDLE_FLIGHT_MIN_ALTITUDE = 2.0D;
    private static final double IDLE_FLIGHT_MAX_ALTITUDE = 5.5D;
    private static final int VILLAGE_BOUNDARY_PADDING = 4;
    private static final int VILLAGE_DEFENSE_PADDING = 12;
    private static final String VILLAGE_ANCHOR_SET_TAG = "VillageAnchorSet";
    private static final String VILLAGE_ANCHOR_X_TAG = "VillageAnchorX";
    private static final String VILLAGE_ANCHOR_Y_TAG = "VillageAnchorY";
    private static final String VILLAGE_ANCHOR_Z_TAG = "VillageAnchorZ";
    private static final String VILLAGE_MIN_X_TAG = "VillageMinX";
    private static final String VILLAGE_MAX_X_TAG = "VillageMaxX";
    private static final String VILLAGE_MIN_Z_TAG = "VillageMinZ";
    private static final String VILLAGE_MAX_Z_TAG = "VillageMaxZ";

    private int laserCooldownTicks;
    private int tacticRefreshTicks;
    private int orbitDirection = 1;
    private double orbitRadius = FLIGHT_ORBIT_RADIUS;
    private double orbitOffset;
    private boolean riderSpawned;
    private boolean fusionDefeated;
    private int riderMissingTicks;
    private int idleRepathTicks;
    private boolean idleFlying;
    private Vec3 idleDestination = Vec3.ZERO;
    private boolean villageAnchorSet;
    private BlockPos villageAnchorPos = BlockPos.ZERO;
    private int villageMinX;
    private int villageMaxX;
    private int villageMinZ;
    private int villageMaxZ;
    private @Nullable UUID provokedPlayerUuid;

    public AiboMoDiCaiFusionEntity(EntityType<? extends AiboMoDiCaiFusionEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.chen_mod.mo_di_cai"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MoDiCaiEntity.createAttributes();
    }

    public static boolean checkSpawnRules(
            EntityType<AiboMoDiCaiFusionEntity> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random) {
        return level.getDifficulty().getId() > 0 && Mob.checkMobSpawnRules(entityType, level, spawnType, pos, random);
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData groupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.captureVillageBounds(level, this.blockPosition());
        return groupData;
    }

    @Override
    protected void tickGroundCombatBehavior() {
        if (this.fusionDefeated) {
            super.tickGroundCombatBehavior();
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.fusionDefeated || this.isDeadOrDying()) {
            return;
        }

        this.refreshFlightStyle();
        this.ensureAiboRider();

        if (this.laserCooldownTicks > 0) {
            this.laserCooldownTicks--;
        }

        LivingEntity target = this.getTarget();
        if (!isValidCombatTarget(this, target)) {
            target = null;
            this.setTarget(null);
        }

        if (target == null && this.tickCount % THREAT_SCAN_INTERVAL_TICKS == 0) {
            target = this.findVillageThreatTarget();
            if (target != null) {
                this.setTarget(target);
            }
        }

        if (target == null) {
            this.captureVillageBoundsFromCurrentPosition();
            this.tickIdleRoaming();
        } else {
            this.tickAirCombatMovement(target);
            this.getLookControl().setLookAt(target, 35.0F, 35.0F);

            if (this.canFireLaserAt(target)) {
                this.firePigLaserAt(target);
                this.resetLaserCooldown();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        LivingEntity attacker = this.resolveFusionAttacker(source);
        UUID previousProvokedPlayerUuid = this.provokedPlayerUuid;
        if (attacker instanceof Player player && this.canRemainHostileTo(player)) {
            this.provokedPlayerUuid = player.getUUID();
        }

        boolean didHurt = super.hurt(source, amount);
        if (!didHurt) {
            this.provokedPlayerUuid = previousProvokedPlayerUuid;
        }
        return didHurt;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof Player player && !this.canTargetProvokedPlayer(player)) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        boolean usingFlight = this.shouldUseFlightLocomotion();
        if (usingFlight) {
            this.fallDistance = 0.0F;
            // Pig leg animation should stay frozen while the fusion is airborne.
            this.walkAnimation.setSpeed(0.0F);
        }
        if (!this.level().isClientSide()) {
            this.setNoGravity(usingFlight);
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide()) {
            this.fusionDefeated = true;
            this.releaseFusionRidersForEscape(damageSource);
        }
        super.die(damageSource);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("LaserCooldownTicks", this.laserCooldownTicks);
        compound.putInt("TacticRefreshTicks", this.tacticRefreshTicks);
        compound.putInt("OrbitDirection", this.orbitDirection);
        compound.putDouble("OrbitRadius", this.orbitRadius);
        compound.putDouble("OrbitOffset", this.orbitOffset);
        compound.putBoolean("RiderSpawned", this.riderSpawned);
        compound.putBoolean("FusionDefeated", this.fusionDefeated);
        compound.putInt("RiderMissingTicks", this.riderMissingTicks);
        compound.putInt("IdleRepathTicks", this.idleRepathTicks);
        compound.putBoolean("IdleFlying", this.idleFlying);
        compound.putDouble("IdleDestX", this.idleDestination.x);
        compound.putDouble("IdleDestY", this.idleDestination.y);
        compound.putDouble("IdleDestZ", this.idleDestination.z);
        compound.putBoolean(VILLAGE_ANCHOR_SET_TAG, this.villageAnchorSet);
        if (this.villageAnchorSet) {
            compound.putInt(VILLAGE_ANCHOR_X_TAG, this.villageAnchorPos.getX());
            compound.putInt(VILLAGE_ANCHOR_Y_TAG, this.villageAnchorPos.getY());
            compound.putInt(VILLAGE_ANCHOR_Z_TAG, this.villageAnchorPos.getZ());
            compound.putInt(VILLAGE_MIN_X_TAG, this.villageMinX);
            compound.putInt(VILLAGE_MAX_X_TAG, this.villageMaxX);
            compound.putInt(VILLAGE_MIN_Z_TAG, this.villageMinZ);
            compound.putInt(VILLAGE_MAX_Z_TAG, this.villageMaxZ);
        }
        if (this.provokedPlayerUuid != null) {
            compound.putUUID(PROVOKED_PLAYER_UUID_TAG, this.provokedPlayerUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.laserCooldownTicks = Math.max(0, compound.getInt("LaserCooldownTicks"));
        this.tacticRefreshTicks = Math.max(0, compound.getInt("TacticRefreshTicks"));
        this.orbitDirection = compound.getInt("OrbitDirection") >= 0 ? 1 : -1;
        this.orbitRadius = Mth.clamp(compound.getDouble("OrbitRadius"), 7.0D, 13.0D);
        this.orbitOffset = compound.getDouble("OrbitOffset");
        this.riderSpawned = compound.getBoolean("RiderSpawned");
        this.fusionDefeated = compound.getBoolean("FusionDefeated");
        this.riderMissingTicks = Math.max(0, compound.getInt("RiderMissingTicks"));
        this.idleRepathTicks = Math.max(0, compound.getInt("IdleRepathTicks"));
        this.idleFlying = compound.getBoolean("IdleFlying");
        if (compound.contains("IdleDestX") && compound.contains("IdleDestY") && compound.contains("IdleDestZ")) {
            this.idleDestination = new Vec3(
                    compound.getDouble("IdleDestX"),
                    compound.getDouble("IdleDestY"),
                    compound.getDouble("IdleDestZ"));
        } else {
            this.idleDestination = Vec3.ZERO;
        }
        this.villageAnchorSet = compound.getBoolean(VILLAGE_ANCHOR_SET_TAG);
        if (this.villageAnchorSet) {
            this.villageAnchorPos = new BlockPos(
                    compound.getInt(VILLAGE_ANCHOR_X_TAG),
                    compound.getInt(VILLAGE_ANCHOR_Y_TAG),
                    compound.getInt(VILLAGE_ANCHOR_Z_TAG));
            this.villageMinX = compound.getInt(VILLAGE_MIN_X_TAG);
            this.villageMaxX = compound.getInt(VILLAGE_MAX_X_TAG);
            this.villageMinZ = compound.getInt(VILLAGE_MIN_Z_TAG);
            this.villageMaxZ = compound.getInt(VILLAGE_MAX_Z_TAG);
        } else {
            this.villageAnchorPos = BlockPos.ZERO;
            this.villageMinX = 0;
            this.villageMaxX = 0;
            this.villageMinZ = 0;
            this.villageMaxZ = 0;
        }
        this.provokedPlayerUuid = compound.hasUUID(PROVOKED_PLAYER_UUID_TAG)
                ? compound.getUUID(PROVOKED_PLAYER_UUID_TAG)
                : null;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    private void refreshFlightStyle() {
        if (this.tacticRefreshTicks > 0) {
            this.tacticRefreshTicks--;
            return;
        }

        this.orbitDirection = this.getRandom().nextBoolean() ? 1 : -1;
        this.orbitRadius = FLIGHT_ORBIT_RADIUS + (this.getRandom().nextDouble() * 2.6D - 1.3D);
        this.orbitOffset = this.getRandom().nextDouble() * Mth.TWO_PI;
        this.tacticRefreshTicks = Mth.nextInt(this.getRandom(), TACTIC_REFRESH_MIN_TICKS, TACTIC_REFRESH_MAX_TICKS);
    }

    private void tickIdleRoaming() {
        if (this.villageAnchorSet && !this.isInsideVillageBounds(this.position())) {
            this.idleFlying = !this.onGround();
            this.idleDestination = this.createVillageReturnDestination(this.idleFlying);
            this.idleRepathTicks = Mth.nextInt(this.getRandom(), IDLE_REPATH_MIN_TICKS, IDLE_REPATH_MAX_TICKS);
        }

        if (this.idleRepathTicks > 0) {
            this.idleRepathTicks--;
        }

        if (this.idleRepathTicks <= 0 || this.hasReachedIdleDestination()) {
            this.chooseNextIdleDestination();
        }

        this.idleDestination = this.limitIdleDestinationToVillage(this.idleDestination, this.idleFlying);

        if (this.idleFlying) {
            this.getNavigation().stop();
            this.tickIdleFlightMovement();
            return;
        }

        if (!this.onGround()) {
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(velocity.x * 0.91D, velocity.y, velocity.z * 0.91D);
            return;
        }

        this.getNavigation().moveTo(
                this.idleDestination.x,
                this.idleDestination.y,
                this.idleDestination.z,
                IDLE_WALK_SPEED);
        if (this.idleDestination.distanceToSqr(this.position()) > 1.0E-4D) {
            this.getLookControl().setLookAt(this.idleDestination.x, this.getEyeY(), this.idleDestination.z, 20.0F, 20.0F);
        }
    }

    private void tickAirCombatMovement(@Nullable LivingEntity target) {
        Vec3 desiredPosition = this.computeDesiredFlightPosition(target);
        this.moveTowardAirDestination(desiredPosition, target == null ? 0.06D : 0.12D, target == null ? 0.22D : 0.30D);
    }

    private void tickIdleFlightMovement() {
        this.moveTowardAirDestination(this.idleDestination, 0.05D, 0.18D);
    }

    private void moveTowardAirDestination(Vec3 desiredPosition, double targetSpeedScale, double moveLerp) {
        Vec3 toDesired = desiredPosition.subtract(this.position());
        Vec3 velocity = this.getDeltaMovement();

        double desiredX = Mth.clamp(toDesired.x * targetSpeedScale, -FLIGHT_HORIZONTAL_SPEED, FLIGHT_HORIZONTAL_SPEED);
        double desiredY = Mth.clamp(toDesired.y * targetSpeedScale, -FLIGHT_VERTICAL_SPEED, FLIGHT_VERTICAL_SPEED);
        double desiredZ = Mth.clamp(toDesired.z * targetSpeedScale, -FLIGHT_HORIZONTAL_SPEED, FLIGHT_HORIZONTAL_SPEED);

        double newX = Mth.lerp(moveLerp, velocity.x, desiredX);
        double newY = Mth.lerp(moveLerp, velocity.y, desiredY);
        double newZ = Mth.lerp(moveLerp, velocity.z, desiredZ);

        this.setDeltaMovement(newX, newY, newZ);
        this.hasImpulse = true;

        float yaw;
        if (toDesired.x * toDesired.x + toDesired.z * toDesired.z > 1.0E-6D) {
            yaw = (float) (Mth.atan2(toDesired.z, toDesired.x) * (180.0F / Math.PI)) - 90.0F;
        } else if (newX * newX + newZ * newZ > 1.0E-6D) {
            yaw = (float) (Mth.atan2(newZ, newX) * (180.0F / Math.PI)) - 90.0F;
        } else {
            yaw = this.getYRot();
        }

        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private Vec3 computeDesiredFlightPosition(@Nullable LivingEntity target) {
        if (target != null) {
            double orbitAngle = this.tickCount * 0.085D * this.orbitDirection + this.orbitOffset;
            double wave = Math.sin((this.tickCount + this.getId()) * 0.07D) * 1.2D;
            double radius = this.orbitRadius + wave;
            double x = target.getX() + Mth.cos((float) orbitAngle) * radius;
            double z = target.getZ() + Mth.sin((float) orbitAngle) * radius;
            double y = target.getY() + target.getBbHeight() + FLIGHT_BASE_ALTITUDE
                    + Math.sin((this.tickCount + this.getId()) * 0.045D) * FLIGHT_ALTITUDE_SWAY;
            return new Vec3(x, y, z);
        }

        double idleWave = Math.sin((this.tickCount + this.getId()) * 0.05D) * 0.14D;
        return new Vec3(this.getX(), this.getY() + idleWave, this.getZ());
    }

    private void chooseNextIdleDestination() {
        this.idleFlying = this.getRandom().nextBoolean();
        this.idleDestination = this.idleFlying ? this.sampleIdleFlightDestination() : this.sampleIdleWalkDestination();
        this.idleRepathTicks = Mth.nextInt(this.getRandom(), IDLE_REPATH_MIN_TICKS, IDLE_REPATH_MAX_TICKS);
    }

    private Vec3 sampleIdleWalkDestination() {
        if (this.villageAnchorSet) {
            return this.sampleVillageDestination(false);
        }

        float angle = this.getRandom().nextFloat() * Mth.TWO_PI;
        double distance = 2.5D + this.getRandom().nextDouble() * IDLE_WALK_RADIUS;
        double x = this.getX() + Mth.cos(angle) * distance;
        double z = this.getZ() + Mth.sin(angle) * distance;
        return new Vec3(x, this.getY(), z);
    }

    private Vec3 sampleIdleFlightDestination() {
        if (this.villageAnchorSet) {
            return this.sampleVillageDestination(true);
        }

        float angle = this.getRandom().nextFloat() * Mth.TWO_PI;
        double distance = 3.0D + this.getRandom().nextDouble() * IDLE_FLIGHT_RADIUS;
        double x = this.getX() + Mth.cos(angle) * distance;
        double z = this.getZ() + Mth.sin(angle) * distance;
        double y = this.getY() + IDLE_FLIGHT_MIN_ALTITUDE
                + this.getRandom().nextDouble() * (IDLE_FLIGHT_MAX_ALTITUDE - IDLE_FLIGHT_MIN_ALTITUDE);
        return new Vec3(x, y, z);
    }

    private boolean hasReachedIdleDestination() {
        double threshold = this.idleFlying ? 2.0D : 1.8D;
        return this.idleDestination == Vec3.ZERO || this.idleDestination.distanceToSqr(this.position()) <= threshold * threshold;
    }

    private void captureVillageBoundsFromCurrentPosition() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.captureVillageBounds(serverLevel, this.blockPosition());
    }

    private void captureVillageBounds(ServerLevelAccessor level, BlockPos pos) {
        StructureStart village = level.getLevel().structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
        if (!village.isValid()) {
            return;
        }

        BoundingBox bounds = village.getBoundingBox().inflatedBy(VILLAGE_BOUNDARY_PADDING, 0, VILLAGE_BOUNDARY_PADDING);
        this.villageAnchorSet = true;
        this.villageAnchorPos = bounds.getCenter().immutable();
        this.villageMinX = bounds.minX();
        this.villageMaxX = bounds.maxX();
        this.villageMinZ = bounds.minZ();
        this.villageMaxZ = bounds.maxZ();
    }

    private boolean isInsideVillageBounds(Vec3 position) {
        if (!this.villageAnchorSet) {
            return true;
        }

        return position.x >= this.villageMinX
                && position.x <= this.villageMaxX + 1.0D
                && position.z >= this.villageMinZ
                && position.z <= this.villageMaxZ + 1.0D;
    }

    private boolean isInsideVillageDefenseBounds(Vec3 position) {
        if (!this.villageAnchorSet) {
            double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
            return this.distanceToSqr(position) <= followRange * followRange;
        }

        return position.x >= this.villageMinX - VILLAGE_DEFENSE_PADDING
                && position.x <= this.villageMaxX + 1.0D + VILLAGE_DEFENSE_PADDING
                && position.z >= this.villageMinZ - VILLAGE_DEFENSE_PADDING
                && position.z <= this.villageMaxZ + 1.0D + VILLAGE_DEFENSE_PADDING;
    }

    private @Nullable LivingEntity findVillageThreatTarget() {
        double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB scanBox = this.getBoundingBox().inflate(followRange, 10.0D, followRange);
        LivingEntity nearestThreat = null;
        double nearestDistanceSqr = Double.MAX_VALUE;

        for (Mob mob : this.level().getEntitiesOfClass(Mob.class, scanBox, this::isVillageThreat)) {
            double distanceSqr = this.distanceToSqr(mob);
            if (distanceSqr >= nearestDistanceSqr) {
                continue;
            }

            nearestThreat = mob;
            nearestDistanceSqr = distanceSqr;
        }

        return nearestThreat;
    }

    private boolean isVillageThreat(Mob mob) {
        return mob.isAlive()
                && mob != this
                && mob instanceof Enemy
                && this.isInsideVillageDefenseBounds(mob.position());
    }

    private Vec3 limitIdleDestinationToVillage(Vec3 destination, boolean flying) {
        if (!this.villageAnchorSet || destination == Vec3.ZERO) {
            return destination;
        }

        double clampedX = Mth.clamp(destination.x, this.villageMinX + 0.5D, this.villageMaxX + 0.5D);
        double clampedZ = Mth.clamp(destination.z, this.villageMinZ + 0.5D, this.villageMaxZ + 0.5D);
        BlockPos groundPos = this.getVillageGroundPos(clampedX, clampedZ);
        double minY = groundPos.getY() + IDLE_FLIGHT_MIN_ALTITUDE;
        double maxY = groundPos.getY() + IDLE_FLIGHT_MAX_ALTITUDE;
        double clampedY = flying ? Mth.clamp(destination.y, minY, maxY) : groundPos.getY();
        return new Vec3(clampedX, clampedY, clampedZ);
    }

    private Vec3 createVillageReturnDestination(boolean flying) {
        if (!this.villageAnchorSet) {
            return this.position();
        }

        BlockPos groundPos = this.getVillageGroundPos(this.villageAnchorPos.getX() + 0.5D, this.villageAnchorPos.getZ() + 0.5D);
        double y = flying
                ? groundPos.getY() + IDLE_FLIGHT_MIN_ALTITUDE
                        + this.getRandom().nextDouble() * (IDLE_FLIGHT_MAX_ALTITUDE - IDLE_FLIGHT_MIN_ALTITUDE)
                : groundPos.getY();
        return new Vec3(groundPos.getX() + 0.5D, y, groundPos.getZ() + 0.5D);
    }

    private Vec3 sampleVillageDestination(boolean flying) {
        if (this.level() instanceof ServerLevel serverLevel) {
            for (int attempt = 0; attempt < 12; attempt++) {
                int sampleX = Mth.nextInt(this.getRandom(), this.villageMinX, this.villageMaxX);
                int sampleZ = Mth.nextInt(this.getRandom(), this.villageMinZ, this.villageMaxZ);
                BlockPos groundPos = serverLevel.getHeightmapPos(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(sampleX, this.blockPosition().getY(), sampleZ));
                if (!serverLevel.isVillage(groundPos)) {
                    continue;
                }

                double y = flying
                        ? groundPos.getY() + IDLE_FLIGHT_MIN_ALTITUDE
                                + this.getRandom().nextDouble() * (IDLE_FLIGHT_MAX_ALTITUDE - IDLE_FLIGHT_MIN_ALTITUDE)
                        : groundPos.getY();
                return new Vec3(groundPos.getX() + 0.5D, y, groundPos.getZ() + 0.5D);
            }
        }

        return this.createVillageReturnDestination(flying);
    }

    private BlockPos getVillageGroundPos(double x, double z) {
        BlockPos targetPos = BlockPos.containing(x, this.getY(), z);
        return this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos);
    }

    private void ensureAiboRider() {
        if (this.fusionDefeated) {
            return;
        }
        if (this.tickCount % 20 != 0) {
            return;
        }
        this.discardLegacyFusionRiders();
        if (this.getPassengers().stream().anyMatch(AiboMoDiCaiFusionEntity::isFusionAiboRider)) {
            this.riderMissingTicks = 0;
            return;
        }

        if (this.riderSpawned) {
            this.riderMissingTicks++;
            if (this.riderMissingTicks >= 1) {
                this.onFusionRiderDefeated();
            }
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AiboEntity rider = ChenMod.AIBO.get().create(serverLevel);
        if (rider == null) {
            return;
        }

        rider.moveTo(this.getX(), this.getY() + this.getBbHeight() + 0.08D, this.getZ(), this.getYRot(), 0.0F);
        rider.setNoAi(false);
        rider.setInvulnerable(false);
        rider.setSilent(true);
        rider.setPersistenceRequired();
        rider.getPersistentData().putBoolean(FUSION_RIDER_TAG, true);
        rider.setCustomName(Component.translatable("entity.chen_mod.aibo"));
        rider.setCustomNameVisible(false);
        serverLevel.addFreshEntity(rider);
        rider.startRiding(this, true);
        this.riderSpawned = true;
        this.riderMissingTicks = 0;
    }

    private void releaseFusionRidersForEscape(DamageSource source) {
        LivingEntity attacker = this.resolveFusionAttacker(source);
        for (Entity passenger : new ArrayList<>(this.getPassengers())) {
            if (!isFusionAiboRider(passenger)) {
                continue;
            }

            passenger.stopRiding();
            if (passenger instanceof AiboEntity rider) {
                rider.getPersistentData().remove(FUSION_RIDER_TAG);
                rider.setSilent(false);
                rider.triggerImmediateEscape(attacker);
            }
        }
    }

    private void discardLegacyFusionRiders() {
        for (Entity passenger : this.getPassengers()) {
            if (isTaggedFusionChicken(passenger) && !(passenger instanceof AiboEntity)) {
                passenger.discard();
            }
        }
    }

    private static boolean isFusionAiboRider(Entity passenger) {
        return passenger instanceof AiboEntity chicken
                && chicken.getPersistentData().getBoolean(FUSION_RIDER_TAG);
    }

    private static boolean isTaggedFusionChicken(Entity passenger) {
        return passenger instanceof Chicken chicken
                && chicken.getPersistentData().getBoolean(FUSION_RIDER_TAG);
    }

    public void onFusionRiderDefeated() {
        if (this.level().isClientSide() || this.fusionDefeated || this.isDeadOrDying()) {
            return;
        }
        this.fusionDefeated = true;
        this.setTarget(null);
        this.getNavigation().stop();
        this.setNoGravity(false);
        this.setDeltaMovement(Vec3.ZERO);
    }

    private boolean shouldUseFlightLocomotion() {
        if (this.fusionDefeated || this.isDeadOrDying()) {
            return false;
        }
        LivingEntity target = this.getTarget();
        return isValidCombatTarget(this, target) || this.idleFlying || !this.onGround();
    }

    private @Nullable LivingEntity resolveFusionAttacker(DamageSource source) {
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

    private boolean canRemainHostileTo(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return true;
        }

        return !player.isCreative() && !player.isSpectator();
    }

    private boolean canTargetProvokedPlayer(Player player) {
        return this.canRemainHostileTo(player)
                && this.provokedPlayerUuid != null
                && this.provokedPlayerUuid.equals(player.getUUID());
    }

    private static boolean isValidCombatTarget(AiboMoDiCaiFusionEntity self, @Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && target.level() == self.level()
                && self.canRemainHostileTo(target)
                && (!(target instanceof Enemy) || self.isInsideVillageDefenseBounds(target.position()))
                && (!(target instanceof Player player) || self.canTargetProvokedPlayer(player));
    }

    private boolean canFireLaserAt(@Nullable LivingEntity target) {
        return isValidCombatTarget(this, target)
                && this.laserCooldownTicks <= 0
                && this.hasLineOfSight(target)
                && this.distanceToSqr(target) <= LASER_RANGE * LASER_RANGE;
    }

    private void resetLaserCooldown() {
        int jitter = this.getRandom().nextInt(LASER_COOLDOWN_JITTER_TICKS * 2 + 1) - LASER_COOLDOWN_JITTER_TICKS;
        this.laserCooldownTicks = Math.max(20, LASER_COOLDOWN_TICKS + jitter);
    }

    @Nullable
    private Vec3 computeLaserAimDirection(LivingEntity target) {
        Vec3 aimPoint = target.getEyePosition().subtract(0.0D, target.getBbHeight() * 0.18D, 0.0D);
        Vec3 aimDirection = aimPoint.subtract(this.getEyePosition());
        if (aimDirection.lengthSqr() < 1.0E-6D) {
            return null;
        }
        return aimDirection.normalize();
    }

    private void firePigLaserAt(LivingEntity target) {
        Vec3 look = this.computeLaserAimDirection(target);
        if (look == null) {
            return;
        }

        this.getLookControl().setLookAt(target, 35.0F, 35.0F);
        Vec3 leftStart = computeEyeStart(this, look, -1.0D);
        Vec3 rightStart = computeEyeStart(this, look, 1.0D);
        Vec3 leftEnd = resolveBeamEnd(leftStart, look);
        Vec3 rightEnd = resolveBeamEnd(rightStart, look);

        Set<BlockPos> brokenBlocks = new HashSet<>();
        breakBlocksAlongBeam(this.level(), this, leftStart, leftEnd, brokenBlocks);
        breakBlocksAlongBeam(this.level(), this, rightStart, rightEnd, brokenBlocks);

        Set<Integer> hitEntityIds = new HashSet<>();
        damageEntitiesAlongBeam(this.level(), this, leftStart, leftEnd, hitEntityIds);
        damageEntitiesAlongBeam(this.level(), this, rightStart, rightEnd, hitEntityIds);

        this.level().addFreshEntity(new PigLaserEntity(this.level(), leftStart, leftEnd, rightStart, rightEnd));
    }

    private static Vec3 computeEyeStart(LivingEntity caster, Vec3 look, double side) {
        Vec3 up = caster.getUpVector(1.0F).normalize();
        Vec3 right = up.cross(look);
        if (right.lengthSqr() < 1.0E-7D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        Vec3 eyeCenter = caster.getEyePosition()
                .add(look.scale(0.25D))
                .add(up.scale(-0.10D));
        return eyeCenter.add(right.scale(0.19D * side));
    }

    private static Vec3 resolveBeamEnd(Vec3 start, Vec3 direction) {
        return start.add(direction.scale(LASER_RANGE));
    }

    private static void breakBlocksAlongBeam(Level level, LivingEntity caster, Vec3 start, Vec3 end, Set<BlockPos> brokenBlocks) {
        Vec3 delta = end.subtract(start);
        double totalLength = delta.length();
        if (totalLength < 1.0E-7D) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / totalLength);
        int samples = Math.max(1, Mth.ceil(totalLength / BLOCK_BREAK_STEP));
        for (int i = 0; i <= samples; i++) {
            double traveled = Math.min(totalLength, i * BLOCK_BREAK_STEP);
            BlockPos blockPos = BlockPos.containing(start.add(direction.scale(traveled))).immutable();
            if (!brokenBlocks.add(blockPos)) {
                continue;
            }

            if (canLaserBreakBlock(level, caster, blockPos)) {
                level.destroyBlock(blockPos, false, caster);
            }
        }
    }

    private static boolean canLaserBreakBlock(Level level, LivingEntity caster, BlockPos blockPos) {
        BlockState state = level.getBlockState(blockPos);
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && !state.getCollisionShape(level, blockPos).isEmpty()
                && state.getDestroySpeed(level, blockPos) >= 0.0F
                && state.canEntityDestroy(level, blockPos, caster);
    }

    private static void damageEntitiesAlongBeam(Level level, LivingEntity caster, Vec3 start, Vec3 end, Set<Integer> hitEntityIds) {
        AABB beamBox = new AABB(start, end).inflate(0.8D);
        Vec3 knockbackDirection = end.subtract(start);
        if (knockbackDirection.lengthSqr() > 1.0E-7D) {
            knockbackDirection = knockbackDirection.normalize();
        } else {
            knockbackDirection = caster.getLookAngle().normalize();
        }

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, beamBox, entity ->
                entity.isAlive() && entity != caster && !entity.isSpectator());

        for (LivingEntity target : targets) {
            if (!target.getBoundingBox().inflate(0.35D).clip(start, end).isPresent()) {
                continue;
            }
            if (!hitEntityIds.add(target.getId())) {
                continue;
            }

            target.hurt(caster.damageSources().mobAttack(caster), LASER_DAMAGE);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.15D));
            target.push(knockbackDirection.x * LASER_KNOCKBACK, 0.035D, knockbackDirection.z * LASER_KNOCKBACK);
            target.hurtMarked = true;
        }
    }
}
