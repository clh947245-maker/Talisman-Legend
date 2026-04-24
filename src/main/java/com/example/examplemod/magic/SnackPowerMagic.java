package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 蛇符咒魔法效果
 * <p>
 * 赋予隐身效果，并消除周围敌对生物的仇恨。
 * </p>
 */
public class SnackPowerMagic extends MobEffect {
    private static final int REVEAL_DURATION_TICKS = 20 * 5;
    private static final Map<UUID, Integer> REVEALED_UNTIL_TICK = new ConcurrentHashMap<>();

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
        if (newTarget != null && isSnackInvisible(newTarget)) {
            // 如果目标有蛇符咒效果，取消事件（即不让怪瞄准他）
            event.setCanceled(true);
        }
    }

    /**
     * 处理可见性事件 (隐身)
     */
    public static void onVisibility(LivingVisibilityEvent event) {
        if (isSnackInvisible(event.getEntity())) {
            // 将可见性系数修改为 0.0，相当于完全隐身
            event.modifyVisibility(0.0);
        }
    }

    public static void revealAfterAttack(Player player) {
        if (player == null || !player.hasEffect(ChenMod.SNACK_POWER)) {
            return;
        }

        REVEALED_UNTIL_TICK.put(player.getUUID(), player.tickCount + REVEAL_DURATION_TICKS);
    }

    public static boolean isSnackInvisible(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.hasEffect(ChenMod.SNACK_POWER)) {
            return false;
        }
        return !isRevealed(living);
    }

    public static boolean isRevealed(LivingEntity entity) {
        Integer revealedUntilTick = REVEALED_UNTIL_TICK.get(entity.getUUID());
        if (revealedUntilTick == null) {
            return false;
        }
        if (entity.tickCount <= revealedUntilTick) {
            return true;
        }
        REVEALED_UNTIL_TICK.remove(entity.getUUID(), revealedUntilTick);
        return false;
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
                if (isSnackInvisible(pLivingEntity) && mob.getTarget() == pLivingEntity) {
                    mob.setTarget(null);
                }
            });
        }
        return true;
    }
}
