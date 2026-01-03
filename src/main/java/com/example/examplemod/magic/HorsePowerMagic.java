package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔法效果："马的力量"
 * <p>
 * 负责处理马符咒的具体魔法效果，包括持续清除负面状态和生命恢复。
 * </p>
 */
public class HorsePowerMagic extends MobEffect {

    /**
     * API: 赋予实体马力量效果 (指定持续时间)
     * 
     * @param entity 目标实体
     * @param duration 持续时间 (ticks)
     */
    public static void grantHorsePower(LivingEntity entity, int duration) {
        if (entity == null) return;
        
        // showIcon = false (显示图标), visible = false (不显示粒子), ambient = false
        // 使用 IClientMobEffectExtensions 来完全隐藏图标
        entity.addEffect(new MobEffectInstance(
            ChenMod.HORSE_POWER, 
            duration, 
            0, 
            false, 
            false,
            false
        ));
    }

    public HorsePowerMagic() {
        // 有益效果，颜色（白色）
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 每 tick 都执行，确保负面效果被立即清除
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity == null) return false;

        // 清除负面状态效果
        removeHarmfulEffects(entity);
        
        // 直接恢复生命值至最大值
        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }

        return true;
    }

    /**
     * 清除实体身上的负面状态效果
     *
     * @param entity 目标实体
     */
    public static void removeHarmfulEffects(LivingEntity entity) {
        // 创建一个列表来存储需要移除的效果，避免在遍历时修改集合
        List<MobEffectInstance> effectsToRemove = new ArrayList<>();

        for (MobEffectInstance effectInstance : entity.getActiveEffects()) {
            if (effectInstance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                effectsToRemove.add(effectInstance);
            }
        }

        for (MobEffectInstance effectInstance : effectsToRemove) {
            entity.removeEffect(effectInstance.getEffect());
        }
    }
}
