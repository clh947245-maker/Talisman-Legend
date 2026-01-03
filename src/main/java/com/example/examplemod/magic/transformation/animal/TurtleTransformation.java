package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractWaterSurvivalTransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 海龟形态
 */
public class TurtleTransformation extends AbstractWaterSurvivalTransformation {

    @Override
    public String getId() {
        return "turtle";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.TURTLE;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.TURTLE.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.4F;
    }

    @Override
    public float getHealth() {
        return 30.0f; // High health
    }
}
