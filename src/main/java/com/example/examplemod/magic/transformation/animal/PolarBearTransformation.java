package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 北极熊形态
 * <p>
 * 特性：高生命值，抗击退。
 * </p>
 */
public class PolarBearTransformation implements ITransformation {

    @Override
    public String getId() {
        return "polar_bear";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.POLAR_BEAR;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.POLAR_BEAR.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.85F;
    }

    @Override
    public float getHealth() {
        return 30.0f; // 15 hearts
    }

    private static final net.minecraft.resources.ResourceLocation ATTACK_DAMAGE_MODIFIER_ID = 
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("chen_mod", "polar_bear_attack");

    @Override
    public void onTick(LivingEntity entity) {
        // 力量效果 (Strength I approx +3 damage)
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attribute != null && !attribute.hasModifier(ATTACK_DAMAGE_MODIFIER_ID)) {
             attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                 ATTACK_DAMAGE_MODIFIER_ID, 3.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attribute != null) {
            attribute.removeModifier(ATTACK_DAMAGE_MODIFIER_ID);
        }
    }
}
