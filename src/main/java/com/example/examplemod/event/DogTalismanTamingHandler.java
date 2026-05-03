package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.config.ChenModLootConfig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.AnimalTameEvent;

/**
 * Drops the Dog Talisman when a wolf is successfully tamed.
 */
@EventBusSubscriber(modid = ChenMod.MODID)
public class DogTalismanTamingHandler {
    private static final float DEFAULT_DROP_CHANCE = 0.05F;

    @SubscribeEvent
    public static void onAnimalTame(AnimalTameEvent event) {
        if (!(event.getAnimal() instanceof Wolf wolf) || wolf.level().isClientSide()) {
            return;
        }

        float dropChance = ChenModLootConfig.getLootChance(ChenModLootConfig.DOG_WOLF_TAMING, DEFAULT_DROP_CHANCE);
        if (wolf.getRandom().nextFloat() < dropChance) {
            wolf.spawnAtLocation(new ItemStack(ChenMod.DOG_TALISMAN.get()));
        }
    }
}
