package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端发送给服务端的影忍小队控制指令。
 */
public record ShadowNinjaCommandPayload(int action) implements CustomPacketPayload {
    // 召唤一组影忍。
    public static final int ACTION_SUMMON = 0;
    // 遣散当前玩家的所有影忍。
    public static final int ACTION_DISMISS = 1;
    public static final int ACTION_KNEEL = 2;
    public static final Type<ShadowNinjaCommandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "shadow_ninja_command"));
    // 该数据包只需要传递一个动作整型值。
    public static final StreamCodec<ByteBuf, ShadowNinjaCommandPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, ShadowNinjaCommandPayload::action, ShadowNinjaCommandPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
