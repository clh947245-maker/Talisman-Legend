package com.example.examplemod.magic.transformation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 水下生存形态基类
 * <p>
 * 赋予实体水下呼吸能力，使其可以在水下生存。
 * 适用于：鱼、海豚、海龟、蝌蚪、青蛙、鱿鱼。
 * </p>
 */
public abstract class AbstractWaterSurvivalTransformation implements ITransformation {

    @Override
    public void onTick(LivingEntity entity) {
        // 赋予水下呼吸能力 (无需药水效果)
        // 如果在水中，持续恢复氧气
        if (entity.isInWater()) {
            entity.setAirSupply(entity.getMaxAirSupply());
        }
    }
}
