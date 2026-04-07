package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SheepReturnPayload() implements CustomPacketPayload {
    public static final Type<SheepReturnPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_return"));

    public static final StreamCodec<ByteBuf, SheepReturnPayload> STREAM_CODEC = StreamCodec.unit(new SheepReturnPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
