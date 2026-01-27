package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent;

/**
 * 蛇符咒魔法效果
 * <p>
 * 赋予隐身效果，并消除周围敌对生物的仇恨。
 * </p>
 */
public class SnackPowerMagic extends MobEffect {

    public SnackPowerMagic() {
        // 有益效果，颜色（淡绿色）
        super(MobEffectCategory.BENEFICIAL, 0x90EE90);
    }

    /**
     * API: 赋予实体蛇符咒力量
     * 
     * @param entity   目标实体
     * @param duration 持续时间 (ticks)
     */
    public static void grantSnackPower(LivingEntity entity, int duration) {
        if (entity == null)
            return;
        entity.addEffect(new MobEffectInstance(
                ChenMod.SNACK_POWER,
                duration,
                0,
                false,
                false,
                true));
    }

    /**
     * 处理生物改变目标事件 (防止被瞄准)
     */
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget != null && newTarget.hasEffect(ChenMod.SNACK_POWER)) {
            // 如果目标有蛇符咒效果，取消事件（即不让怪瞄准他）
            event.setCanceled(true);
        }
    }

    /**
     * 处理可见性事件 (隐身)
     */
    public static void onVisibility(LivingVisibilityEvent event) {
        if (event.getEntity().hasEffect(ChenMod.SNACK_POWER)) {
            // 将可见性系数修改为 0.0，相当于完全隐身
            event.modifyVisibility(0.0);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // 每 tick 都执行
    }

    @Override
    public boolean applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        // 2. 主动消除仇恨 (作为 LivingChangeTargetEvent 的补充，处理已存在的仇恨)
        if (!pLivingEntity.level().isClientSide) {
            // 搜索周围 16 格内的生物
            AABB searchBox = pLivingEntity.getBoundingBox().inflate(16.0);
            pLivingEntity.level().getEntitiesOfClass(Mob.class, searchBox).forEach(mob -> {
                // 如果生物的目标是当前实体（玩家），则清除目标
                if (mob.getTarget() == pLivingEntity) {
                    mob.setTarget(null);
                }
            });
        }
        return true;
    }
}
