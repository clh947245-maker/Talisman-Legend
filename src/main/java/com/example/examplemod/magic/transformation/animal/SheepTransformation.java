package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 羊形态
 */
public class SheepTransformation implements ITransformation {

    @Override
    public String getId() {
        return "sheep";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.SHEEP;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        // 羊的尺寸
        return EntityType.SHEEP.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        // 羊的眼睛高度
        return dimensions.height() * 0.95F;
    }

    @Override
    public float getHealth() {
        return 8.0f; // 羊有 4 颗心
    }

    @Override
    public void onTick(LivingEntity entity) {
        // 羊目前没有特殊的 tick 逻辑，未来可以添加吃草回血等
    }
}
