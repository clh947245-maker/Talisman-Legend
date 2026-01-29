package com.example.examplemod.client;

import com.example.examplemod.ChenMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.example.examplemod.client.renderer.TigerCloneRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;

@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 使用自定义的 TigerCloneRenderer 来渲染分身，使其看起来像玩家
        event.registerEntityRenderer(ChenMod.TIGER_CLONE.get(), TigerCloneRenderer::new);
        
        // 注册龙爆破实体的渲染器 (使用投掷物品渲染器，渲染为火焰弹)
        event.registerEntityRenderer(ChenMod.DRAGON_FIREBALL.get(), ThrownItemRenderer::new);
    }
}
