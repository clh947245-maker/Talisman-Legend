package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.TigerPowerMagic;
import com.example.examplemod.talisman.TigerTalismanHalfItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TigerCloneEntity extends Zombie {
    private static final int HOSTILE_ATTRACTION_INTERVAL = 10;
    private static final int SLIME_ATTRACTION_INTERVAL = 2;
    private static final double HOSTILE_ATTRACTION_SEARCH_RADIUS = 32.0D;
    private static final double DEFAULT_FOLLOW_RANGE = 16.0D;
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(TigerCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public TigerCloneEntity(EntityType<? extends TigerCloneEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);

        if (this.level().isClientSide) {
            return;
        }

        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID == null) {
            return;
        }

        Player owner = this.level().getPlayerByUUID(ownerUUID);
        if (owner == null) {
            return;
        }

        float backlashDamage = owner.getHealth() / 2.0F;
        owner.hurt(this.damageSources().magic(), backlashDamage);
        owner.removeEffect(ChenMod.TIGER_POWER.getHolder().orElseThrow());
        TigerTalismanHalfItem.restoreLinkedHalf(owner, this.getUUID());
        owner.displayClientMessage(Component.translatable("message.chen_mod.tiger_clone_died"), true);
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
    public boolean killedEntity(ServerLevel level, LivingEntity entity) {
        if (entity instanceof Villager) {
            return true;
        }

        return super.killedEntity(level, entity);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || this.isDeadOrDying()) {
            return;
        }

        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID == null) {
            if (this.tickCount > 20) {
                this.discard();
            }
            return;
        }

        Player owner = this.level().getPlayerByUUID(ownerUUID);
        if (owner == null || !owner.hasEffect(ChenMod.TIGER_POWER.getHolder().orElseThrow())) {
            this.discard();
            return;
        }

        if (this.tickCount % SLIME_ATTRACTION_INTERVAL == 0) {
            this.refreshNearbySlimeTargets(owner);
        }
        this.handleSlimeContactDamage();
        if (this.tickCount % HOSTILE_ATTRACTION_INTERVAL == 0) {
            this.attractNearbyHostileMobs(owner);
        }

        TigerPowerMagic.trackClone(this);
    }

    private void attractNearbyHostileMobs(Player owner) {
        this.level().getEntitiesOfClass(
                Mob.class,
                this.getBoundingBox().inflate(HOSTILE_ATTRACTION_SEARCH_RADIUS),
                mob -> mob instanceof Monster && mob.isAlive() && mob.isEffectiveAi() && mob != this
        ).forEach(mob -> this.tryRetargetHostileMob(mob, owner));
    }

    private void refreshNearbySlimeTargets(Player owner) {
        this.level().getEntitiesOfClass(
                Slime.class,
                this.getBoundingBox().inflate(HOSTILE_ATTRACTION_SEARCH_RADIUS),
                slime -> slime.isAlive() && slime.isEffectiveAi()
        ).forEach(slime -> this.tryRetargetHostileMob(slime, owner));
    }

    private void handleSlimeContactDamage() {
        List<Slime> nearbySlimes = this.level().getEntitiesOfClass(
                Slime.class,
                this.getBoundingBox().inflate(0.5D),
                slime -> slime.isAlive() && slime.isEffectiveAi()
        );

        for (Slime slime : nearbySlimes) {
            if (this.getBoundingBox().intersects(slime.getBoundingBox())) {
                this.hurt(this.damageSources().mobAttack(slime), (float) slime.getSize());
            }
        }

        List<MagmaCube> nearbyMagmaCubes = this.level().getEntitiesOfClass(
                MagmaCube.class,
                this.getBoundingBox().inflate(0.5D),
                cube -> cube.isAlive() && cube.isEffectiveAi()
        );

        for (MagmaCube magmaCube : nearbyMagmaCubes) {
            if (this.getBoundingBox().intersects(magmaCube.getBoundingBox())) {
                this.hurt(this.damageSources().mobAttack(magmaCube), (float) magmaCube.getSize());
            }
        }
    }

    private void tryRetargetHostileMob(Mob mob, Player owner) {
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget != null && currentTarget != this && currentTarget != owner && currentTarget.isAlive()) {
            return;
        }

        if (!this.shouldActivelyTargetCloneAsPlayer(mob, owner)) {
            return;
        }

        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = DEFAULT_FOLLOW_RANGE;
        }
        if (mob.distanceToSqr(this) > followRange * followRange) {
            return;
        }
        if (!mob.getSensing().hasLineOfSight(this)) {
            return;
        }

        mob.setTarget(this);
    }

    private boolean shouldActivelyTargetCloneAsPlayer(Mob mob, Player owner) {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (!this.canBeSeenAsEnemy() || mob.isAlliedTo(this) || !mob.canAttackType(EntityType.PLAYER)) {
            return false;
        }
        if (mob instanceof Spider && this.level().getMaxLocalRawBrightness(mob.blockPosition()) >= 8) {
            return false;
        }
        if (mob instanceof EnderMan enderMan) {
            return this.shouldEnderManTargetClone(enderMan, owner);
        }
        if (mob instanceof NeutralMob neutralMob) {
            return neutralMob.isAngryAt(owner);
        }
        return true;
    }

    private boolean shouldEnderManTargetClone(EnderMan enderMan, Player owner) {
        return enderMan.isAngryAt(owner) || this.isCloneProvokingEnderMan(enderMan, owner);
    }

    private boolean isCloneProvokingEnderMan(EnderMan enderMan, Player owner) {
        if (ForgeHooks.shouldSuppressEnderManAnger(enderMan, owner, this.getItemBySlot(EquipmentSlot.HEAD))) {
            return false;
        }

        Vec3 viewVector = this.getViewVector(1.0F).normalize();
        Vec3 toEnderMan = new Vec3(
                enderMan.getX() - this.getX(),
                enderMan.getEyeY() - this.getEyeY(),
                enderMan.getZ() - this.getZ()
        );
        double distance = toEnderMan.length();
        if (distance <= 0.0D) {
            return true;
        }

        Vec3 lookDirection = toEnderMan.normalize();
        double dot = viewVector.dot(lookDirection);
        return dot > 1.0D - 0.025D / distance && this.hasLineOfSight(enderMan);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        TigerPowerMagic.handleCloneRemoved(this, reason);
        super.remove(reason);
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

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }
}
