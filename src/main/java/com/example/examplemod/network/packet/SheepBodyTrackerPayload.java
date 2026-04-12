package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端向客户端同步“原身体”追踪信息。
 */
public record SheepBodyTrackerPayload(
        // 是否存在可追踪的身体快照。
        boolean hasBody,
        // 身体当前是否仍然存活。
        boolean alive,
        // 身体坐标。
        double x,
        double y,
        double z,
        // 身体所在维度 ID。
        String dimension
) implements CustomPacketPayload {
    public static final Type<SheepBodyTrackerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_body_tracker"));

    // 字段按声明顺序编码，客户端会原样读取并刷新本地快照。
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
