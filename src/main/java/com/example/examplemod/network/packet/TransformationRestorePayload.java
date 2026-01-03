package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TransformationRestorePayload() implements CustomPacketPayload {
    public static final Type<TransformationRestorePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "transformation_restore"));

    public static final StreamCodec<ByteBuf, TransformationRestorePayload> STREAM_CODEC = StreamCodec.unit(new TransformationRestorePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
