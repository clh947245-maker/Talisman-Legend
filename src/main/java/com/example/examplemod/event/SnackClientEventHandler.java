package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/**
 * 处理客户端事件，如渲染逻辑
 */
@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public class SnackClientEventHandler {

    /**
     * 在生物渲染之前触发
     * 如果生物拥有蛇符咒效果，取消渲染，实现完全的视觉隐身（包括装备）。
     */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity().hasEffect(ChenMod.SNACK_POWER)) {
            // 取消渲染，实体将不可见
            event.setCanceled(true);
        }
    }
}
