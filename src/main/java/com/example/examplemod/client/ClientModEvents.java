package com.example.examplemod.client;

import com.example.examplemod.ChenMod;
import com.example.examplemod.client.renderer.DragonFireballRenderer;
import com.example.examplemod.client.renderer.LivingBlockRenderer;
import com.example.examplemod.client.renderer.MouseBeamRenderer;
import com.example.examplemod.client.renderer.PigLaserRenderer;
import com.example.examplemod.client.renderer.SheepBodyRenderer;
import com.example.examplemod.client.renderer.TigerCloneRenderer;
import com.example.examplemod.event.SheepClientEventHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ChenMod.TIGER_CLONE.get(), TigerCloneRenderer::new);
        event.registerEntityRenderer(ChenMod.SHEEP_BODY.get(), SheepBodyRenderer::new);
        event.registerEntityRenderer(ChenMod.DRAGON_FIREBALL.get(), DragonFireballRenderer::new);
        event.registerEntityRenderer(ChenMod.MOUSE_BEAM.get(), MouseBeamRenderer::new);
        event.registerEntityRenderer(ChenMod.LIVING_BLOCK.get(), LivingBlockRenderer::new);
        event.registerEntityRenderer(ChenMod.PIG_LASER.get(), PigLaserRenderer::new);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        SheepClientEventHandler.initDisguiseRenderers(event.getContext());
    }
}
