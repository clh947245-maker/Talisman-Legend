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
 * 魔法效果："牛的力量"
 * <p>
 * 提供巨大的力量加成 (+50 攻击力) 和击退增强 (+4 击退)。
 * </p>
 */
public class OxPowerMagic extends MobEffect {
    // 牛符咒提升的攻击力
    public static final double STRENGTH_AMOUNT = 50.0;
    // 牛符咒提升的击退距离
    public static final double KNOCKBACK_AMOUNT = 4.0;

    // 属性修饰符的唯一ID
    public static final ResourceLocation OX_POWER_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID,
            "ox_power_modifier");

    /**
     * API: 赋予实体牛力量效果 (指定持续时间)
     * 
     * @param entity   目标实体
     * @param duration 持续时间 (ticks)
     */
    public static void grantOxPower(LivingEntity entity, int duration) {
        if (entity == null)
            return;

        // showIcon = false (不显示图标), visible = false (不显示粒子), ambient = false
        // 使用 IClientMobEffectExtensions 来完全隐藏图标
        entity.addEffect(new MobEffectInstance(
                ChenMod.OX_POWER.getHolder().orElseThrow(),
                duration,
                0,
                false,
                false,
                false));
    }

    public OxPowerMagic() {
        // BENEFICIAL (有益), 颜色 (红色)
        super(MobEffectCategory.BENEFICIAL, 0xFF0000);

        // 添加属性修饰符
        // 攻击力 +50 (ADD_VALUE)
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE,
                OX_POWER_MODIFIER_ID, STRENGTH_AMOUNT, AttributeModifier.Operation.ADD_VALUE);

        // 击退 +4 (ADD_VALUE)
        this.addAttributeModifier(Attributes.ATTACK_KNOCKBACK,
                OX_POWER_MODIFIER_ID, KNOCKBACK_AMOUNT, AttributeModifier.Operation.ADD_VALUE);
    }
}
