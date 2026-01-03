package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 驴形态
 * <p>
 * 特性：负重能力。
 * </p>
 */
public class DonkeyTransformation implements ITransformation {

    @Override
    public String getId() {
        return "donkey";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.DONKEY;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.DONKEY.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.85F;
    }

    @Override
    public float getHealth() {
        return 20.0f; // 10 hearts
    }

    private static final net.minecraft.resources.ResourceLocation ARMOR_MODIFIER_ID = 
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("chen_mod", "donkey_armor");

    @Override
    public void onTick(LivingEntity entity) {
        // 稍微增加一点抗性 (Armor +2)
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        if (attribute != null && !attribute.hasModifier(ARMOR_MODIFIER_ID)) {
             attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                 ARMOR_MODIFIER_ID, 2.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        if (attribute != null) {
            attribute.removeModifier(ARMOR_MODIFIER_ID);
        }
    }
}
