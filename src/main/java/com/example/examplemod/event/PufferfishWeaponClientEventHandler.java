package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.item.PufferfishWeaponItem;
import com.example.examplemod.network.packet.PufferfishWeaponAttackPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public final class PufferfishWeaponClientEventHandler {

    private static boolean wasAttackKeyDown;

    private PufferfishWeaponClientEventHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean attackKeyDown = minecraft.options.keyAttack.isDown();

        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null) {
            wasAttackKeyDown = attackKeyDown;
            return;
        }

        if (!PufferfishWeaponItem.isHoldingPufferfishWeapon(minecraft.player)) {
            wasAttackKeyDown = attackKeyDown;
            return;
        }

        if (minecraft.player.getCooldowns().isOnCooldown(minecraft.player.getMainHandItem().getItem())) {
            wasAttackKeyDown = attackKeyDown;
            return;
        }

        if (attackKeyDown && !wasAttackKeyDown && (minecraft.hitResult == null || minecraft.hitResult.getType() == HitResult.Type.MISS)) {
            PacketDistributor.sendToServer(new PufferfishWeaponAttackPayload());
        }

        wasAttackKeyDown = attackKeyDown;
    }
}
