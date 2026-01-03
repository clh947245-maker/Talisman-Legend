package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class PigTransformation implements ITransformation {

    @Override
    public String getId() {
        return "pig";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.PIG;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.PIG.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.9F;
    }

    @Override
    public float getHealth() {
        return 10.0f;
    }

    @Override
    public void onTick(LivingEntity entity) {
    }
}
