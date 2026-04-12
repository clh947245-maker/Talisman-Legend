package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端通知服务端：尝试返回最近一个可回归的身体。
 */
public record SheepReturnPayload() implements CustomPacketPayload {
    public static final Type<SheepReturnPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_return"));

    // 回归请求只表达“执行此动作”，因此不需要额外字段。
    public static final StreamCodec<ByteBuf, SheepReturnPayload> STREAM_CODEC = StreamCodec.unit(new SheepReturnPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
