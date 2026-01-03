package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;

/**
 * 鸡形态
 */
public class ChickenTransformation implements ITransformation {

    @Override
    public String getId() {
        return "chicken";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.CHICKEN;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        // 鸡的尺寸: 宽 0.4, 高 0.7
        return EntityType.CHICKEN.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        // 鸡的眼睛高度
        return dimensions.height() * 0.85F;
    }

    @Override
    public float getHealth() {
        return 4.0f; // 鸡只有 2 颗心
    }

    @Override
    public void onTick(LivingEntity entity) {
        // 鸡的特殊能力：滑翔 (Slow Falling)
        // 逻辑参考 Chicken.aiStep()
        if (!entity.onGround() && entity.getDeltaMovement().y < 0.0D) {
            // 减缓下落速度
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
            entity.fallDistance = 0; // 避免摔落伤害
        }
        
        // 鸡的特殊能力：自动浮水
        if (entity.isInWater()) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0D, 0.05D, 0.0D));
        }
    }

    @Override
    public void syncAnimation(Player player, LivingEntity dummy, boolean isNewTick) {
        if (dummy instanceof Chicken chicken) {
            if (isNewTick) {
                chicken.oFlap = chicken.flap;

                // 模仿原版鸡的翅膀逻辑
                float delta = (player.onGround() ? -1.0F : 4.0F) * 0.3F;
                chicken.flapSpeed += delta;
                chicken.flapSpeed = Mth.clamp(chicken.flapSpeed, 0.0F, 1.0F);

                if (!player.onGround() && chicken.flapSpeed < 1.0F) {
                    chicken.flapSpeed = 1.0F;
                }

                chicken.flapSpeed *= 0.9F;
                chicken.flap += chicken.flapSpeed * 2.0F;
            }
        }
    }
}
