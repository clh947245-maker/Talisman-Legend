package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PufferfishWeaponAttackPayload() implements CustomPacketPayload {
    public static final Type<PufferfishWeaponAttackPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "pufferfish_weapon_attack"));
    public static final StreamCodec<ByteBuf, PufferfishWeaponAttackPayload> STREAM_CODEC =
            StreamCodec.unit(new PufferfishWeaponAttackPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
