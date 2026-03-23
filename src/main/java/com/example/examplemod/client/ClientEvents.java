package com.example.examplemod.client;

import com.example.examplemod.ChenMod;
import com.example.examplemod.client.renderer.PlayerDecoyRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端事件处理器
 *
 * 用于注册客户端渲染器等客户端专用内容。
 */
@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public class ClientEvents {

    /**
     * 注册实体渲染器
     */
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ChenMod.PLAYER_DECOY.get(), PlayerDecoyRenderer::new);
    }
}
