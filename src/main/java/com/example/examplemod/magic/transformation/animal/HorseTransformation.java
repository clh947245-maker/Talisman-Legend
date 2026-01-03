package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class HorseTransformation implements ITransformation {

    @Override
    public String getId() {
        return "horse";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.HORSE;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.HORSE.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.95F;
    }

    @Override
    public float getHealth() {
        return 20.0f;
    }

    @Override
    public void onTick(LivingEntity entity) {
        // 在 1.21+ 版本，使用 Attributes.STEP_HEIGHT 属性来控制最大步高
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        if (attribute != null && !attribute.hasModifier(STEP_HEIGHT_MODIFIER_ID)) {
             attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                 STEP_HEIGHT_MODIFIER_ID, 0.4, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
    }
    
    private static final net.minecraft.resources.ResourceLocation STEP_HEIGHT_MODIFIER_ID = 
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("chen_mod", "horse_step_height");

    @Override
    public void onRemove(LivingEntity entity) {
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        if (attribute != null) {
            attribute.removeModifier(STEP_HEIGHT_MODIFIER_ID);
        }
    }
}
