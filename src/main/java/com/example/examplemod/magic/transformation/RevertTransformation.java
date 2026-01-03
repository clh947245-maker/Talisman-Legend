package com.example.examplemod.magic.transformation;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 变回人类形态
 */
public class RevertTransformation implements ITransformation {

    @Override
    public String getId() {
        return "revert";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.PLAYER;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.PLAYER.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return EntityType.PLAYER.getDimensions().height() * 0.85F;
    }

    @Override
    public void onTick(LivingEntity entity) {
        // Do nothing
    }
}
