package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TransformationSelectionPayload(int transformationId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TransformationSelectionPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "transformation_selection"));

    public static final StreamCodec<ByteBuf, TransformationSelectionPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        TransformationSelectionPayload::transformationId,
        TransformationSelectionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
