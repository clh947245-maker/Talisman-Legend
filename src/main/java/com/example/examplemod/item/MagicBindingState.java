package com.example.examplemod.item;

import com.example.examplemod.ChenMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class MagicBindingState {
    private MagicBindingState() {
    }

    public static boolean hasRemovalAccess(@Nullable Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return false;
        }

        return hasSpecialRemovalEffect(livingEntity);
    }

    private static boolean hasSpecialRemovalEffect(LivingEntity livingEntity) {
        return livingEntity.hasEffect(ChenMod.MASK_RELEASE.getHolder().orElseThrow());
    }
}
