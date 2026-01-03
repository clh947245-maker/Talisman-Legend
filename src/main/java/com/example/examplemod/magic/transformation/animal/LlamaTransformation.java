package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

public class LlamaTransformation implements ITransformation {

    @Override
    public String getId() {
        return "llama";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.LLAMA;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.LLAMA.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.95F;
    }

    @Override
    public float getHealth() {
        return 15.0f; // Llamas can have up to 30 health, but we set a standard base
    }

    @Override
    public void onTick(LivingEntity entity) {
    }
}
