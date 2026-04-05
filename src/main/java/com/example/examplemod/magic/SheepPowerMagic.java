package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForgeMod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 魔法效果："羊的力量"
 *
 * 灵魂状态效果：
 * - 穿墙（禁用物理碰撞）
 * - 飞行（使用 CREATIVE_FLIGHT 属性实现创造模式飞行）
 * - 单键起飞（按下空格直接飞行）
 * - 增加水平移动速度
 * - 穿墙不掉血（屏蔽窒息伤害）
 * - 无敌状态（屏蔽所有伤害）
 * - 禁止使用物品、与方块/实体交互（由 SheepPowerEventHandler 拦截）
 *
 *
 * @author ChenMod
 */
public class SheepPowerMagic extends MobEffect {

    /**
     * 飞行属性修饰符的资源位置ID。
     * 用于唯一标识羊的力量提供的创造模式飞行能力。
     */
    private static final ResourceLocation SHEEP_FLIGHT_ID = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID,
            "sheep_power_flight");

    /**
     * 速度属性修饰符的资源位置ID。
     * 用于唯一标识羊的力量提供的移动速度加成。
     */
    private static final ResourceLocation SHEEP_SPEED_ID = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID,
            "sheep_power_speed");

    /**
     * 速度倍数。
     * 玩家的移动速度将增加基础速度的150%。
     */
    private static final double SPEED_MULTIPLIER = 1.5;

    /**
     * LivingEntity类中"jumping"字段的反射引用。
     * 用于检测玩家是否正在按下跳跃键（空格）。
     */
    private static Field jumpingField;

    /**
     * 静态初始化块。
     * 使用反射获取LivingEntity的jumping字段，用于检测跳跃输入。
     */
    static {
        try {
            jumpingField = LivingEntity.class.getDeclaredField("jumping");
            jumpingField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            ChenMod.LOGGER.error("在 SheepPowerMagic 中无法访问 'jumping' 字段。", e);
        }
    }

    /**
     * 构造方法。
     *
     * 创建有益效果，颜色为淡蓝色（0xA8D8FF）。
     */
    public SheepPowerMagic() {
        super(MobEffectCategory.BENEFICIAL, 0xA8D8FF);
    }

    /**
     * 给予实体羊的力量效果。
     *
     * 静态工具方法，用于方便地给予玩家羊的力量效果。
     * 效果参数：无环境效果、无粒子效果、显示图标。
     *
     * @param entity 目标实体
     * @param duration 效果持续时间（tick）
     */
    public static void grantSheepPower(LivingEntity entity, int duration) {
        if (entity == null) return;
        entity.addEffect(new MobEffectInstance(
                ChenMod.SHEEP_POWER, duration, 0, false, false, true));
    }

    /**
     * 判断是否应该在此tick应用效果。
     *
     * @param duration 剩余持续时间
     * @param amplifier 效果等级
     * @return 始终返回true，每tick都执行
     */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    /**
     * 每tick应用效果逻辑。
     *
     * 实现羊的力量的核心功能：
     * 1. 穿墙（禁用物理碰撞）
     * 2. 清除其他所有效果
     * 3. 屏蔽窒息伤害
     * 4. 无敌状态
     * 5. 飞行能力
     * 6. 移动速度加成
     * 7. 单键起飞（空格直接飞行）
     * 8. 悬浮/重力控制（按Shift下降，否则悬浮）
     *
     * @param entity 效果作用的实体
     * @param amplifier 效果等级
     * @return 始终返回true
     */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player)) return true;

        // 1. 穿墙 - 禁用物理碰撞，允许穿过方块
        player.noPhysics = true;

        // 2. 清除其他效果 - 保持只有羊的力量效果
        removeAllOtherEffects(player);

        // 3. 屏蔽窒息伤害 - 设置无敌时间防止窒息伤害
        if (player.invulnerableTime < 20) {
            player.invulnerableTime = 20;
        }

        // 4. 无敌 - 设置创造模式的无敌能力
        if (!player.getAbilities().invulnerable) {
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
        }

        // 5. 飞行 - 添加创造模式飞行属性修饰符
        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null && !flightAttribute.hasModifier(SHEEP_FLIGHT_ID)) {
            flightAttribute.addTransientModifier(
                    new AttributeModifier(SHEEP_FLIGHT_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
        }

        // 6. 速度 - 添加移动速度属性修饰符
        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null && !speedAttribute.hasModifier(SHEEP_SPEED_ID)) {
            speedAttribute.addTransientModifier(
                    new AttributeModifier(SHEEP_SPEED_ID, SPEED_MULTIPLIER,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        // 7. 单键起飞 - 按下空格键直接开始飞行
        if (isJumping(player) && !player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        // 8. 悬浮/重力控制
        // 如果没有在飞行状态，则控制垂直移动
        if (!player.getAbilities().flying) {
            if (player.isShiftKeyDown()) {
                // 按Shift下降
                player.setDeltaMovement(player.getDeltaMovement().x, -0.6, player.getDeltaMovement().z);
            } else {
                // 否则悬浮（抵消重力）
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
            }
        }

        return true;
    }

    /**
     * 检查实体是否正在跳跃。
     *
     * 使用反射访问LivingEntity的jumping字段。
     *
     * @param entity 要检查的实体
     * @return 如果正在跳跃返回true，否则返回false
     */
    private boolean isJumping(LivingEntity entity) {
        if (jumpingField == null) return false;
        try {
            return jumpingField.getBoolean(entity);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /**
     * 移除玩家身上除羊的力量外的所有效果。
     *
     * 遍历玩家所有活跃效果，移除非羊的力量效果。
     *
     * @param player 目标玩家
     */
    private void removeAllOtherEffects(Player player) {
        List<MobEffectInstance> activeEffects = new ArrayList<>(player.getActiveEffects());
        for (MobEffectInstance effectInstance : activeEffects) {
            if (effectInstance.is(ChenMod.SHEEP_POWER)) continue;
            Holder<MobEffect> effectHolder = effectInstance.getEffect();
            player.removeEffect(effectHolder);
        }
    }

    /**
     * 恢复玩家到正常状态。
     *
     * 当羊的力量效果结束时调用，恢复玩家的所有属性：
     * - 恢复物理碰撞
     * - 移除无敌时间
     * - 移除飞行和速度属性修饰符
     * - 恢复飞行能力（如果不是创造模式或观察者模式）
     *
     * @param player 目标玩家
     */
    public static void restorePlayer(Player player) {
        if (player == null) return;

        // 恢复物理碰撞
        player.noPhysics = false;
        // 移除无敌时间
        player.invulnerableTime = 0;

        // 恢复无敌状态（创造模式本来就是无敌的，这里确保非创造模式恢复正常）
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().invulnerable = false;
        }

        // 移除飞行属性修饰符（所有模式都需要移除）
        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null) flightAttribute.removeModifier(SHEEP_FLIGHT_ID);

        // 移除速度属性修饰符（所有模式都需要移除）
        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) speedAttribute.removeModifier(SHEEP_SPEED_ID);

        // 如果不是创造模式或观察者模式，停止飞行
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }
}
