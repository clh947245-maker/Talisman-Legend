package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端通知服务端：当前猴符咒选择了哪个变形目标。
 */
public record TransformationSelectionPayload(int transformationId) implements CustomPacketPayload {

    // 注册该自定义负载的唯一类型标识。
    public static final CustomPacketPayload.Type<TransformationSelectionPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "transformation_selection"));

    // 负责在网络上传输 transformationId 字段。
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
