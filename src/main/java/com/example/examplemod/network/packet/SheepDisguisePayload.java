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

/**
 * 服务端同步绵羊伪装相关的客户端展示数据。
 */
public record SheepDisguisePayload(
        // 被同步状态的玩家 UUID。
        java.util.UUID playerUUID,
        // 当前是否处于伪装状态。
        boolean active,
        // 皮肤来源玩家 UUID，空字符串表示没有额外皮肤来源。
        String skinSourceUUID,
        // 客户端需要展示的名字，可为空。
        Component displayName
) implements CustomPacketPayload {
    public static final Type<SheepDisguisePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_disguise"));

    // 使用 RegistryFriendlyByteBuf 是因为 Component 的序列化依赖注册表上下文。
    public static final StreamCodec<RegistryFriendlyByteBuf, SheepDisguisePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SheepDisguisePayload::playerUUID,
            ByteBufCodecs.BOOL,
            SheepDisguisePayload::active,
            ByteBufCodecs.STRING_UTF8,
            SheepDisguisePayload::skinSourceUUID,
            ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC,
            payload -> java.util.Optional.ofNullable(payload.displayName()),
            // 反序列化时把 Optional<Component> 还原回 record 中允许为 null 的字段。
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
