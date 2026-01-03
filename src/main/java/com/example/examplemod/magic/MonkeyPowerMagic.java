package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.transformation.ITransformation;
import com.example.examplemod.magic.transformation.TransformationManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
/**
 * 魔法效果："猴子的力量" (Monkey Power)
 *
 * 赋予实体变身能力。
 * 变身的目标形态由 Effect 的 Amplifier (等级) 决定。
 * 0: 鸡
 * 1: 羊
 * ...
 */
public class MonkeyPowerMagic extends MobEffect {
    /**
     * API: 赋予实体猴力量效果 (指定持续时间和变身ID)
     */
    public static void grantMonkeyPower(LivingEntity entity, int duration, int transformationId) {
        if (entity == null) return;
        
        // 移除旧效果以更新 Amplifier
        if (entity.hasEffect(ChenMod.MONKEY_POWER)) {
            entity.removeEffect(ChenMod.MONKEY_POWER);
        }

        entity.addEffect(new MobEffectInstance(
            ChenMod.MONKEY_POWER, 
            duration, 
            transformationId, // Amplifier 用于存储变身ID
            false, 
            false, 
            true
        ));
    }

    public MonkeyPowerMagic() {
        // NEUTRAL (中性), 颜色 (棕色)
        super(MobEffectCategory.BENEFICIAL, 0x8B4513);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 获取当前的变身逻辑并执行 tick 更新
        ITransformation transformation = TransformationManager.getTransformation(amplifier);
        if (transformation != null) {
            transformation.onTick(entity);
            
            // 仅在尺寸不匹配时刷新，或者每隔一段时间刷新一次，避免每 tick 刷新导致性能问题和网络拥堵
            if (entity.tickCount % 20 == 0) {
                 entity.refreshDimensions();
            }
        }
        return true;
    }


}
