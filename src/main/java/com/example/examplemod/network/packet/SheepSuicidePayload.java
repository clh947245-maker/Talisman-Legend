package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SheepSuicidePayload() implements CustomPacketPayload {
    public static final Type<SheepSuicidePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_suicide"));

    public static final StreamCodec<ByteBuf, SheepSuicidePayload> STREAM_CODEC =
            StreamCodec.unit(new SheepSuicidePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
