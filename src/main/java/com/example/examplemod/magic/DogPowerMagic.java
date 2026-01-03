package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 魔法效果："狗的力量"
 * <p>
 * 赋予持有者不死之身。
 * 具体逻辑在 DogImmortalityHandler 中调用此类方法。
 * </p>
 */
public class DogPowerMagic extends MobEffect {

    /**
     * 处理死亡事件逻辑
     */
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // 检查是否拥有狗符咒效果
        if (entity.hasEffect(ChenMod.DOG_POWER)) {
            // 狗符咒防止死亡
            event.setCanceled(true);
            
            // 将生命值设置为 1.0 (半颗心)
            entity.setHealth(1.0f);
        }
    }

    /**
     * API: 赋予实体狗力量效果 (指定持续时间)
     */
    public static void grantDogPower(LivingEntity entity, int duration) {
        if (entity == null) return;
        
        entity.addEffect(new MobEffectInstance(
            ChenMod.DOG_POWER, 
            duration, 
            0, 
            false, 
            false, 
            false
        ));
    }

    public DogPowerMagic() {
        // BENEFICIAL (有益), 颜色 (金黄色)
        super(MobEffectCategory.BENEFICIAL, 0xC6A300);
    }
}
