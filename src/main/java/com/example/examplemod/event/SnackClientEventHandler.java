package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.SheepBodyEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RenderLivingEvent;

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
        if (event.getEntity().hasEffect(ChenMod.SNACK_POWER.getHolder().orElseThrow())
                || event.getEntity() instanceof SheepBodyEntity bodyEntity && bodyEntity.isSnackInvisible()) {
            // 取消渲染，实体将不可见
            event.setCanceled(true);
        }
    }
}
