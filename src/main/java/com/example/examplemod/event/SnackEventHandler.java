package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.SnackPowerMagic;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingVisibilityEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/**
 * 处理服务端事件，如生物仇恨和可见性逻辑
 */
@EventBusSubscriber(modid = ChenMod.MODID)
public class SnackEventHandler {

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        SnackPowerMagic.revealAfterAttack(event.getEntity());
    }

    /**
     * 当生物尝试改变目标时触发
     * 如果新目标拥有蛇符咒效果，则阻止该目标变更。
     */
    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        // 调用 SnackPowerMagic 中的逻辑
        SnackPowerMagic.onChangeTarget(event);
    }

    /**
     * 计算生物可见性时触发
     * 如果生物拥有蛇符咒效果，将其可见性设为 0，使 AI 很难发现它。
     */
    @SubscribeEvent
    public static void onLivingVisibility(LivingVisibilityEvent event) {
        // 调用 SnackPowerMagic 中的逻辑
        SnackPowerMagic.onVisibility(event);
    }
}
