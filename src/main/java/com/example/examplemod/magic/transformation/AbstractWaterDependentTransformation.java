package com.example.examplemod.magic.transformation;

import net.minecraft.world.entity.LivingEntity;

/**
 * 水生依赖形态基类
 * <p>
 * 继承自水下生存形态。
 * 额外增加逻辑：如果离开水（和雨），会像鱿鱼一样窒息（扣血）。
 * 适用于：鱼、海豚、蝌蚪、鱿鱼。
 * </p>
 */
public abstract class AbstractWaterDependentTransformation extends AbstractWaterSurvivalTransformation {

    @Override
    public void onTick(LivingEntity entity) {
        super.onTick(entity);

        if (!entity.isInWaterOrRain()) {
            // 在陆地上：模拟窒息
            // 由于 LivingEntity.tick() 会在 tickEffects() 之后运行，并自动恢复空气 (+4)
            // 所以我们需要扣除更多，以抵消恢复量并实现净减少 (-1)
            // Net change: -5 + 4 = -1
            int airSupply = entity.getAirSupply() - 5;
            entity.setAirSupply(airSupply);

            // 如果空气耗尽 (-20 ticks = 1秒)，造成伤害
            if (airSupply <= -20) {
                entity.setAirSupply(0);
                entity.hurt(entity.damageSources().dryOut(), 2.0f);
            }
        } else {
            // 在水中：恢复空气值
            entity.setAirSupply(entity.getMaxAirSupply());
        }
    }
}
