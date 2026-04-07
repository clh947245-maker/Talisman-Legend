package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.SheepPowerMagic;
import com.example.examplemod.magic.transformation.TransformationManager;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 玩家“身体”实体。
 * 这个实体本质上是一个静止的生物实体，用来在玩家进入特殊状态时临时保存玩家的大部分运行时数据，
 * 例如：
 * 1. 所属玩家 UUID
 * 2. 背包与装备
 * 3. 药水效果
 * 4. 血量、护盾值、饥饿值
 *
 * 当玩家状态切换回来时，再把这些数据还原给玩家。
 * 同时它也承担了一些“替身”行为，例如吸引敌对生物仇恨、接触史莱姆时受伤、自动拾取附近物品等。
 */
public class SheepBodyEntity extends PathfinderMob {
    /** 普通敌对生物仇恨刷新间隔，单位为 tick（20 tick = 1 秒）。 */
    private static final int HOSTILE_ATTRACTION_INTERVAL = 10;
    /** 史莱姆/岩浆怪目标刷新更频繁，避免这类近战弹跳怪丢失目标。 */
    private static final int SLIME_ATTRACTION_INTERVAL = 2;
    /** 搜索附近敌对生物时使用的半径。 */
    private static final double HOSTILE_ATTRACTION_SEARCH_RADIUS = 32.0D;
    /** 当怪物没有配置跟随距离属性时，使用的默认跟随距离。 */
    private static final double DEFAULT_FOLLOW_RANGE = 16.0D;
    /** 身体自动拾取地面掉落物时使用的极小吸附半径。 */
    private static final double BODY_ITEM_PICKUP_RADIUS = 0.35D;

