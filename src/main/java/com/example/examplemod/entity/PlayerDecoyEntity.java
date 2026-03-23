package com.example.examplemod.entity;

import com.example.examplemod.ChenMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.UUID;

/**
 * 玩家分身实体（PlayerDecoyEntity）
 *
 * 适配版本：NeoForge 1.21
 *
 * 行为：
 *   - 外观与玩家完全一致（皮肤、装备，由 PlayerDecoyRenderer 负责渲染）
 *   - 继承 PathfinderMob：怪物的 NearestAttackableTargetGoal 默认攻击此类实体
 *   - 复制玩家血量、护甲值、装备、药水效果（排除羊符咒）
 *   - 自身不移动，静止吸引仇恨
 *   - 死亡不掉落任何物品
 *   - lifetimeTicks 到期后自动 discard()
 */
public class PlayerDecoyEntity extends PathfinderMob {

    // ── 客户端同步数据 ────────────────────────────────────────────────────────────
    private static final EntityDataAccessor<String> OWNER_UUID =
            SynchedEntityData.defineId(PlayerDecoyEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(PlayerDecoyEntity.class, EntityDataSerializers.STRING);

    /** true = Alex 细手臂模型，false = Steve 宽手臂模型 */
    private static final EntityDataAccessor<Boolean> IS_SLIM =
            SynchedEntityData.defineId(PlayerDecoyEntity.class, EntityDataSerializers.BOOLEAN);

    // ── 存活计时 ─────────────────────────────────────────────────────────────────
    private int lifetimeTicks = -1;
    private int ticksAlive    = 0;

    // ── 构造 ─────────────────────────────────────────────────────────────────────
    public PlayerDecoyEntity(EntityType<? extends PlayerDecoyEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(); // 不因区块卸载而消失
    }

    // ── 属性 ─────────────────────────────────────────────────────────────────────
    /**
     * NeoForge 1.21：在 EntityAttributeCreationEvent 里调用
     *   event.put(ChenMod.PLAYER_DECOY.get(), PlayerDecoyEntity.createAttributes().build());
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,          20.0)
                .add(Attributes.MOVEMENT_SPEED,       0.0)  // 分身不主动移动
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0)
                .add(Attributes.ARMOR,                0.0)
                .add(Attributes.ARMOR_TOUGHNESS,      0.0);
    }

    // ── 同步数据初始化 ────────────────────────────────────────────────────────────
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, "");
        builder.define(OWNER_NAME, "Steve");
        builder.define(IS_SLIM,    false);
    }

    // ── AI ───────────────────────────────────────────────────────────────────────
    @Override
    protected void registerGoals() {
        // 只注册浮水 Goal，防止溺水沉底；分身本身不追踪任何目标
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    // ── 工厂方法 ─────────────────────────────────────────────────────────────────
    /**
     * 服务端调用：在玩家脚下生成分身，复制所有状态。
     *
     * @param player        原始玩家（必须在服务端）
     * @param lifetimeTicks 存活时间（ticks）；-1 表示永久存活直到被击杀
     */
    public static void spawnFor(Player player, int lifetimeTicks) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        PlayerDecoyEntity decoy = ChenMod.PLAYER_DECOY.get().create(serverLevel);
        if (decoy == null) return;

        // ── 位置与朝向 ──
        decoy.copyPosition(player);
        decoy.setYRot(player.getYRot());
        decoy.setXRot(player.getXRot());
        decoy.yHeadRot  = player.yHeadRot;
        decoy.yBodyRot  = player.yBodyRot;

        // ── 玩家标识 ──
        decoy.entityData.set(OWNER_UUID, player.getStringUUID());
        decoy.entityData.set(OWNER_NAME, player.getGameProfile().getName());

        // Alex / Steve 判定（Mojang 官方算法：UUID hashCode 奇偶位）
        boolean slim = false;
        UUID uuid = player.getGameProfile().getId();
        if (uuid != null) {
            slim = (uuid.hashCode() & 1) == 1;
        }
        decoy.entityData.set(IS_SLIM, slim);

        // ── 名称标签 ──
        decoy.setCustomName(Component.literal(player.getGameProfile().getName()));
        decoy.setCustomNameVisible(true);

        // ── 血量（上限 + 当前） ──
        float maxHp = player.getMaxHealth();
        // getAttribute() 在非 null 断言后直接使用（1.21 属性实例不会为 null）
        decoy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHp);
        decoy.setHealth(player.getHealth());

        // ── 护甲值（复制计算后的总护甲，用于伤害减免） ──
        var armorAttr = decoy.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(player.getArmorValue());
        }

        // ── 装备（所有槽位原样复制，供渲染层显示） ──
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            decoy.setItemSlot(slot, player.getItemBySlot(slot).copy());
        }

        // ── 药水效果（排除羊符咒） ──
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        for (MobEffectInstance effect : effects) {
            if (effect.is(ChenMod.SHEEP_POWER)) continue;
            // NeoForge 1.21：MobEffectInstance 有复制构造函数
            decoy.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }

        // ── 存活时间 ──
        decoy.lifetimeTicks = lifetimeTicks;

        // ── 生成 ──
        serverLevel.addFreshEntity(decoy);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            ticksAlive++;
            if (lifetimeTicks > 0 && ticksAlive >= lifetimeTicks) {
                this.discard();
            }
        }
    }

    // ── 伤害 / 死亡 ──────────────────────────────────────────────────────────────
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    /**
     * NeoForge 1.21 中 dropCustomDeathLoot 签名：
     *   (ServerLevel, DamageSource, boolean)
     * 覆写为空，使分身死亡不掉落任何物品。
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        // 有意为空：不掉落装备或任何物品
    }

    /**
     * NeoForge 1.21 中 isInvulnerableTo 签名保持 (DamageSource)，
     * 直接调用 super 保留虚空等原版豁免，其余全部允许伤害。
     */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return super.isInvulnerableTo(source);
    }

    // ── Getter（供渲染器使用） ────────────────────────────────────────────────────
    public String getOwnerUUID() { return this.entityData.get(OWNER_UUID); }
    public String getOwnerName() { return this.entityData.get(OWNER_NAME); }
    public boolean isSlim()      { return this.entityData.get(IS_SLIM);    }

    // ── NBT 序列化 ────────────────────────────────────────────────────────────────
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("OwnerUUID",     getOwnerUUID());
        tag.putString("OwnerName",     getOwnerName());
        tag.putBoolean("IsSlim",       isSlim());
        tag.putInt("LifetimeTicks",    lifetimeTicks);
        tag.putInt("TicksAlive",       ticksAlive);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(OWNER_UUID, tag.getString("OwnerUUID"));
        this.entityData.set(OWNER_NAME, tag.getString("OwnerName"));
        this.entityData.set(IS_SLIM,    tag.getBoolean("IsSlim"));
        this.lifetimeTicks = tag.getInt("LifetimeTicks");
        this.ticksAlive    = tag.getInt("TicksAlive");
    }

    // ── 杂项 ─────────────────────────────────────────────────────────────────────
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // 防止超出渲染距离后自动消失
    }
}