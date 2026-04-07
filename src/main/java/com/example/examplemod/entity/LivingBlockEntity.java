package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;

import java.util.UUID;

public class LivingBlockEntity extends PathfinderMob {

    private static final int ALERT_RANGE = 24;
    private static final int MIN_ANGER_TICKS = 20 * 20;
    private static final int MAX_ANGER_TICKS = 20 * 39;

    private static final EntityDataAccessor<Integer> DATA_BLOCK_STATE_ID =
            SynchedEntityData.defineId(LivingBlockEntity.class, EntityDataSerializers.INT);

    private int remainingAngerTicks;
    private UUID angerTargetPlayerUuid;

    public LivingBlockEntity(EntityType<? extends LivingBlockEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    public static LivingBlockEntity createFromBlock(Level level, BlockPos pos, BlockState state) {
        LivingBlockEntity entity = ChenMod.LIVING_BLOCK.get().create(level);
        if (entity == null) {
            return null;
        }

        entity.setAnimatedBlockState(state);
        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        entity.yBodyRot = entity.getYRot();
        entity.yHeadRot = entity.getYRot();
        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BLOCK_STATE_ID, Block.getId(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    public void setAnimatedBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK_STATE_ID, Block.getId(state));
        this.refreshBlockAttributes(state);
    }

    public BlockState getAnimatedBlockState() {
        return Block.stateById(this.entityData.get(DATA_BLOCK_STATE_ID));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("AnimatedBlockState", this.entityData.get(DATA_BLOCK_STATE_ID));
        compound.putInt("AngerTime", this.remainingAngerTicks);
        if (this.angerTargetPlayerUuid != null) {
            compound.putUUID("AngerTarget", this.angerTargetPlayerUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("AnimatedBlockState")) {
            this.entityData.set(DATA_BLOCK_STATE_ID, compound.getInt("AnimatedBlockState"));
            this.refreshBlockAttributes(this.getAnimatedBlockState());
        }
        this.remainingAngerTicks = compound.getInt("AngerTime");
        this.angerTargetPlayerUuid = compound.hasUUID("AngerTarget") ? compound.getUUID("AngerTarget") : null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getAnimatedBlockState().getSoundType().getHitSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return this.getAnimatedBlockState().getSoundType().getBreakSound();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getAnimatedBlockState().getSoundType().getBreakSound();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        SoundType soundType = this.getAnimatedBlockState().getSoundType();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), soundType.getStepSound(), SoundSource.HOSTILE, soundType.getVolume() * 0.45F, soundType.getPitch());
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        Item blockItem = this.getAnimatedBlockState().getBlock().asItem();
        if (blockItem == ItemStack.EMPTY.getItem()) {
            return;
        }

        ItemStack drop = new ItemStack(blockItem);
        if (drop.isEmpty()) {
            return;
        }

        ItemEntity itemEntity = new ItemEntity(level, this.getX(), this.getY() + 0.25D, this.getZ(), drop);
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        boolean wasHurt = super.hurt(damageSource, amount);
        if (wasHurt && !this.level().isClientSide && damageSource.getEntity() instanceof Player player) {
            this.becomeAngryAt(player);
            this.alertNearbySameBlocks(player);
        }
        return wasHurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        BlockState state = this.getAnimatedBlockState();
        if (state.isAir()) {
            this.discard();
            return;
        }

        this.refreshBlockAttributes(state);
        this.tickPlayerAnger();
    }

    private void refreshBlockAttributes(BlockState state) {
        int harvestLevel = getHarvestLevel(state);
        float maxHealth = 12.0F + harvestLevel * 8.0F;
        float attackDamage = 1.5F + harvestLevel * 1.25F;
        float movementSpeed = 0.25F - harvestLevel * 0.04F;

        if (Math.abs(this.getMaxHealth() - maxHealth) > 0.01F) {
            AttributeInstance attribute = this.getAttribute(Attributes.MAX_HEALTH);
            if (attribute != null) {
                float healthRatio = this.getMaxHealth() <= 0.0F ? 1.0F : this.getHealth() / this.getMaxHealth();
                attribute.setBaseValue(maxHealth);
                this.setHealth(Mth.clamp(maxHealth * healthRatio, 1.0F, maxHealth));
            }
        }

        AttributeInstance attackAttribute = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttribute != null && Math.abs(attackAttribute.getBaseValue() - attackDamage) > 0.01D) {
            attackAttribute.setBaseValue(attackDamage);
        }

        AttributeInstance movementAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementAttribute != null && Math.abs(movementAttribute.getBaseValue() - movementSpeed) > 0.0001D) {
            movementAttribute.setBaseValue(movementSpeed);
        }
    }

    private static int getHarvestLevel(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return 3;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return 2;
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return 1;
        }
        return 0;
    }

    private void becomeAngryAt(Player player) {
        this.angerTargetPlayerUuid = player.getUUID();
        this.remainingAngerTicks = this.random.nextInt(MAX_ANGER_TICKS - MIN_ANGER_TICKS + 1) + MIN_ANGER_TICKS;
        this.setTarget(player);
    }

    private void alertNearbySameBlocks(Player player) {
        this.level().getEntitiesOfClass(
                LivingBlockEntity.class,
                this.getBoundingBox().inflate(ALERT_RANGE),
                other -> other.isAlive() && other != this && other.getAnimatedBlockState().getBlock() == this.getAnimatedBlockState().getBlock()
        ).forEach(other -> other.becomeAngryAt(player));
    }

    private void tickPlayerAnger() {
        if (this.remainingAngerTicks <= 0) {
            if (this.getTarget() instanceof Player) {
                this.setTarget(null);
            }
            this.angerTargetPlayerUuid = null;
            return;
        }

        this.remainingAngerTicks--;
        Player angerTarget = this.getAngerTargetPlayer();
        if (angerTarget == null || !angerTarget.isAlive() || angerTarget.isSpectator()) {
            this.angerTargetPlayerUuid = null;
            this.remainingAngerTicks = 0;
            if (this.getTarget() instanceof Player) {
                this.setTarget(null);
            }
            return;
        }

        if (this.getTarget() != angerTarget) {
            this.setTarget(angerTarget);
        }
    }

    private Player getAngerTargetPlayer() {
        return this.angerTargetPlayerUuid == null ? null : this.level().getPlayerByUUID(this.angerTargetPlayerUuid);
    }
}
