package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.DogPowerMagic;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

/**
 * Handles the Dog Talisman health floor.
 */
@EventBusSubscriber(modid = ChenMod.MODID)
public class DogImmortalityHandler {

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent event) {
        DogPowerMagic.clampDamageToHalfHeart(event);
    }
}
