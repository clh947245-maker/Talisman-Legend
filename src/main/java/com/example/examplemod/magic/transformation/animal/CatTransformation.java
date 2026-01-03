package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CatTransformation implements ITransformation {

    @Override
    public String getId() {
        return "cat";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.CAT;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.CAT.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.85F;
    }

    @Override
    public float getHealth() {
        return 10.0f;
    }

    @Override
    public void onTick(LivingEntity entity) {
        // No fall damage
        entity.fallDistance = 0;

        if (entity.level().isClientSide || entity.tickCount % 10 != 0) return;

        // Scare Creepers
        List<Creeper> creepers = entity.level().getEntitiesOfClass(Creeper.class, entity.getBoundingBox().inflate(10));
        for (Creeper creeper : creepers) {
             if (creeper.getTarget() == entity) {
                creeper.setTarget(null);
            }

            PathNavigation navigation = creeper.getNavigation();
            
            Vec3 creeperPos = creeper.position();
            Vec3 playerPos = entity.position();
            Vec3 awayDir = creeperPos.subtract(playerPos).normalize();
            Vec3 targetPos = creeperPos.add(awayDir.scale(6));
            
            navigation.moveTo(targetPos.x, targetPos.y, targetPos.z, 1.5D);
        }
    }
}
