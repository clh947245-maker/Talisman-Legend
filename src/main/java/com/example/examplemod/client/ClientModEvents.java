package com.example.examplemod.client;

import com.example.examplemod.ChenMod;
import com.example.examplemod.client.renderer.DragonFireballRenderer;
import com.example.examplemod.client.renderer.LivingBlockRenderer;
import com.example.examplemod.client.renderer.MouseBeamRenderer;
import com.example.examplemod.client.renderer.PigLaserRenderer;
import com.example.examplemod.client.renderer.PufferfishLaserRenderer;
import com.example.examplemod.client.renderer.SheepBodyRenderer;
import com.example.examplemod.client.renderer.ShengZhuRenderer;
import com.example.examplemod.client.renderer.ShadowNinjaRenderer;
import com.example.examplemod.client.renderer.TigerCloneRenderer;
import com.example.examplemod.client.renderer.layer.OniMaskFaceLayer;
import com.example.examplemod.event.SheepClientEventHandler;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

public class ClientModEvents {
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ChenMod.TIGER_CLONE.get(), TigerCloneRenderer::new);
        event.registerEntityRenderer(ChenMod.SHEEP_BODY.get(), SheepBodyRenderer::new);
        event.registerEntityRenderer(ChenMod.DRAGON_FIREBALL.get(), DragonFireballRenderer::new);
        event.registerEntityRenderer(ChenMod.MOUSE_BEAM.get(), MouseBeamRenderer::new);
        event.registerEntityRenderer(ChenMod.LIVING_BLOCK.get(), LivingBlockRenderer::new);
        event.registerEntityRenderer(ChenMod.PIG_LASER.get(), PigLaserRenderer::new);
        event.registerEntityRenderer(ChenMod.PUFFERFISH_LASER.get(), PufferfishLaserRenderer::new);
        event.registerEntityRenderer(ChenMod.SHADOW_NINJA.get(), ShadowNinjaRenderer::new);
        event.registerEntityRenderer(ChenMod.SHENG_ZHU.get(), ShengZhuRenderer::new);
        event.registerEntityRenderer(ChenMod.AIBO.get(), ChickenRenderer::new);
        event.registerEntityRenderer(ChenMod.MO_DI_CAI.get(), PigRenderer::new);
        event.registerEntityRenderer(ChenMod.AIBO_MO_DI_CAI_FUSION.get(), PigRenderer::new);
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        ShadowNinjaKeyMappings.register(event);
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        SheepClientEventHandler.initDisguiseRenderers(event.getContext());

        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getPlayerSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new OniMaskFaceLayer(renderer));
            }
        }
    }
}