    /** 同步到客户端的“所属玩家 UUID”。 */
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(SheepBodyEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    /** 同步到客户端的“外观来源 UUID”，用于决定这具身体显示谁的皮肤。 */
    private static final EntityDataAccessor<Optional<UUID>> DATA_APPEARANCE_UUID =
            SynchedEntityData.defineId(SheepBodyEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    /** 同步到客户端的饥饿值。 */
    private static final EntityDataAccessor<Integer> DATA_FOOD_LEVEL =
            SynchedEntityData.defineId(SheepBodyEntity.class, EntityDataSerializers.INT);

    /** 同步到客户端的饱和度。 */
    private static final EntityDataAccessor<Float> DATA_SATURATION =
            SynchedEntityData.defineId(SheepBodyEntity.class, EntityDataSerializers.FLOAT);

    /** 同步到客户端的饥饿消耗值。 */
    private static final EntityDataAccessor<Float> DATA_EXHAUSTION =
            SynchedEntityData.defineId(SheepBodyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_MONKEY_TRANSFORMATION_ID =
            SynchedEntityData.defineId(SheepBodyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SNACK_INVISIBLE =
            SynchedEntityData.defineId(SheepBodyEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * 用于快照玩家背包的固定容量。
     * 这里使用 41 格，通常覆盖主背包、快捷栏、副手等常见槽位。
     */
    private static final int INVENTORY_SIZE = 41;

    /** 保存玩家背包快照的列表，不直接引用玩家背包，避免后续状态联动。 */
    private final NonNullList<ItemStack> inventorySnapshot = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);

    public SheepBodyEntity(EntityType<? extends SheepBodyEntity> type, Level level) {
        super(type, level);
        // 身体实体仍然受重力影响，保持正常实体表现。
        this.setNoGravity(false);
        // 标记为持久实体，避免因距离或常规刷怪清理逻辑被移除。
        this.setPersistenceRequired();
    }

    /**
     * 定期把周围敌对生物的仇恨拉到“身体”实体上。
     * 这样玩家在脱离原身体后，附近怪物仍会继续攻击身体而不是立即丢失仇恨。
     */
    private void attractNearbyHostileMobs() {
        Player owner = this.getOwnerPlayer();
        if (owner == null || !owner.isAlive()) {
            
        }
        this.level().getEntitiesOfClass(
                Mob.class,
                this.getBoundingBox().inflate(HOSTILE_ATTRACTION_SEARCH_RADIUS),
                mob -> mob instanceof Monster && mob.isAlive() && mob.isEffectiveAi() && mob != this
        ).forEach(mob -> this.tryRetargetHostileMob(mob, owner));
    }

    /**
     * 史莱姆与岩浆怪这类实体经常依赖近距离更新目标，因此这里单独高频刷新一次。
     */
    private void refreshNearbySlimeTargets() {
        Player owner = this.getOwnerPlayer();
        if (owner == null || !owner.isAlive()) {
            return;
        }
        this.level().getEntitiesOfClass(
                Slime.class,
                this.getBoundingBox().inflate(HOSTILE_ATTRACTION_SEARCH_RADIUS),
                slime -> slime.isAlive() && slime.isEffectiveAi()
        ).forEach(slime -> this.tryRetargetHostileMob(slime, owner));
    }

    /**
     * 尝试把某个怪物的目标改为当前身体实体。
     * 只有在目标为空、目标就是身体/主人，或者目标已经无效时，才会接管其仇恨。
     */
    private void tryRetargetHostileMob(Mob mob, Player owner) {
        LivingEntity currentTarget = mob.getTarget();
        // 怪物已经在追击其他有效目标时，不强行改写，避免干扰其正常战斗行为。
        if (currentTarget != null && currentTarget != this && currentTarget != owner && currentTarget.isAlive()) {
            return;
        }

        // 按“如果这是一个玩家，怪物是否会主动攻击它”的规则进行筛选。
        if (!this.shouldActivelyTargetAsPlayer(mob, owner)) {
            return;
        }

        // 优先读取怪物自身的跟随距离属性，若没有则回退到默认值。
        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = DEFAULT_FOLLOW_RANGE;
        }

        // 超出怪物正常索敌距离时，不强制锁定。
        if (mob.distanceToSqr(this) > followRange * followRange) {
            return;
        }

        // 需要对身体有视线，避免隔墙强制拉怪。
        if (!mob.getSensing().hasLineOfSight(this)) {
            return;
        }

        mob.setTarget(this);
    }

    /** 根据已保存的 UUID 找回所属玩家。 */
    private Player getOwnerPlayer() {
        UUID ownerUUID = this.getOwnerUUID();
        return ownerUUID == null ? null : this.level().getPlayerByUUID(ownerUUID);
    }

    /**
     * 判断某个怪物是否应该把当前身体视作“玩家型敌人”。
     * 这里尽量模拟原版怪物对玩家的索敌规则，而不是无条件让所有怪攻击身体。
     */
    private boolean shouldActivelyTargetAsPlayer(Mob mob, Player owner) {
        // 和平模式下，怪物不应主动敌对。
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }

        // 身体不可被视为敌人、同阵营、或者怪物本就不能攻击玩家时，直接跳过。
        if (!this.canBeSeenAsEnemy() || mob.isAlliedTo(this) || !mob.canAttackType(EntityType.PLAYER)) {
            return false;
        }

        // 蜘蛛在高亮度下会失去主动攻击性，保持原版行为。
        if (mob instanceof Spider && this.level().getMaxLocalRawBrightness(mob.blockPosition()) >= 8) {
            return false;
        }

        // 末影人是否敌对需要额外判断“是否被盯视”。
        if (mob instanceof EnderMan enderMan) {
            return this.shouldEnderManTargetBody(enderMan, owner);
        }

        // 中立生物只会在对主人处于愤怒状态时才接管仇恨。
        if (mob instanceof NeutralMob neutralMob) {
            return neutralMob.isAngryAt(owner);
        }

        // 其余怪物默认按普通敌对怪处理。
        return true;
    }

    /** 末影人只有在本就仇恨主人，或身体正在“凝视挑衅”它时才会攻击身体。 */
    private boolean shouldEnderManTargetBody(EnderMan enderMan, Player owner) {
        return enderMan.isAngryAt(owner) || this.isBodyProvokingEnderMan(enderMan, owner);
    }

    /**
     * 判断身体当前视线是否等价于“玩家盯着末影人看”。
     * 这里基本复用了原版末影人被挑衅的判定思路。
     */
    private boolean isBodyProvokingEnderMan(EnderMan enderMan, Player owner) {
        // 如果头盔等装备可抑制末影人仇恨，则不触发挑衅。
        ItemStack headItem = this.getItemBySlot(EquipmentSlot.HEAD);
        if (CommonHooks.shouldSuppressEnderManAnger(enderMan, owner, headItem)) {
            return false;
        }

        // 获取身体当前朝向向量。
        Vec3 viewVector = this.getViewVector(1.0F).normalize();
        // 计算从身体眼睛位置指向末影人的方向向量。
        Vec3 toEnderMan = new Vec3(
                enderMan.getX() - this.getX(),
                enderMan.getEyeY() - this.getEyeY(),
                enderMan.getZ() - this.getZ()
        );
        double distance = toEnderMan.length();
        // 极端情况下距离为 0，视作正在盯视。
        if (distance <= 0.0D) {
            return true;
        }

        Vec3 lookDirection = toEnderMan.normalize();
        double dot = viewVector.dot(lookDirection);
        // 点乘越接近 1，说明身体朝向越接近末影人方向；同时要求真实可见。
        return dot > 1.0D - 0.025D / distance && this.hasLineOfSight(enderMan);
    }

    /** 定义身体实体的基础属性：20 点生命值，且自身不移动。 */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /** 注册所有需要同步保存的实体数据。 */
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_APPEARANCE_UUID, Optional.empty());
        builder.define(DATA_FOOD_LEVEL, 20);
        builder.define(DATA_SATURATION, 5.0F);
        builder.define(DATA_EXHAUSTION, 0.0F);
        builder.define(DATA_MONKEY_TRANSFORMATION_ID, TransformationManager.ID_REVERT);
        builder.define(DATA_SNACK_INVISIBLE, false);
    }

