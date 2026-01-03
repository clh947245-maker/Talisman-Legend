package com.example.examplemod.network;

import com.example.examplemod.magic.transformation.TransformationManager;
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
}
