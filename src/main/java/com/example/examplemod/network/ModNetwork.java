package com.example.examplemod.network;

import com.example.examplemod.ChenMod;
import com.example.examplemod.network.packet.PufferfishWeaponAttackPayload;
import com.example.examplemod.network.packet.ShadowNinjaCommandPayload;
import com.example.examplemod.network.packet.SheepBodyTrackerPayload;
import com.example.examplemod.network.packet.SheepDisguisePayload;
import com.example.examplemod.network.packet.SheepReturnPayload;
import com.example.examplemod.network.packet.SheepSuicidePayload;
import com.example.examplemod.network.packet.TransformationRestorePayload;
import com.example.examplemod.network.packet.TransformationSelectionPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork {
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    private static boolean registered;
    private static int id;

    private ModNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CHANNEL.messageBuilder(TransformationSelectionPayload.class, id++)
                .direction(PacketFlow.SERVERBOUND)
                .encoder((payload, buffer) -> TransformationSelectionPayload.STREAM_CODEC.encode(buffer, payload))
                .decoder(buffer -> TransformationSelectionPayload.STREAM_CODEC.decode(buffer))
                .consumerMainThread(ServerPayloadHandler::handleTransformationSelection)
                .add();
        CHANNEL.messageBuilder(SheepReturnPayload.class, id++)
                .direction(PacketFlow.SERVERBOUND)
                .encoder((payload, buffer) -> SheepReturnPayload.STREAM_CODEC.encode(buffer, payload))
                .decoder(buffer -> SheepReturnPayload.STREAM_CODEC.decode(buffer))
                .consumerMainThread(ServerPayloadHandler::handleSheepReturn)
                .add();
        CHANNEL.messageBuilder(SheepSuicidePayload.class, id++)
                .direction(PacketFlow.SERVERBOUND)
                .encoder((payload, buffer) -> SheepSuicidePayload.STREAM_CODEC.encode(buffer, payload))
                .decoder(buffer -> SheepSuicidePayload.STREAM_CODEC.decode(buffer))
                .consumerMainThread(ServerPayloadHandler::handleSheepSuicide)
                .add();
        CHANNEL.messageBuilder(ShadowNinjaCommandPayload.class, id++)
                .direction(PacketFlow.SERVERBOUND)
                .encoder((payload, buffer) -> ShadowNinjaCommandPayload.STREAM_CODEC.encode(buffer, payload))
                .decoder(buffer -> ShadowNinjaCommandPayload.STREAM_CODEC.decode(buffer))
                .consumerMainThread(ServerPayloadHandler::handleShadowNinjaCommand)
                .add();
        CHANNEL.messageBuilder(PufferfishWeaponAttackPayload.class, id++)
                .direction(PacketFlow.SERVERBOUND)
                .encoder((payload, buffer) -> PufferfishWeaponAttackPayload.STREAM_CODEC.encode(buffer, payload))
                .decoder(buffer -> PufferfishWeaponAttackPayload.STREAM_CODEC.decode(buffer))
                .consumerMainThread(ServerPayloadHandler::handlePufferfishWeaponAttack)
                .add();
        CHANNEL.messageBuilder(SheepBodyTrackerPayload.class, id++)
                .direction(PacketFlow.CLIENTBOUND)
                .encoder((payload, buffer) -> SheepBodyTrackerPayload.STREAM_CODEC.encode(buffer, payload))
                .decoder(buffer -> SheepBodyTrackerPayload.STREAM_CODEC.decode(buffer))
                .consumerMainThread(ClientPayloadHandler::handleSheepBodyTracker)
                .add();
        CHANNEL.messageBuilder(SheepDisguisePayload.class, id++)
                .direction(PacketFlow.CLIENTBOUND)
                .encoder(ModNetwork::encodeSheepDisguise)
                .decoder(ModNetwork::decodeSheepDisguise)
                .consumerMainThread(ClientPayloadHandler::handleSheepDisguise)
                .add();
        CHANNEL.messageBuilder(TransformationRestorePayload.class, id++)
                .direction(PacketFlow.CLIENTBOUND)
                .encoder((payload, buffer) -> TransformationRestorePayload.STREAM_CODEC.encode(buffer, payload))
                .decoder(buffer -> TransformationRestorePayload.STREAM_CODEC.decode(buffer))
                .consumerMainThread(ClientPayloadHandler::handleTransformationRestore)
                .add();
    }

    public static void sendToServer(Object payload) {
        CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
    }

    private static void encodeSheepDisguise(SheepDisguisePayload payload, FriendlyByteBuf buffer) {
        buffer.writeUUID(payload.playerUUID());
        buffer.writeBoolean(payload.active());
        buffer.writeUtf(payload.skinSourceUUID());
        buffer.writeBoolean(payload.displayName() != null);
        if (payload.displayName() != null) {
            ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, payload.displayName());
        }
    }

    private static SheepDisguisePayload decodeSheepDisguise(FriendlyByteBuf buffer) {
        return new SheepDisguisePayload(
                buffer.readUUID(),
                buffer.readBoolean(),
                buffer.readUtf(),
                buffer.readBoolean() ? ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buffer) : null
        );
    }
}
