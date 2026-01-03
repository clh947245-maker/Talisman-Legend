package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractWaterDependentTransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import java.lang.reflect.Field;

/**
 * 鳕鱼形态
 */
public class CodTransformation extends AbstractWaterDependentTransformation {

    private static Field wasTouchingWaterField;

    static {
        try {
            wasTouchingWaterField = net.minecraft.world.entity.Entity.class.getDeclaredField("wasTouchingWater");
            wasTouchingWaterField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String getId() {
        return "cod";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.COD;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.COD.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.65F;
    }

    @Override
    public float getHealth() {
        return 6.0f; // 3 hearts
    }
}
