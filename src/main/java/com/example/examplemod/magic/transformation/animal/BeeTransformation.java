package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractFlyingTransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 蜜蜂形态
 */
public class BeeTransformation extends AbstractFlyingTransformation {

    @Override
    public String getId() {
        return "bee";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.BEE;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.BEE.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.6F;
    }

    @Override
    public float getHealth() {
        return 10.0f;
    }
}
