package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.client.ShadowNinjaKeyMappings;
import com.example.examplemod.item.OniMaskItem;
import com.example.examplemod.network.packet.ShadowNinjaCommandPayload;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import com.example.examplemod.network.ModNetwork;

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
            while (ShadowNinjaKeyMappings.KNEEL.consumeClick()) {
            }
            return;
        }

        while (ShadowNinjaKeyMappings.SUMMON.consumeClick()) {
            ModNetwork.sendToServer(new ShadowNinjaCommandPayload(ShadowNinjaCommandPayload.ACTION_SUMMON));
        }

        while (ShadowNinjaKeyMappings.DISMISS.consumeClick()) {
            ModNetwork.sendToServer(new ShadowNinjaCommandPayload(ShadowNinjaCommandPayload.ACTION_DISMISS));
        }

        while (ShadowNinjaKeyMappings.KNEEL.consumeClick()) {
            ModNetwork.sendToServer(new ShadowNinjaCommandPayload(ShadowNinjaCommandPayload.ACTION_KNEEL));
        }
    }
}
