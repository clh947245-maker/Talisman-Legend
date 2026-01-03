package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Fox;
import java.lang.reflect.Method;

/**
 * 狐狸形态
 */
public class FoxTransformation implements ITransformation {

    private static Method setSleepingMethod;
    private static Method setSittingMethod;

    static {
        try {
            setSleepingMethod = Fox.class.getDeclaredMethod("setSleeping", boolean.class);
            setSleepingMethod.setAccessible(true);
            setSittingMethod = Fox.class.getDeclaredMethod("setSitting", boolean.class);
            setSittingMethod.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String getId() {
        return "fox";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.FOX;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.FOX.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.6F;
    }

    @Override
    public float getHealth() {
        return 10.0f; // 5 hearts
    }

    private static final net.minecraft.resources.ResourceLocation SPEED_MODIFIER_ID = 
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("chen_mod", "fox_speed");

    @Override
    public void onTick(LivingEntity entity) {
        // 速度效果 (Speed II approx +40%)
        var speedAttr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speedAttr != null && !speedAttr.hasModifier(SPEED_MODIFIER_ID)) {
             speedAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                 SPEED_MODIFIER_ID, 0.4, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        // Jump logic handled in onJump or manually if attribute fails
    }

    @Override
    public void onJump(LivingEntity entity) {
        // 如果没有跳跃属性（例如玩家），手动提升跳跃高度
        if (entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH) == null) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.2, 0)); // Approx Jump Boost II
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        var speedAttr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    @Override
    public void syncAnimation(Player player, LivingEntity dummy, boolean isNewTick) {
        if (dummy instanceof Fox fox) {
             // 狐狸动画
             fox.setIsCrouching(player.isShiftKeyDown());
             try {
                 if (setSleepingMethod != null) {
                    setSleepingMethod.invoke(fox, player.isSleeping());
                 }
             } catch (Exception e) {
                 // ignore
             }
             
             // 坐下
             // FoxModel 依赖于 entity.isSitting()
             boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6;
             if (player.isShiftKeyDown() && !isMoving) { 
                  // 潜行且未移动视为坐下
                  try {
                      if (setSittingMethod != null) {
                        setSittingMethod.invoke(fox, true);
                      }
                  } catch (Exception e) {
                      // ignore
                  }
                  fox.setIsCrouching(false); // 坐下时取消蹲伏
             } else {
                  // 取消坐下
                  try {
                      if (setSittingMethod != null) {
                        setSittingMethod.invoke(fox, false);
                      }
                  } catch (Exception e) {
                      // ignore
                  }
             }
        }
    }
}