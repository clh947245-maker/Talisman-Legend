package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 骡子形态
 * <p>
 * 特性：速度较快，负重能力。
 * </p>
 */
public class MuleTransformation implements ITransformation {

    @Override
    public String getId() {
        return "mule";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.MULE;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.MULE.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.9F;
    }

    @Override
    public float getHealth() {
        return 20.0f; // 10 hearts
    }

    private static final net.minecraft.resources.ResourceLocation SPEED_MODIFIER_ID = 
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("chen_mod", "mule_speed");

    @Override
    public void onTick(LivingEntity entity) {
        // 速度效果 (Speed I approx +20%)
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attribute != null && !attribute.hasModifier(SPEED_MODIFIER_ID)) {
             attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                 SPEED_MODIFIER_ID, 0.2, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(SPEED_MODIFIER_ID);
        }
    }
}
