package com.example.examplemod.network;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.SheepPowerMagic;
import com.example.examplemod.magic.transformation.TransformationManager;
import com.example.examplemod.network.packet.SheepReturnPayload;
import com.example.examplemod.network.packet.SheepSuicidePayload;
import com.example.examplemod.network.packet.TransformationSelectionPayload;
import com.example.examplemod.talisman.MonkeyTalismanItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

    public static void handleTransformationSelection(final TransformationSelectionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof MonkeyTalismanItem)) {
                stack = player.getOffhandItem();
            }

            if (stack.getItem() instanceof MonkeyTalismanItem) {
                // Validate ID
                if (payload.transformationId() >= 0 && payload.transformationId() < TransformationManager.getTransformationCount()) {
                    // Save to DataComponents / NBT
                    MonkeyTalismanItem.setSelectedTransformation(stack, payload.transformationId());
                }
            }
        });
    }

    public static void handleSheepReturn(final SheepReturnPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!player.hasEffect(ChenMod.SHEEP_POWER)) {
                return;
            }

            var targetBody = SheepPowerMagic.getNearestReturnableBody(player);
            if (targetBody == null) {
                return;
            }

            SheepPowerMagic.setPendingReturnBody(player, targetBody);
            player.removeEffect(ChenMod.SHEEP_POWER);
        });
    }

    public static void handleSheepSuicide(final SheepSuicidePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!player.hasEffect(ChenMod.SHEEP_POWER)) {
                return;
            }

            SheepPowerMagic.markSkipRestore(player);
            SheepPowerMagic.discardTrackedBody(player);
            SheepPowerMagic.clearSoulState(player);
            player.removeEffect(ChenMod.SHEEP_POWER);
            player.hurt(player.damageSources().magic(), 999999.0F);
        });
    }
}
