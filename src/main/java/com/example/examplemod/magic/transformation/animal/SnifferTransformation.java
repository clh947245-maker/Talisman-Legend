package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.ITransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 嗅探兽变身形态
 *
 * 玩家变身为嗅探兽，拥有更高的生命值。
 */
public class SnifferTransformation implements ITransformation {

    @Override
    public String getId() {
        return "sniffer";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.SNIFFER;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.SNIFFER.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.6F; // Sniffer eyes are relatively low
    }

    @Override
    public float getHealth() {
        return 28.0f;
    }

    @Override
    public void onTick(LivingEntity entity) {
    }
}
