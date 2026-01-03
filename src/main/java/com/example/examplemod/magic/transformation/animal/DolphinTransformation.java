package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractWaterDependentTransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import java.lang.reflect.Field;

/**
 * 海豚形态
 */
public class DolphinTransformation extends AbstractWaterDependentTransformation {

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
        return "dolphin";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.DOLPHIN;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.DOLPHIN.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.4F;
    }

    @Override
    public float getHealth() {
        return 20.0f; // 10 hearts
    }

    private static final net.minecraft.resources.ResourceLocation SWIM_SPEED_MODIFIER_ID = 
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("chen_mod", "dolphin_swim_speed");

    @Override
    public void onTick(LivingEntity entity) {
        super.onTick(entity);
        
        // 海豚特性：在水中获得速度提升 (Simulating Dolphin's Grace)
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            if (entity.isInWater()) {
                if (!attribute.hasModifier(SWIM_SPEED_MODIFIER_ID)) {
                    attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        SWIM_SPEED_MODIFIER_ID, 0.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            } else {
                if (attribute.hasModifier(SWIM_SPEED_MODIFIER_ID)) {
                    attribute.removeModifier(SWIM_SPEED_MODIFIER_ID);
                }
            }
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        var attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(SWIM_SPEED_MODIFIER_ID);
        }
    }

    @Override
    public void syncAnimation(net.minecraft.world.entity.player.Player player, LivingEntity dummy, boolean isNewTick) {
        if (dummy instanceof net.minecraft.world.entity.animal.Dolphin dolphin) {
            // 海豚游泳姿态
            if (player.isSwimming()) {
                // 当玩家游泳时，强制海豚模型保持水平 (无俯仰角)
                dolphin.setXRot(0);
                dolphin.xRotO = 0;
            }

            // 修复海豚游泳动画：确保实体被认为是在水中
            try {
                if (wasTouchingWaterField != null) {
                    wasTouchingWaterField.setBoolean(dolphin, player.isInWater());
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
