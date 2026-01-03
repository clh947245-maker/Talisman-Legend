package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WolfTransformation implements ITransformation {

    @Override
    public String getId() {
        return "wolf";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.WOLF;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.WOLF.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.85F;
    }

    @Override
    public float getHealth() {
        return 8.0f;
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (entity.level().isClientSide || entity.tickCount % 10 != 0) return;

        // Scare Skeletons
        List<Skeleton> skeletons = entity.level().getEntitiesOfClass(Skeleton.class, entity.getBoundingBox().inflate(6));
        for (Skeleton skeleton : skeletons) {
            // If targeting the player, stop attacking
            if (skeleton.getTarget() == entity) {
                skeleton.setTarget(null);
            }
            
            // Run away logic
            PathNavigation navigation = skeleton.getNavigation();
            // Don't interrupt if already moving? Or override? Let's override to ensure they run.
            
            Vec3 skeletonPos = skeleton.position();
            Vec3 playerPos = entity.position();
            Vec3 awayDir = skeletonPos.subtract(playerPos).normalize();
            Vec3 targetPos = skeletonPos.add(awayDir.scale(4)); // Run 4 blocks away
            
            navigation.moveTo(targetPos.x, targetPos.y, targetPos.z, 1.5D);
        }
    }
}
