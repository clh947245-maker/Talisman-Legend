package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端通知服务端：玩家选择结束当前灵魂出窍状态并立即自尽。
 */
public record SheepSuicidePayload() implements CustomPacketPayload {
    public static final Type<SheepSuicidePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_suicide"));

    // 该操作没有额外参数，只需发送一个空负载作为动作信号。
    public static final StreamCodec<ByteBuf, SheepSuicidePayload> STREAM_CODEC =
            StreamCodec.unit(new SheepSuicidePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
