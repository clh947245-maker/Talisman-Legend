package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractWaterDependentTransformation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.player.Player;

/**
 * 鱿鱼形态
 */
public class SquidTransformation extends AbstractWaterDependentTransformation {

    @Override
    public String getId() {
        return "squid";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.SQUID;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.SQUID.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.5F;
    }

    @Override
    public float getHealth() {
        return 20.0f;
    }

    @Override
    public void syncAnimation(Player player, LivingEntity dummy, boolean isNewTick) {
        if (dummy instanceof Squid squid) {
            // 鱿鱼触手动画
            if (isNewTick) {
                squid.oldTentacleMovement = squid.tentacleMovement;
                squid.oldTentacleAngle = squid.tentacleAngle;

                squid.tentacleMovement += 1.0f / (player.walkAnimation.speed() > 0.1 ? 8.0f : 20.0f);
                squid.tentacleAngle = Mth.abs(Mth.sin(squid.tentacleMovement)) * (float) Math.PI * 0.25F;

                squid.xBodyRot = player.getXRot();
                squid.xBodyRotO = player.xRotO;
            }
        }
    }
}
