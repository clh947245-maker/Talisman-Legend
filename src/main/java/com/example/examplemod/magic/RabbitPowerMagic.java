package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.world.effect.MobEffectInstance;

/**
 * 魔法效果："兔子的力量"
 * <p>
 * 提供极高的移动速度加成 (400%)。
 * </p>
 */
public class RabbitPowerMagic extends MobEffect {
    // 兔符咒提升的速度
    public static final double speed = 4.0;

    // 属性修饰符的唯一ID
    public static final ResourceLocation RABBIT_POWER_SPEED_MODIFIER_ID = ResourceLocation
            .fromNamespaceAndPath(ChenMod.MODID, "rabbit_power_speed");

    /**
     * API: 赋予实体兔子力量效果 (指定持续时间)
     * 
     * @param entity 目标实体
     * @param duration 持续时间 (ticks)
     */
    public static void grantRabbitPower(LivingEntity entity, int duration) {
        if (entity == null)
            return;

        // showIcon = false (不显示图标), visible = false (不显示粒子), ambient = false
        // 使用 IClientMobEffectExtensions 来完全隐藏图标
        entity.addEffect(new MobEffectInstance(
                ChenMod.RABBIT_POWER,
                duration,
                0,
                false,
                false,
                false));
    }

    public RabbitPowerMagic() {
        // BENEFICIAL (有益), 颜色 (白色/淡粉色)
        super(MobEffectCategory.BENEFICIAL, 0xFFE0E0);

        // 添加属性修饰符
        // 移动速度 +200% (Multiplier 2.0)
        // 使用 ADD_MULTIPLIED_BASE 以匹配 RabbitTalismanItem 的实现逻辑 (基础值 * (1 + multiplier))
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                RABBIT_POWER_SPEED_MODIFIER_ID, speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // 飞行速度 +200%
        // 注意：FLYING_SPEED (generic.flying_speed) 只影响鹦鹉等生物的飞行速度
        // 对于玩家，创造模式飞行速度由 abilities.flySpeed 控制，鞘翅飞行由其他机制控制
        // 但对于一些 Mod 添加的飞行能力，这个属性可能有效
        this.addAttributeModifier(Attributes.FLYING_SPEED,
                RABBIT_POWER_SPEED_MODIFIER_ID, speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // 游泳速度 +200% (RabbitTalismanItem 中也包含了游泳速度)
        this.addAttributeModifier(Attributes.WATER_MOVEMENT_EFFICIENCY,
                RABBIT_POWER_SPEED_MODIFIER_ID, speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // 每 tick 执行
    }
}
