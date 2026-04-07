package com.example.examplemod.network;

import com.example.examplemod.network.packet.SheepDisguisePayload;
import com.example.examplemod.network.packet.SheepBodyTrackerPayload;
import com.example.examplemod.network.packet.TransformationRestorePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class ClientPayloadHandler {
    public static void handleTransformationRestore(final TransformationRestorePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                // Use reflection to avoid class loading issues on Server
                // Calling ClientHelpers.handleRestore()
                Class<?> clazz = Class.forName("com.example.examplemod.ClientHelpers");
                clazz.getMethod("handleRestore").invoke(null);
            } catch (Exception e) {
                // Ignore errors
            }
        });
    }

    public static void handleSheepBodyTracker(final SheepBodyTrackerPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> SheepBodyTrackerState.update(
                payload.hasBody(),
                payload.alive(),
                payload.x(),
                payload.y(),
                payload.z(),
                payload.dimension()
        ));
    }

    public static void handleSheepDisguise(final SheepDisguisePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            UUID skinSourceUUID = null;
            if (payload.active() && !payload.skinSourceUUID().isBlank()) {
                skinSourceUUID = UUID.fromString(payload.skinSourceUUID());
            }

            SheepDisguiseState.update(payload.playerUUID(), payload.active(), skinSourceUUID, payload.displayName());

            if (Minecraft.getInstance().level == null) {
                return;
            }

            Player player = Minecraft.getInstance().level.getPlayerByUUID(payload.playerUUID());
            if (player != null) {
                player.refreshDisplayName();
            }
        });
    }
}
