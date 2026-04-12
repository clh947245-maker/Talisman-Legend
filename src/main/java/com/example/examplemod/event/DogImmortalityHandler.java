package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.DogPowerMagic;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Handles the Dog Talisman health floor.
 */
@EventBusSubscriber(modid = ChenMod.MODID)
public class DogImmortalityHandler {

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        DogPowerMagic.clampDamageToHalfHeart(event);
    }
}