    /** 设置所属玩家 UUID。 */
    public void setOwnerUUID(UUID ownerUUID) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(ownerUUID));
    }

    /** 获取所属玩家 UUID。 */
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    /** 设置这具身体应显示谁的外观。 */
    public void setAppearanceUUID(UUID appearanceUUID) {
        this.entityData.set(DATA_APPEARANCE_UUID, Optional.ofNullable(appearanceUUID));
    }

    /**
     * 获取这具身体的外观来源 UUID。
     * 为兼容旧数据，没有单独记录时回退到 owner UUID。
     */
    public UUID getAppearanceUUID() {
        return this.entityData.get(DATA_APPEARANCE_UUID).orElse(this.getOwnerUUID());
    }

    public void setMonkeyTransformationId(int transformationId) {
        this.entityData.set(DATA_MONKEY_TRANSFORMATION_ID, transformationId);
    }

    public int getMonkeyTransformationId() {
        return this.entityData.get(DATA_MONKEY_TRANSFORMATION_ID);
    }

    public void setSnackInvisible(boolean snackInvisible) {
        this.entityData.set(DATA_SNACK_INVISIBLE, snackInvisible);
    }

    public boolean isSnackInvisible() {
        return this.entityData.get(DATA_SNACK_INVISIBLE);
    }

    public UUID resolveAppearanceUUID() {
        Optional<UUID> explicitAppearanceUUID = this.entityData.get(DATA_APPEARANCE_UUID);
        if (explicitAppearanceUUID.isPresent()) {
            return explicitAppearanceUUID.get();
        }

        Player owner = this.getOwnerPlayer();
        if (owner != null) {
            return SheepPowerMagic.getCurrentAppearanceUUID(owner);
        }

        UUID ownerUUID = this.getOwnerUUID();
        if (ownerUUID != null) {
            return ownerUUID;
        }

        return this.getUUID();
    }

    public Component resolveDisplayName() {
        if (this.getCustomName() != null) {
            return this.getCustomName().copy();
        }

        Player owner = this.getOwnerPlayer();
        if (owner != null) {
            return SheepPowerMagic.getCurrentDisplayName(owner);
        }

        return this.getName().copy();
    }

    /**
     * 从玩家拷贝完整状态到身体实体。
     * 这一步通常发生在玩家进入“灵魂/脱体”之类状态时。
     */
    public void copyStateFrom(Player player) {
        this.setOwnerUUID(player.getUUID());
        this.setAppearanceUUID(SheepPowerMagic.getCurrentAppearanceUUID(player));
        this.setMonkeyTransformationId(resolveCurrentMonkeyTransformationId(player));
        this.setSnackInvisible(player.hasEffect(ChenMod.SNACK_POWER));

        // 同步位置与朝向，保证身体留在玩家离开的原地。
        this.setPos(player.getX(), player.getY(), player.getZ());
        this.setYRot(player.getYRot());
        this.setYBodyRot(player.yBodyRot);
        this.setYHeadRot(player.getYHeadRot());
        this.setXRot(player.getXRot());
        this.yRotO = player.yRotO;
        this.yBodyRotO = player.yBodyRotO;
        this.yHeadRotO = player.yHeadRotO;
        this.xRotO = player.xRotO;

        this.setCustomName(SheepPowerMagic.getCurrentDisplayName(player));
        this.setCustomNameVisible(false);

        // 分别保存背包、装备、药水、食物与生命状态。
        this.copyInventory(player);
        this.copyEquipment(player);
        this.copyEffects(player);
        this.copyFoodData(player);
        this.syncHealthFrom(player.getMaxHealth(), player.getHealth());
        this.setAbsorptionAmount(player.getAbsorptionAmount());
        this.refreshDimensions();
    }

    /**
     * 把身体上保存的状态重新写回玩家。
     * 这一步通常发生在玩家“回到身体”时。
     */
    public void applyStoredStateTo(Player player) {
        // 先还原位置与朝向，避免玩家回归后朝向错乱。
        player.teleportTo(this.getX(), this.getY(), this.getZ());
        player.setYRot(this.getYRot());
        player.setYHeadRot(this.getYHeadRot());
        player.setXRot(this.getXRot());
        // 清空速度，防止恢复瞬间保留旧速度导致弹飞或滑动。
        player.setDeltaMovement(Vec3.ZERO);

        this.restoreInventory(player);
        this.restoreEquipment(player);
        this.restoreEffects(player);
        syncPlayerHealth(player, this.getMaxHealth(), this.getHealth());
        player.setAbsorptionAmount(this.getAbsorptionAmount());

        FoodData playerFoodData = player.getFoodData();
        playerFoodData.setFoodLevel(this.getStoredFoodLevel());
        playerFoodData.setSaturation(this.getStoredSaturation());
        playerFoodData.setExhaustion(this.getStoredExhaustion());

        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    /**
     * 把来源生命上限差值应用到身体实体，再同步当前生命值。
     * 这样可兼容额外属性加成导致的最大生命变化。
     */
    private void syncHealthFrom(float sourceMaxHealth, float sourceHealth) {
        var maxHealthAttribute = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double delta = sourceMaxHealth - this.getMaxHealth();
            maxHealthAttribute.setBaseValue(maxHealthAttribute.getBaseValue() + delta);
        }
        // 当前生命值不能超过同步后的生命上限。
        this.setHealth(Math.min(sourceHealth, this.getMaxHealth()));
    }

    /** 与 {@link #syncHealthFrom(float, float)} 类似，但目标对象改为玩家。 */
    private static void syncPlayerHealth(Player player, float sourceMaxHealth, float sourceHealth) {
        var maxHealthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double delta = sourceMaxHealth - player.getMaxHealth();
            maxHealthAttribute.setBaseValue(maxHealthAttribute.getBaseValue() + delta);
        }
        player.setHealth(Math.min(sourceHealth, player.getMaxHealth()));
    }

    private static int resolveCurrentMonkeyTransformationId(Player player) {
        MobEffectInstance monkeyEffect = player.getEffect(ChenMod.MONKEY_POWER);
        return monkeyEffect == null ? TransformationManager.ID_REVERT : monkeyEffect.getAmplifier();
    }

    /** 复制玩家背包快照。先清空旧快照，再逐格深拷贝。 */
    private void copyInventory(Player player) {
        for (int slot = 0; slot < this.inventorySnapshot.size(); slot++) {
            this.inventorySnapshot.set(slot, ItemStack.EMPTY);
        }

        int size = Math.min(player.getInventory().getContainerSize(), this.inventorySnapshot.size());
        for (int slot = 0; slot < size; slot++) {
            this.inventorySnapshot.set(slot, player.getInventory().getItem(slot).copy());
        }
    }

    /** 将背包快照恢复到玩家当前背包。 */
    private void restoreInventory(Player player) {
        int size = Math.min(player.getInventory().getContainerSize(), this.inventorySnapshot.size());
        for (int slot = 0; slot < size; slot++) {
            player.getInventory().setItem(slot, this.inventorySnapshot.get(slot).copy());
        }
    }

    /** 复制玩家所有常用装备槽位。 */
    private void copyEquipment(Player player) {
        copyEquipmentSlot(player, EquipmentSlot.MAINHAND);
        copyEquipmentSlot(player, EquipmentSlot.OFFHAND);
        copyEquipmentSlot(player, EquipmentSlot.FEET);
        copyEquipmentSlot(player, EquipmentSlot.LEGS);
        copyEquipmentSlot(player, EquipmentSlot.CHEST);
        copyEquipmentSlot(player, EquipmentSlot.HEAD);
    }

    /** 复制单个装备槽位，并将身体掉落率设为 0，避免死亡时重复掉装。 */
    private void copyEquipmentSlot(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        this.setItemSlot(slot, stack.copy());
        this.setDropChance(slot, 0.0F);
    }

    /** 将身体上的装备恢复给玩家。 */
    private void restoreEquipment(Player player) {
        restoreEquipmentSlot(player, EquipmentSlot.MAINHAND);
        restoreEquipmentSlot(player, EquipmentSlot.OFFHAND);
        restoreEquipmentSlot(player, EquipmentSlot.FEET);
        restoreEquipmentSlot(player, EquipmentSlot.LEGS);
        restoreEquipmentSlot(player, EquipmentSlot.CHEST);
        restoreEquipmentSlot(player, EquipmentSlot.HEAD);
    }

    /** 恢复单个装备槽位。 */
    private void restoreEquipmentSlot(Player player, EquipmentSlot slot) {
        player.setItemSlot(slot, this.getItemBySlot(slot).copy());
    }

    /**
     * 拷贝玩家药水效果。
     * Sheep Power 本身属于机制效果，不应在身体和玩家之间重复搬运，因此显式跳过。
     */
    private void copyEffects(Player player) {
        for (MobEffectInstance effectInstance : player.getActiveEffects()) {
            if (effectInstance.is(ChenMod.SHEEP_POWER)) {
                continue;
            }
            this.addEffect(new MobEffectInstance(effectInstance));
        }
    }

    /**
     * 先清掉玩家现有效果（保留 Sheep Power），再把身体上的效果全部补回去，
     * 确保恢复后的效果集合与保存时一致。
     */
    private void restoreEffects(Player player) {
        List<MobEffectInstance> existingEffects = new ArrayList<>(player.getActiveEffects());
        for (MobEffectInstance effectInstance : existingEffects) {
            if (effectInstance.is(ChenMod.SHEEP_POWER)) {
                continue;
            }
            player.removeEffect(effectInstance.getEffect());
        }

        for (MobEffectInstance effectInstance : this.getActiveEffects()) {
            if (effectInstance.is(ChenMod.SHEEP_POWER)) {
                continue;
            }
            player.addEffect(new MobEffectInstance(effectInstance));
        }
    }

    /** 保存玩家的食物相关数据。 */
    private void copyFoodData(Player player) {
        FoodData foodData = player.getFoodData();
        this.entityData.set(DATA_FOOD_LEVEL, foodData.getFoodLevel());
        this.entityData.set(DATA_SATURATION, foodData.getSaturationLevel());
        this.entityData.set(DATA_EXHAUSTION, foodData.getExhaustionLevel());
    }

    /** 获取已保存的饥饿值。 */
    public int getStoredFoodLevel() {
        return this.entityData.get(DATA_FOOD_LEVEL);
    }

    /** 获取已保存的饱和度。 */
    public float getStoredSaturation() {
        return this.entityData.get(DATA_SATURATION);
    }

    /** 获取已保存的饥饿消耗值。 */
    public float getStoredExhaustion() {
        return this.entityData.get(DATA_EXHAUSTION);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.isAlive()) {
            // 服务端负责处理实际状态变化，客户端只做显示。
            this.syncSnackInvisibility();
            this.collectNearbyItems();
            if (this.tickCount % SLIME_ATTRACTION_INTERVAL == 0) {
                this.refreshNearbySlimeTargets();
            }
            this.handleSlimeContactDamage();
            if (this.tickCount % HOSTILE_ATTRACTION_INTERVAL == 0) {
                this.attractNearbyHostileMobs();
            }
        }
    }

    /**
     * 处理与史莱姆、岩浆怪发生实体接触时的伤害。
     * 这里不依赖它们主动攻击事件，而是直接检测碰撞箱重叠来模拟贴身伤害。
     */
    private void syncSnackInvisibility() {
        boolean shouldBeInvisible = this.hasEffect(ChenMod.SNACK_POWER);
        if (this.isSnackInvisible() != shouldBeInvisible) {
            this.setSnackInvisible(shouldBeInvisible);
        }
    }

    private void handleSlimeContactDamage() {
        if (this.level().isClientSide || !this.isAlive()) {
            return;
        }

        List<Slime> nearbySlimes = this.level().getEntitiesOfClass(
                Slime.class,
                this.getBoundingBox().inflate(0.5D),
                slime -> slime.isAlive() && slime.isEffectiveAi()
        );

        for (Slime slime : nearbySlimes) {
            if (this.getBoundingBox().intersects(slime.getBoundingBox())) {
                // 伤害值直接取史莱姆体型大小，和其近战威胁大致对应。
                int size = slime.getSize();
                float damage = (float) size;
                this.hurt(this.damageSources().mobAttack(slime), damage);
            }
        }

        List<MagmaCube> nearbyMagmaCubes = this.level().getEntitiesOfClass(
                MagmaCube.class,
                this.getBoundingBox().inflate(0.5D),
                cube -> cube.isAlive() && cube.isEffectiveAi()
        );

        for (MagmaCube magmaCube : nearbyMagmaCubes) {
            if (this.getBoundingBox().intersects(magmaCube.getBoundingBox())) {
                // 岩浆怪同样按体型造成接触伤害。
                int size = magmaCube.getSize();
                float damage = (float) size;
                this.hurt(this.damageSources().mobAttack(magmaCube), damage);
            }
        }
    }

    /**
     * 自动拾取身体附近的掉落物，并塞进内部背包快照的空位。
     * 这可以避免玩家脱体期间掉落物堆在脚下消失。
     */
    private void collectNearbyItems() {
        if (this.level().isClientSide || !this.isAlive()) {
            return;
        }

        List<ItemEntity> nearbyItems = this.level().getEntitiesOfClass(
                ItemEntity.class,
                this.getBoundingBox().inflate(BODY_ITEM_PICKUP_RADIUS),
                itemEntity -> itemEntity.isAlive() && !itemEntity.getItem().isEmpty()
        );

        for (ItemEntity itemEntity : nearbyItems) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remaining = stack.copy();
            this.absorbItemIntoSnapshot(remaining);

            if (remaining.isEmpty()) {
                itemEntity.discard();
            } else if (remaining.getCount() != stack.getCount()) {
                itemEntity.setItem(remaining);
            }
        }
    }

    /**
     * 像玩家背包一样尽量吸收一个物品堆：
     * 1. 先合并到已有的同类堆叠
     * 2. 再放入空槽
     * 处理后，remaining 中留下的是仍然装不下的部分。
     */
    private void absorbItemIntoSnapshot(ItemStack remaining) {
        if (remaining.isEmpty()) {
            return;
        }

        for (int i = 0; i < this.inventorySnapshot.size() && !remaining.isEmpty(); i++) {
            ItemStack storedStack = this.inventorySnapshot.get(i);
            if (storedStack.isEmpty()) {
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(storedStack, remaining)) {
                continue;
            }

            int maxStackSize = Math.min(storedStack.getMaxStackSize(), remaining.getMaxStackSize());
            int freeSpace = maxStackSize - storedStack.getCount();
            if (freeSpace <= 0) {
                continue;
            }

            int moved = Math.min(freeSpace, remaining.getCount());
            storedStack.grow(moved);
            remaining.shrink(moved);
        }

        for (int i = 0; i < this.inventorySnapshot.size() && !remaining.isEmpty(); i++) {
            if (!this.inventorySnapshot.get(i).isEmpty()) {
                continue;
            }

            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            ItemStack inserted = remaining.copyWithCount(moved);
            this.inventorySnapshot.set(i, inserted);
            remaining.shrink(moved);
        }
    }

    /**
     * 分身死亡时，将内部保存的背包快照全部掉落到地面。
     * 这里挂在原版死亡掉落流程中，比直接写在 {@code die(...)} 里更可靠。
     * 掉落后立即清空快照，避免异常重复调用时再次掉落同一批物品。
     */
    private void dropStoredInventoryOnDeath() {
        if (this.level().isClientSide) {
            return;
        }

        if (!this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            return;
        }

        for (int i = 0; i < this.inventorySnapshot.size(); i++) {
            ItemStack stack = this.inventorySnapshot.get(i);
            if (stack.isEmpty()) {
                continue;
            }

            ItemEntity itemEntity = new ItemEntity(
                    this.level(),
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    stack.copy()
            );
            itemEntity.setDefaultPickUpDelay();
            this.level().addFreshEntity(itemEntity);
            this.inventorySnapshot.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        this.dropStoredInventoryOnDeath();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);

        // 保存所属玩家信息，确保重载世界后仍能知道这具身体属于谁。
        UUID ownerUUID = this.getOwnerUUID();
        if (ownerUUID != null) {
            compound.putUUID("OwnerUUID", ownerUUID);
        }

        UUID appearanceUUID = this.getAppearanceUUID();
        if (appearanceUUID != null) {
            compound.putUUID("AppearanceUUID", appearanceUUID);
        }
        compound.putInt("MonkeyTransformationId", this.getMonkeyTransformationId());
        compound.putBoolean("SnackInvisible", this.isSnackInvisible());

        // 保存食物系统数据。
        compound.putInt("StoredFoodLevel", this.getStoredFoodLevel());
        compound.putFloat("StoredSaturation", this.getStoredSaturation());
        compound.putFloat("StoredExhaustion", this.getStoredExhaustion());

        // 保存背包快照内容。
        CompoundTag inventoryTag = new CompoundTag();
        ContainerHelper.saveAllItems(inventoryTag, this.inventorySnapshot, this.level().registryAccess());
        compound.put("InventorySnapshot", inventoryTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);

        // 读取所属玩家 UUID。
        if (compound.hasUUID("OwnerUUID")) {
            this.setOwnerUUID(compound.getUUID("OwnerUUID"));
        }

        if (compound.hasUUID("AppearanceUUID")) {
            this.setAppearanceUUID(compound.getUUID("AppearanceUUID"));
        } else {
            this.setAppearanceUUID(this.getOwnerUUID());
        }
        this.setMonkeyTransformationId(compound.getInt("MonkeyTransformationId"));
        this.setSnackInvisible(compound.getBoolean("SnackInvisible"));

        // 读取食物系统数据。
        if (compound.contains("StoredFoodLevel")) {
            this.entityData.set(DATA_FOOD_LEVEL, compound.getInt("StoredFoodLevel"));
        }
        if (compound.contains("StoredSaturation")) {
            this.entityData.set(DATA_SATURATION, compound.getFloat("StoredSaturation"));
        }
        if (compound.contains("StoredExhaustion")) {
            this.entityData.set(DATA_EXHAUSTION, compound.getFloat("StoredExhaustion"));
        }

        // 读取背包快照。
        if (compound.contains("InventorySnapshot", Tag.TAG_COMPOUND)) {
            ContainerHelper.loadAllItems(compound.getCompound("InventorySnapshot"), this.inventorySnapshot, this.level().registryAccess());
        }
    }

    /**
     * 身体死亡时通知 SheepPowerMagic。
     * 这样外部系统可以及时清理追踪关系、触发失败逻辑或做额外处理。
     */
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        SheepPowerMagic.onTrackedBodyDeath(this);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_MONKEY_TRANSFORMATION_ID.equals(key)) {
            this.refreshDimensions();
        }
    }
}
