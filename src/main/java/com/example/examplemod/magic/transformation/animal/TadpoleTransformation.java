package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractWaterDependentTransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 蝌蚪形态
 */
public class TadpoleTransformation extends AbstractWaterDependentTransformation {

    @Override
    public String getId() {
        return "tadpole";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.TADPOLE;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.TADPOLE.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.65F;
    }

    @Override
    public float getHealth() {
        return 6.0f;
    }
}
