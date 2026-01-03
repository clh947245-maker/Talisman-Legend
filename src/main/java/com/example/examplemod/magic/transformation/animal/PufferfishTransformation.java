package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractWaterSurvivalTransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class PufferfishTransformation extends AbstractWaterSurvivalTransformation {

    @Override
    public String getId() {
        return "pufferfish";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.PUFFERFISH;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.PUFFERFISH.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.65F;
    }

    @Override
    public float getHealth() {
        return 3.0f;
    }

    @Override
    public void onTick(LivingEntity entity) {
        super.onTick(entity);
        
        // 河豚在岸上会窒息 (Pufferfish suffocates on land)
        if (!entity.isInWaterOrBubble()) {
            int currentAir = entity.getAirSupply();
            
            // Vanilla LivingEntity increases air by 4 per tick when out of water.
            // We want to decrease it. To counteract +4 and decrease by 1, we subtract 5.
            int newAir = currentAir - 5;
            
            // Handle damage threshold (-20 is the standard drowning threshold)
            if (newAir <= -20) {
                newAir = 0;
                entity.hurt(entity.damageSources().dryOut(), 2.0F);
            }
            
            entity.setAirSupply(newAir);
        }
    }
}
