package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Field;

public class RabbitTransformation implements ITransformation {

    private static Field jumpTicksField;
    private static Field jumpDurationField;

    static {
        try {
            jumpTicksField = Rabbit.class.getDeclaredField("jumpTicks");
            jumpTicksField.setAccessible(true);
            jumpDurationField = Rabbit.class.getDeclaredField("jumpDuration");
            jumpDurationField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String getId() {
        return "rabbit";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.RABBIT;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.RABBIT.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.95F;
    }

    @Override
    public float getHealth() {
        return 3.0f;
    }

    private static final ResourceLocation RABBIT_SPEED_ID = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "rabbit_transformation_speed");
    private static final AttributeModifier RABBIT_SPEED_MODIFIER = new AttributeModifier(
        RABBIT_SPEED_ID, 
        0.4, // +40% Speed (Equivalent to Speed II)
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    @Override
    public void onTick(LivingEntity entity) {
        // 兔子特性：速度 II (跑得快)
        // 使用属性修饰符实现，而不是药水效果
        AttributeInstance speedAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null && !speedAttribute.hasModifier(RABBIT_SPEED_ID)) {
            speedAttribute.addTransientModifier(RABBIT_SPEED_MODIFIER);
        }
    }
    
    @Override
    public void onJump(LivingEntity entity) {
        // 兔子特性：跳跃提升 (跳得高)
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, 1, 0));
    }
    
    @Override
    public void onRemove(LivingEntity entity) {
        // 移除速度修饰符
        AttributeInstance speedAttribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(RABBIT_SPEED_ID);
        }
    }

    @Override
    public void syncAnimation(Player player, LivingEntity dummy, boolean isNewTick) {
        if (dummy instanceof Rabbit rabbit) {
            boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
            boolean isInAir = !player.onGround();

            if (isMoving || isInAir) {
                // 使用系统时间模拟跳跃进度
                long time = System.currentTimeMillis();
                int duration = 10; // 0.5秒一个周期
                int ticks = (int) ((time / 50) % duration);

                try {
                    if (jumpTicksField != null) jumpTicksField.setInt(rabbit, ticks);
                    if (jumpDurationField != null) jumpDurationField.setInt(rabbit, duration);
                } catch (Exception e) {
                    // ignore
                }
            } else {
                try {
                    if (jumpTicksField != null) jumpTicksField.setInt(rabbit, 0);
                    if (jumpDurationField != null) jumpDurationField.setInt(rabbit, 0);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
}
