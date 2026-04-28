package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

/**
 * 狗的能力魔法实现。
 */
public class DogPowerMagic extends MobEffect {
    private static final float MIN_HEALTH = 1.0F;

    public DogPowerMagic() {
        super(MobEffectCategory.BENEFICIAL, 0xC6A300);
    }

    /**
     * Caps post-mitigation damage so the holder never drops below half a heart.
     */
    public static void clampDamageToHalfHeart(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ChenMod.DOG_POWER.getHolder().orElseThrow())) {
            return;
        }

        float currentHealth = entity.getHealth();
        float maxDamage = Math.max(currentHealth - MIN_HEALTH, 0.0F);
        if (event.getAmount() > maxDamage) {
            event.setAmount(maxDamage);
        }
    }

    /**
     * API: grants the dog power effect for the given duration.
     */
    public static void grantDogPower(LivingEntity entity, int duration) {
        if (entity == null) {
            return;
        }

        entity.addEffect(new MobEffectInstance(
                ChenMod.DOG_POWER.getHolder().orElseThrow(),
                duration,
                0,
                false,
                false,
                false
        ));
    }
}
