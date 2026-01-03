package com.example.examplemod.network;

import com.example.examplemod.network.packet.TransformationRestorePayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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
}
