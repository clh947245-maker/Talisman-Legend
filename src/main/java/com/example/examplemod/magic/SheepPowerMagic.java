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
 *   - 穿墙（禁用物理碰撞）
 *   - 飞行（使用 CREATIVE_FLIGHT 属性实现创造模式飞行）
 *   - 单键起飞（按下空格直接飞行）
 *   - 增加水平移动速度
 *   - 穿墙不掉血（屏蔽窒息伤害）
 *   - 无敌状态（屏蔽所有伤害）
 *   - 禁止使用物品、与方块/实体交互（由 SheepPowerEventHandler 拦截）
 *
 * 飞行实现参考：AbstractFlyingTransformation
 */
public class SheepPowerMagic extends MobEffect {

    // 飞行属性修饰符的 ID
    private static final ResourceLocation SHEEP_FLIGHT_ID =
        ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_power_flight");

    // 水平移动速度修饰符的 ID（倍率叠加，1.5 = 速度提升 150%，即原速度的 2.5 倍）
    private static final ResourceLocation SHEEP_SPEED_ID =
        ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_power_speed");

    // 速度倍率：MULTIPLY_TOTAL 模式下 1.5 表示额外 +150%，即最终 2.5 倍原速度
    // 可按需调整，例如 0.5 = 1.5 倍速，1.0 = 2 倍速
    private static final double SPEED_MULTIPLIER = 1.5;

    // 反射获取 LivingEntity 的 jumping 字段，用于检测玩家是否按下跳跃键
    private static Field jumpingField;

    static {
        try {
            jumpingField = LivingEntity.class.getDeclaredField("jumping");
            jumpingField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            ChenMod.LOGGER.error("在 SheepPowerMagic 中无法访问 'jumping' 字段。", e);
        }
    }

    public SheepPowerMagic() {
        // 有益效果，颜色为灵魂蓝
        super(MobEffectCategory.BENEFICIAL, 0xA8D8FF);
    }

    /**
     * 赋予实体灵魂状态效果。
     *
     * @param entity   目标实体
     * @param duration 持续时间（ticks，20 ticks = 1 秒）
     */
    public static void grantSheepPower(LivingEntity entity, int duration) {
        if (entity == null) return;

        entity.addEffect(new MobEffectInstance(
            ChenMod.SHEEP_POWER,
            duration,
            0,
            false,
            false,
            true
        ));
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    /**
     * 每 tick 施加灵魂状态：
     *   1. 禁用碰撞（穿墙）
     *   2. 清除自身所有其他效果
     *   3. 屏蔽窒息伤害（穿墙时与方块重叠不掉血）
     *   4. 无敌状态（屏蔽所有伤害）
     *   5. 使用 CREATIVE_FLIGHT 属性赋予飞行能力
     *   6. 增加水平移动速度
     *   7. 单键起飞（按下空格直接飞行）
     *   8. 悬浮：未飞行时抵消重力；按住 Shift 则下降
     *   9. 完全隐身（隐藏玩家模型）
     */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player)) return true;

        // 1. 穿墙：禁用物理碰撞箱
        player.noPhysics = true;

        // 2. 清除自身所有其他效果（保留羊符咒本身）
        removeAllOtherEffects(player);

        // 3. 屏蔽窒息伤害：每 tick 刷新无敌帧
        // Minecraft 窒息伤害在无敌帧 > 0 时会被完全屏蔽
        // 设为 20（1 秒）确保每 tick 都覆盖，不影响其他有意义的伤害（如玩家攻击）
        if (player.invulnerableTime < 20) {
            player.invulnerableTime = 20;
        }

        // 4. 无敌状态：设置玩家为无敌模式，屏蔽所有伤害
        if (!player.getAbilities().invulnerable) {
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
        }

        // 5. 使用 CREATIVE_FLIGHT 属性赋予飞行能力
        var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flightAttribute != null && !flightAttribute.hasModifier(SHEEP_FLIGHT_ID)) {
            flightAttribute.addTransientModifier(
                new AttributeModifier(SHEEP_FLIGHT_ID, 1.0, AttributeModifier.Operation.ADD_VALUE)
            );
        }

        // 6. 增加水平移动速度（MULTIPLY_TOTAL 模式：在所有加法修饰后再乘以倍率）
        var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null && !speedAttribute.hasModifier(SHEEP_SPEED_ID)) {
            speedAttribute.addTransientModifier(
                new AttributeModifier(SHEEP_SPEED_ID, SPEED_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            );
        }

        // 7. 单键起飞：按下空格直接进入飞行状态
        // （noPhysics=true 时玩家无法正常跳跃，因此直接拦截跳跃键激活飞行）
        if (isJumping(player) && !player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        // 8. 悬浮 / 重力控制（仅在未激活创造飞行时生效）
        if (!player.getAbilities().flying) {
            if (player.isShiftKeyDown()) {
                // 按住 Shift：下降速度与创造飞行空格上升速度一致（固定 -0.6）
                player.setDeltaMovement(
                    player.getDeltaMovement().x,
                    -0.6,
                    player.getDeltaMovement().z
                );
            } else {
                // 默认悬浮：清除 Y 轴速度，完全抵消重力
                player.setDeltaMovement(
                    player.getDeltaMovement().x,
                    0.0,
                    player.getDeltaMovement().z
                );
            }
        }

        return true;
    }



    /**
     * 检测玩家是否正在按下跳跃键（空格）
     *
     * @param entity 实体
     * @return 如果正在跳跃返回 true
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
     * 清除玩家身上所有其他效果，仅保留羊符咒本身。
     * 这样可以确保灵魂状态不受其他药水效果干扰。
     *
     * @param player 玩家实体
     */
    private void removeAllOtherEffects(Player player) {
        // 获取玩家当前所有效果的副本列表，避免遍历时修改集合
        List<MobEffectInstance> activeEffects = new ArrayList<>(player.getActiveEffects());

        for (MobEffectInstance effectInstance : activeEffects) {
            // 跳过羊符咒本身，保留灵魂状态
            if (effectInstance.is(ChenMod.SHEEP_POWER)) {
                continue;
            }

            // 移除其他所有效果
            Holder<MobEffect> effectHolder = effectInstance.getEffect();
            player.removeEffect(effectHolder);
        }
    }

    /**
     * 效果结束时恢复玩家正常物理与飞行状态。
     * 创造 / 旁观模式不受影响。
     */
    public static void restorePlayer(Player player) {
        if (player == null) return;

        // 恢复碰撞
        player.noPhysics = false;

        // 恢复无敌帧（清零，让正常伤害逻辑重新生效）
        player.invulnerableTime = 0;

        if (!player.isCreative() && !player.isSpectator()) {
            // 撤销无敌状态
            player.getAbilities().invulnerable = false;

            // 撤销飞行能力
            var flightAttribute = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
            if (flightAttribute != null) {
                flightAttribute.removeModifier(SHEEP_FLIGHT_ID);
            }

            // 撤销速度加成
            var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                speedAttribute.removeModifier(SHEEP_SPEED_ID);
            }

            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }
}