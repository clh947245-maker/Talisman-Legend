package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 虎符咒魔法效果
 * <p>
 * 分裂出一个攻击所有生物的实体。
 * 实体会检测此效果，如果效果消失，实体也会消失。
 * </p>
 */
public class TigerPowerMagic extends MobEffect {

    public TigerPowerMagic() {
        // 有益效果 (BENEFICIAL)，颜色 (橙色/黄色)
        super(MobEffectCategory.BENEFICIAL, 0xFFA500);
    }

    /**
     * API: 赋予实体虎符咒力量
     * <p>
     * 为指定的实体添加虎符咒魔法效果。
     * 所有的参数（visible, showIcon 等）都预设为 true。
     * </p>
     * 
     * @param entity   要赋予效果的实体（通常是玩家）
     * @param duration 效果持续时间（单位：ticks）
     */
    public static void grantTigerPower(LivingEntity entity, int duration) {
        if (entity == null) return;
        
        entity.addEffect(new MobEffectInstance(
            ChenMod.TIGER_POWER, 
            duration, 
            0, 
            true, 
            true, 
            true
        ));
    }
}
