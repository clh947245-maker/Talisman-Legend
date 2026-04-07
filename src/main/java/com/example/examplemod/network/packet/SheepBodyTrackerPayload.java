package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SheepBodyTrackerPayload(
        boolean hasBody,
        boolean alive,
        double x,
        double y,
        double z,
        String dimension
) implements CustomPacketPayload {
    public static final Type<SheepBodyTrackerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_body_tracker"));

    public static final StreamCodec<ByteBuf, SheepBodyTrackerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SheepBodyTrackerPayload::hasBody,
            ByteBufCodecs.BOOL,
            SheepBodyTrackerPayload::alive,
            ByteBufCodecs.DOUBLE,
            SheepBodyTrackerPayload::x,
            ByteBufCodecs.DOUBLE,
            SheepBodyTrackerPayload::y,
            ByteBufCodecs.DOUBLE,
            SheepBodyTrackerPayload::z,
            ByteBufCodecs.STRING_UTF8,
            SheepBodyTrackerPayload::dimension,
            SheepBodyTrackerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
