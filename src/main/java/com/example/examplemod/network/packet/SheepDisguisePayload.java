package com.example.examplemod.network.packet;

import com.example.examplemod.ChenMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SheepDisguisePayload(
        java.util.UUID playerUUID,
        boolean active,
        String skinSourceUUID,
        Component displayName
) implements CustomPacketPayload {
    public static final Type<SheepDisguisePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_disguise"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SheepDisguisePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SheepDisguisePayload::playerUUID,
            ByteBufCodecs.BOOL,
            SheepDisguisePayload::active,
            ByteBufCodecs.STRING_UTF8,
            SheepDisguisePayload::skinSourceUUID,
            ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC,
            payload -> java.util.Optional.ofNullable(payload.displayName()),
            (playerUUID, active, skinSourceUUID, displayName) -> new SheepDisguisePayload(
                    playerUUID,
                    active,
                    skinSourceUUID,
                    displayName.orElse(null)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
