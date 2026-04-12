package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.client.ShadowNinjaKeyMappings;
import com.example.examplemod.item.OniMaskItem;
import com.example.examplemod.network.packet.ShadowNinjaCommandPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public final class ShadowNinjaClientEventHandler {
    private ShadowNinjaClientEventHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        if (OniMaskItem.getMaskAnchor(minecraft.player) == null) {
            while (ShadowNinjaKeyMappings.SUMMON.consumeClick()) {
            }
            while (ShadowNinjaKeyMappings.DISMISS.consumeClick()) {
            }
            return;
        }

        while (ShadowNinjaKeyMappings.SUMMON.consumeClick()) {
            PacketDistributor.sendToServer(new ShadowNinjaCommandPayload(ShadowNinjaCommandPayload.ACTION_SUMMON));
        }

        while (ShadowNinjaKeyMappings.DISMISS.consumeClick()) {
            PacketDistributor.sendToServer(new ShadowNinjaCommandPayload(ShadowNinjaCommandPayload.ACTION_DISMISS));
        }
    }
}
