package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端通知客户端执行一次“恢复原形”相关的本地处理。
 * 该包不携带额外数据，只作为触发信号。
 */
public record TransformationRestorePayload() implements CustomPacketPayload {
    public static final Type<TransformationRestorePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "transformation_restore"));

    // unit 表示该数据包没有正文，收发双方只需要识别类型即可。
    public static final StreamCodec<ByteBuf, TransformationRestorePayload> STREAM_CODEC = StreamCodec.unit(new TransformationRestorePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
