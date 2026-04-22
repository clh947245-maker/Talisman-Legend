package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MoDiCaiEntity extends Pig {
    private static final double LASER_RANGE = 32.0D;
    private static final float LASER_DAMAGE = 8.0F;
    private static final double LASER_KNOCKBACK = 0.35D;
    private static final double BLOCK_BREAK_STEP = 0.30D;
    private static final int LASER_COOLDOWN_TICKS = 20 * 8;
    private static final int LASER_COOLDOWN_JITTER_TICKS = 20;
    private static final double COMBAT_MIN_DISTANCE = 7.0D;
    private static final double COMBAT_MAX_DISTANCE = 14.0D;
    private static final double COMBAT_MIN_JITTER = 2.0D;
    private static final double COMBAT_MAX_JITTER = 3.0D;
    private static final double RETREAT_STEP_DISTANCE = 10.0D;
    private static final int RETREAT_REPATH_INTERVAL = 2;
    private static final int CHASE_REPATH_INTERVAL = 2;
    private static final double CHASE_SPEED = 0.92D;
    private static final double ORBIT_SPEED = 0.72D;
    private static final double RETREAT_SPEED = 0.98D;
    private static final double ORBIT_RADIUS = 6.0D;
    private static final int ORBIT_REPATH_MIN_TICKS = 10;
    private static final int ORBIT_REPATH_MAX_TICKS = 20;
    private static final int ORBIT_SKIP_MIN_TICKS = 8;
    private static final int ORBIT_SKIP_MAX_TICKS = 14;
    private static final float ORBIT_SKIP_CHANCE = 0.55F;
    private static final int TACTIC_REFRESH_MIN_TICKS = 20;
    private static final int TACTIC_REFRESH_MAX_TICKS = 60;

    protected final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
    private int laserCooldownTicks;
    private int retreatRepathTicks;
    private int chaseRepathTicks;
    private int orbitRepathTicks;
    private int tacticRefreshTicks;
    private int orbitDirection = 1;
    private double dynamicCombatMinDistance = COMBAT_MIN_DISTANCE;
    private double dynamicCombatMaxDistance = COMBAT_MAX_DISTANCE;

    public MoDiCaiEntity(EntityType<? extends MoDiCaiEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("\u83ab\u8fea\u624d"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Pig.createAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.095D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean didHurt = super.hurt(source, amount);
        if (!didHurt || this.level().isClientSide() || this.isDeadOrDying()) {
            return didHurt;
        }

        LivingEntity attacker = this.resolveAttacker(source);
        if (attacker != null && this.canHoldGrudgeAgainst(attacker)) {
            this.setTarget(attacker);
            this.setLastHurtByMob(attacker);
        }

        return didHurt;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.updateBossBar();
        this.tickGroundCombatBehavior();
    }

    protected void tickGroundCombatBehavior() {
        this.refreshCombatStyle();

        if (this.laserCooldownTicks > 0) {
            this.laserCooldownTicks--;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        boolean retreating = this.shouldRetreatFrom(target);
        if (retreating) {
            this.setSprinting(false);
            this.chaseRepathTicks = 0;
            this.retreatFromTarget(target);
        } else if (this.shouldChaseTarget(target)) {
            this.setSprinting(false);
            this.retreatRepathTicks = 0;
            this.chaseTarget(target);
        } else {
            this.setSprinting(false);
            this.retreatRepathTicks = 0;
            this.chaseRepathTicks = 0;
            if (target != null && target.isAlive()) {
                this.orbitAroundTarget(target);
            }
        }

        if (!this.canFireLaserAt(target)) {
            return;
        }

        this.firePigLaserAt(target);
        this.resetLaserCooldown();
    }

    protected void updateBossBar() {
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("LaserCooldownTicks", this.laserCooldownTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.laserCooldownTicks = Math.max(0, compound.getInt("LaserCooldownTicks"));
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

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        this.spawnAtLocation(ChenMod.PIG_TALISMAN.get());
    }

    private LivingEntity resolveAttacker(DamageSource source) {
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

    private boolean canHoldGrudgeAgainst(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return true;
        }

        return !player.isCreative() && !player.isSpectator();
    }

    private boolean canFireLaserAt(LivingEntity target) {
        double laserDistanceLimit = Math.min(LASER_RANGE, this.dynamicCombatMaxDistance + 4.0D);
        return target != null
                && target.isAlive()
                && target.level() == this.level()
                && this.canHoldGrudgeAgainst(target)
                && this.laserCooldownTicks <= 0
                && this.hasLineOfSight(target)
                && this.distanceToSqr(target) <= laserDistanceLimit * laserDistanceLimit;
    }

    private boolean shouldRetreatFrom(LivingEntity target) {
        return target != null
                && target.isAlive()
                && target.level() == this.level()
                && this.canHoldGrudgeAgainst(target)
                && this.distanceToSqr(target) < this.dynamicCombatMinDistance * this.dynamicCombatMinDistance;
    }

    private boolean shouldChaseTarget(LivingEntity target) {
        return target != null
                && target.isAlive()
                && target.level() == this.level()
                && this.canHoldGrudgeAgainst(target)
                && this.distanceToSqr(target) > this.dynamicCombatMaxDistance * this.dynamicCombatMaxDistance;
    }

    private void retreatFromTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.retreatRepathTicks > 0) {
            this.retreatRepathTicks--;
            return;
        }

        Vec3 away = this.position().subtract(target.position());
        double horizontalLengthSqr = away.x * away.x + away.z * away.z;
        if (horizontalLengthSqr < 1.0E-6D) {
            float randomYaw = this.getRandom().nextFloat() * Mth.TWO_PI;
            away = new Vec3(Mth.cos(randomYaw), 0.0D, Mth.sin(randomYaw));
        } else {
            double invLen = 1.0D / Math.sqrt(horizontalLengthSqr);
            away = new Vec3(away.x * invLen, 0.0D, away.z * invLen);
        }

        Vec3 retreatPos = this.position().add(away.scale(RETREAT_STEP_DISTANCE));
        this.getNavigation().moveTo(retreatPos.x, this.getY(), retreatPos.z, RETREAT_SPEED);
        this.retreatRepathTicks = RETREAT_REPATH_INTERVAL;
    }

    private void chaseTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.chaseRepathTicks > 0) {
            this.chaseRepathTicks--;
            return;
        }

        this.getNavigation().moveTo(target, CHASE_SPEED);
        this.chaseRepathTicks = CHASE_REPATH_INTERVAL;
    }

    private void orbitAroundTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.orbitRepathTicks > 0) {
            this.orbitRepathTicks--;
            return;
        }

        if (this.getRandom().nextFloat() < ORBIT_SKIP_CHANCE) {
            this.getNavigation().stop();
            this.orbitRepathTicks = Mth.nextInt(this.getRandom(), ORBIT_SKIP_MIN_TICKS, ORBIT_SKIP_MAX_TICKS);
            return;
        }

        Vec3 radial = this.position().subtract(target.position());
        double horizontalLengthSqr = radial.x * radial.x + radial.z * radial.z;
        if (horizontalLengthSqr < 1.0E-6D) {
            float randomYaw = this.getRandom().nextFloat() * Mth.TWO_PI;
            radial = new Vec3(Mth.cos(randomYaw), 0.0D, Mth.sin(randomYaw));
        } else {
            double invLen = 1.0D / Math.sqrt(horizontalLengthSqr);
            radial = new Vec3(radial.x * invLen, 0.0D, radial.z * invLen);
        }

        Vec3 tangent = new Vec3(-radial.z * this.orbitDirection, 0.0D, radial.x * this.orbitDirection);
        double radius = ORBIT_RADIUS + this.getRandom().nextDouble() * 3.0D - 1.5D;
        Vec3 orbitPos = target.position()
                .add(radial.scale(radius))
                .add(tangent.scale(2.0D + this.getRandom().nextDouble() * 2.0D));

        this.getNavigation().moveTo(orbitPos.x, this.getY(), orbitPos.z, ORBIT_SPEED + this.getRandom().nextDouble() * 0.08D);
        this.orbitRepathTicks = Mth.nextInt(this.getRandom(), ORBIT_REPATH_MIN_TICKS, ORBIT_REPATH_MAX_TICKS);
        if (this.getRandom().nextFloat() < 0.06F) {
            this.orbitDirection *= -1;
        }
    }

    private void refreshCombatStyle() {
        if (this.tacticRefreshTicks > 0) {
            this.tacticRefreshTicks--;
            return;
        }

        double minCandidate = COMBAT_MIN_DISTANCE + (this.getRandom().nextDouble() * 2.0D - 1.0D) * COMBAT_MIN_JITTER;
        this.dynamicCombatMinDistance = Mth.clamp(minCandidate, 4.5D, 10.5D);

        double maxCandidate = COMBAT_MAX_DISTANCE + (this.getRandom().nextDouble() * 2.0D - 1.0D) * COMBAT_MAX_JITTER;
        this.dynamicCombatMaxDistance = Mth.clamp(maxCandidate, this.dynamicCombatMinDistance + 2.0D, LASER_RANGE - 2.0D);

        this.orbitDirection = this.getRandom().nextBoolean() ? 1 : -1;
        this.tacticRefreshTicks = Mth.nextInt(this.getRandom(), TACTIC_REFRESH_MIN_TICKS, TACTIC_REFRESH_MAX_TICKS);
    }

    private void resetLaserCooldown() {
        int jitter = this.getRandom().nextInt(LASER_COOLDOWN_JITTER_TICKS * 2 + 1) - LASER_COOLDOWN_JITTER_TICKS;
        this.laserCooldownTicks = Math.max(20, LASER_COOLDOWN_TICKS + jitter);
    }

    @Nullable
    private Vec3 computeLaserAimDirection(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return null;
        }

        Vec3 aimPoint = target.getEyePosition().subtract(0.0D, target.getBbHeight() * 0.20D, 0.0D);
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

        this.faceTargetImmediately(target);
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.lookAt(target, 30.0F, 30.0F);

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

    private void faceTargetImmediately(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        if (dx * dx + dz * dz < 1.0E-6D) {
            return;
        }

        float yaw = (float) (Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRot = yaw;
        this.yHeadRotO = yaw;
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
        return eyeCenter.add(right.scale(0.18D * side));
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
        AABB beamBox = new AABB(start, end).inflate(0.75D);
        Vec3 knockbackDirection = end.subtract(start);
        if (knockbackDirection.lengthSqr() > 1.0E-7D) {
            knockbackDirection = knockbackDirection.normalize();
        } else {
            knockbackDirection = caster.getLookAngle().normalize();
        }

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, beamBox, entity ->
                entity.isAlive() && entity != caster && !entity.isSpectator());

        for (LivingEntity target : targets) {
            if (!target.getBoundingBox().inflate(0.3D).clip(start, end).isPresent()) {
                continue;
            }
            if (!hitEntityIds.add(target.getId())) {
                continue;
            }

            target.hurt(caster.damageSources().mobAttack(caster), LASER_DAMAGE);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.15D));
            target.push(knockbackDirection.x * LASER_KNOCKBACK, 0.03D, knockbackDirection.z * LASER_KNOCKBACK);
            target.hurtMarked = true;
        }
    }
}
