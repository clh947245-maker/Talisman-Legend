package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractFlyingTransformation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;

/**
 * 鹦鹉形态
 */
public class ParrotTransformation extends AbstractFlyingTransformation {

    @Override
    public String getId() {
        return "parrot";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.PARROT;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.PARROT.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.6F;
    }

    @Override
    public float getHealth() {
        return 6.0f;
    }

    // Cookie death logic is handled in TransformationEventHandler

    @Override
    public void syncAnimation(Player player, LivingEntity dummy, boolean isNewTick) {
        if (dummy instanceof Parrot parrot) {
            // 鹦鹉飞行逻辑 (与鸡类似)
            if (isNewTick) {
                parrot.oFlap = parrot.flap;

                float delta = (player.onGround() ? -1.0F : 4.0F) * 0.3F;
                parrot.flapSpeed += delta;
                parrot.flapSpeed = Mth.clamp(parrot.flapSpeed, 0.0F, 1.0F);

                if (!player.onGround() && parrot.flapSpeed < 1.0F) {
                    parrot.flapSpeed = 1.0F;
                }

                parrot.flapSpeed *= 0.9F;
                parrot.flap += parrot.flapSpeed * 2.0F;
            }
            // 确保鹦鹉不在坐下状态
            parrot.setOrderedToSit(false);
        }
    }
}
