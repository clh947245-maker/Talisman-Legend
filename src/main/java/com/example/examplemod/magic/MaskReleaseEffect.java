package com.example.examplemod.magic;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class MaskReleaseEffect extends MobEffect {
    private static final int PARTICLE_COLOR = 0x39FF14;

    public MaskReleaseEffect() {
        super(MobEffectCategory.BENEFICIAL, PARTICLE_COLOR);
    }
}
