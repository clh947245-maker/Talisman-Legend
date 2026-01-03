package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractFlyingTransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 悦灵形态
 */
public class AllayTransformation extends AbstractFlyingTransformation {

    @Override
    public String getId() {
        return "allay";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.ALLAY;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.ALLAY.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.6F;
    }

    @Override
    public float getHealth() {
        return 4.0f;
    }

    @Override
    public void onTick(LivingEntity entity) {
        super.onTick(entity); // Handle flight logic

        // 每秒自动回血 (20 ticks)
        if (entity.tickCount % 20 == 0) {
            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(1.0f);
            }
        }
    }
}
