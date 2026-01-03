package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class CowTransformation implements ITransformation {

    @Override
    public String getId() {
        return "cow";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.COW;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.COW.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.95F;
    }

    @Override
    public float getHealth() {
        return 10.0f;
    }

    @Override
    public void onTick(LivingEntity entity) {
    }
}
