package com.example.examplemod.network;

import com.example.examplemod.network.packet.SheepDisguisePayload;
import com.example.examplemod.network.packet.SheepBodyTrackerPayload;
import com.example.examplemod.network.packet.TransformationRestorePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent.Context;

import java.util.UUID;

/**
 * 处理服务端同步到客户端的自定义网络包。
 */
public class ClientPayloadHandler {
    public static void handleTransformationRestore(final TransformationRestorePayload payload, final Context context) {
        context.enqueueWork(() -> {
            try {
                // 通过反射调用纯客户端类，避免公共代码在服务端加载 ClientHelpers 时崩溃。
                Class<?> clazz = Class.forName("com.example.examplemod.ClientHelpers");
                clazz.getMethod("handleRestore").invoke(null);
            } catch (Exception e) {
                // 这里是兜底恢复逻辑，失败时静默忽略，避免网络线程被异常打断。
            }
        });
    }

    public static void handleSheepBodyTracker(final SheepBodyTrackerPayload payload, final Context context) {
        context.enqueueWork(() -> SheepBodyTrackerState.update(
                payload.hasBody(),
                payload.alive(),
                payload.x(),
                payload.y(),
                payload.z(),
                payload.dimension()
        ));
    }

    public static void handleSheepDisguise(final SheepDisguisePayload payload, final Context context) {
        context.enqueueWork(() -> {
            UUID skinSourceUUID = null;
            if (payload.active() && !payload.skinSourceUUID().isBlank()) {
                // 仅在伪装开启且服务端提供了来源 UUID 时才解析皮肤来源。
                skinSourceUUID = UUID.fromString(payload.skinSourceUUID());
            }

            SheepDisguiseState.update(payload.playerUUID(), payload.active(), skinSourceUUID, payload.displayName());

            if (Minecraft.getInstance().level == null) {
                return;
            }

            Player player = Minecraft.getInstance().level.getPlayerByUUID(payload.playerUUID());
            if (player != null) {
                // 强制刷新名称显示，让昵称和皮肤相关渲染尽快应用到当前实体。
                player.refreshDisplayName();
            }
        });
    }
}
